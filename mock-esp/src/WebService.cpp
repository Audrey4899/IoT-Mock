#include "WebService.h"

#include "load/JsonLoader.h"
#include "ConfigManager.h"

void WebService::start() {
  server.on("/config", HTTP_POST, [this]() { handleConfigPOST(); });
  server.on("/rules", HTTP_POST, [this]() { handleRulesPOST(); });
  server.onNotFound([this]() { handleNotFound(); });

  const char *keys[] = {"Content-Type"};
  server.collectHeaders(keys, 1);

  server.begin();
  Serial.println("Web service initialized.");
}

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
      Serial.println(inout->getRequest().getPath());
      for (auto &&h : inout->getRequest().getHeaders()) {
        headersKeys.push_back(h.first);
      }

      inOutRules.push_back(inout);
    } else if (r->getClass().equals("OutInRule")) {
      OutInRule *outin = (OutInRule *)r;
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

void WebService::handleNotFound() {
  String rawURI = server.uri();
  String query = "";
  for (size_t i = 0; i < server.args(); i++) {
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
      if (header.first == "Content-Type") contentType = header.first;
      else server.sendHeader(header.first, header.second);
    }
    contentType = (contentType == "") ? "text/plain" : contentType;
    server.send(r->getResponse().getStatus(), contentType, r->getResponse().getBody());
  } else {
    server.send(404, "text/plain", "No rule defined for this request.");
  }
}

void WebService::handleConfigPOST() {
  String ssid = server.arg("ssid");
  String password = server.arg("password");
  ConfigManager::save(ssid, password);
  server.send(204);
  delay(500);
  void (*reset)(void) = 0;
  reset();
}
