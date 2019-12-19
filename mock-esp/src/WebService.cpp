#include "WebService.h"

#include "load/JsonLoader.h"
#include "ConfigManager.h"
#include "Attacker.h"

/**
 * This method starts the web server. The server is listening on 3 paths:
 * /config, /rules and /attack.
 * It also collects the content type of the HTTP request.
 */
void WebService::start() {
  server.on("/config", HTTP_POST, [this]() { handleConfigPOST(); });
  server.on("/rules", HTTP_POST, [this]() { handleRulesPOST(); });
  server.on("/attack", HTTP_POST, [this]() { handleAttackPOST(); });
  server.onNotFound([this]() { handleNotFound(); });

  const char *keys[] = {"Content-Type"};
  server.collectHeaders(keys, 1);

  server.begin();
  Serial.println("Web service initialized.");
}

/**
 * This method must be called in the ESP loop method.
 * It updates the OutputHandlers and add them to the ended list if they are.
 * After the update, it deletes the ended OutputHandlers from the list.
 */ 
void WebService::update() {
  server.handleClient();
  std::list<OutputHandler*> ended;
  for (OutputHandler *handler : this->outputHandlers) {
    handler->update();
    if(handler->isDone()) {
      ended.push_back(handler);
    }
  }
  while (ended.size()>0) {
    OutputHandler *h = ended.front();
    ended.pop_front();
    outputHandlers.remove(h);
    delete h;
  }
}

/**
 * This method is called when the user does a HTTP POST request to the
 * attack path of the mock IP address. It calls the correct attack methods
 * according to the user's choice and creates OutputHandlers for each attack rules
 * returned by the attacker class.
 */
void WebService::handleAttackPOST() {
  String type = server.arg("type");
  Attacker attacker(outInRules);
  if(type.equals("all")) {
    attacker.XSSAttacks();
    attacker.httpFloodAttack();
    attacker.robustnessAttack();
  } else if (type.equals("xss")) {
    attacker.XSSAttacks();
  } else if (type.equals("httpflood")) {
    attacker.httpFloodAttack();
  } else if(type.equals("robustness")) {
    attacker.robustnessAttack();
  } else {
    server.send(400, "text/plain", "Attack type not correct.");
    return;
  } 
  attackRules = attacker.attack();
  for(OutInRule* rule: attackRules) {
    this->outputHandlers.push_back(new OutputHandler(*rule)); 
  }
  server.send(204);
}

/**
 * This method is called when the user does a HTTP POST request to the
 * rules path of the mock IP address. It loads the rules given with the JSON loader.
 */ 
void WebService::handleRulesPOST() {
  Loader *loader;

  String contentType = server.header("Content-Type");
  if (contentType.equals("application/json")) {
    loader = new JsonLoader();
  } else {
    server.send(400, "text/plain", "Content-Type must be application/json.");
    return;
  }

  std::list<Rule *> rules;
  String error = loader->load(server.arg("plain"), rules);
  delete loader;
  if (!error.equals("OK")) {
    server.send(400, "text/plain", error);
    return;
  }
  std::list<String> headersKeys;
  for (Rule *r : rules) {
    if (r->getClass().equals("InOutRule")) {
      InOutRule *inout = (InOutRule *)r;
      for (auto &&h : inout->getRequest().getHeaders()) {
        headersKeys.push_back(h.first);
      }

      inOutRules.push_back(inout);
    } else if (r->getClass().equals("OutInRule")) {
      OutInRule *outin = (OutInRule *)r;
      outInRules.push_back(outin);
      outputHandlers.push_back(new OutputHandler(*outin));
    }
  }

  headersKeys.unique();
  collectedHeaders.merge(headersKeys);
  collectedHeaders.unique();
  char *keys[collectedHeaders.size()];
  int i = 0;
  for (String k : collectedHeaders) {
    char *key = new char[k.length() + 1];
    strcpy(key, k.c_str());
    keys[i++] = key;
  }
  server.collectHeaders((const char **)keys, collectedHeaders.size());

  server.send(204);
}

/**
 * This method is called when the user does a HTTP request to the
 * mock IP address. It checks if an InOut rule with the requested path exist.
 * If not, it returns a 404 status code.
 */ 
void WebService::handleNotFound() {
  String rawURI = server.uri();
  String query = "";
  for (size_t i = 0; i < (size_t)server.args(); i++) {
    if (server.argName(i).equals("plain")) continue;
    query += "&" + server.argName(i) + "=" + server.arg(i);
  }
  if (query.length() != 0) {
    query[0] = *"?";
  }
  rawURI += query;

  String methods[] = {"ANY", "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"};

  InOutRule *r = nullptr;
  for (InOutRule *rule : inOutRules) {
    String simpleURI = rule->getRequest().getPath().substring(0, rule->getRequest().getPath().indexOf("?"));
    if (!methods[server.method()].equals(rule->getRequest().getMethod())) continue;
    if (!rawURI.equals(rule->getRequest().getPath())) continue;
    if (!server.arg("plain").equals(rule->getRequest().getBody())) continue;

    bool areHeadersGood = true;
    for (auto &&h : rule->getRequest().getHeaders()) {
      if (server.header(h.first) != h.second) areHeadersGood = false;
    }
    if (!areHeadersGood) continue;

    r = rule;
    break;
  }
  if (r != nullptr) {
    String contentType;
    for (auto &&header : r->getResponse().getHeaders()) {
      if (header.first == "Content-Type") contentType = header.second;
      else server.sendHeader(header.first, header.second);
    }
    contentType = (contentType == "") ? "text/plain" : contentType;
    server.send(r->getResponse().getStatus(), contentType, r->getResponse().getBody());
    Serial.println(r->getRequest().getMethod() + " on " + r->getRequest().getPath() + " -> " + r->getResponse().getStatus());
  } else {
    server.send(404, "text/plain", "No rule defined for this request.");
    Serial.println(r->getRequest().getMethod() + " on " + r->getRequest().getPath() + " -> 404 No rule found.");
  }
}

/**
 * This method is called when the user does a HTTP POST request to the
 * config path of the mock IP address. It configures the Wifi SSID and password
 * on which the ESP will connect. 
 */ 
void WebService::handleConfigPOST() {
  String ssid = server.arg("ssid");
  String password = server.arg("password");
  String ip = server.arg("ip");
  ConfigManager::save(ssid, password, ip);
  server.send(204);
  delay(500);
  Serial.println("Rebooting...");
  void (*reset)(void) = 0;
  reset();
}
