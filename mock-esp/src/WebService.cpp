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
  for (OutputHandler *handler : this->outputHandlers) {
    handler->update();
    // if(handler->isDone()) {
    //   outputHandlers.remove(handler);
    //   delete handler;
    // }
  }
}

void WebService::handleRulesPOST() {
  Loader *loader;

  String contentType = server.header("Content-Type");
  if (contentType.equals("application/json")) {
    loader = new JsonLoader();
  } else {
    server.send(400);
    return;
  }

  std::list<Rule *> rules;
  String error = loader->load(server.arg("plain"), rules);
  delete loader;
  if (!error.equals("OK")) {
    server.send(400, "text/plain", error);
    return;
  }
  for (Rule *r : rules) {
    if (r->getClass().equals("InOutRule")) {
      InOutRule *inout = (InOutRule *)r;
      Serial.println(inout->getRequest().getPath());
      inOutRules.push_back(inout);
    } else if (r->getClass().equals("OutInRule")) {
      OutInRule *outin = (OutInRule *)r;
      outputHandlers.push_back(new OutputHandler(*outin));
    }
  }

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

  InOutRule *r = nullptr;
  for (InOutRule *rule : inOutRules) {
    String simpleURI = rule->getRequest().getPath().substring(0, rule->getRequest().getPath().indexOf("?"));
    if (!rawURI.equals(rule->getRequest().getPath())) continue;
    // TODO: Check method, headers and body
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
  ConfigManager::saveConfig(ssid, password);
  server.send(204);
  delay(500);
  void (*reset)(void) = 0;
  reset();
}
