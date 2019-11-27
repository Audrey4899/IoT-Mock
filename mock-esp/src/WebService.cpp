#include "WebService.h"

#include "load/JsonLoader.h"

void WebService::start() {
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
    } else if (r->getClass().equals("OutInRule")) {
      OutInRule *outin = (OutInRule *)r;
      Serial.println(outin->getRequest().getPath());
      outputHandlers.push_back(new OutputHandler(*outin));
    }
  }

  server.send(204);
}

void WebService::handleNotFound() {
  server.send(200, "text/plain", "This is route *");
}
