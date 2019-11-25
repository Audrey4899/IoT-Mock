#include "WebService.h"

#include "load/JsonLoader.h"

void WebService::start() {
  server.on("/rules", HTTP_POST, [this]() {
    JsonLoader *loader;

    String contentType = server.header("Content-Type");
    if (contentType.equals("application/json")) {
      loader = new JsonLoader();
    } else {
      server.send(400);
      return;
    }
    std::list<Rule *> rules;
    String error = loader->load(server.arg("plain"), rules);
    if (!error.equals("OK")) {
      server.send(400, "text/plain", error);
      return;
    }
    // for (Rule *r : rules) {
    //   if(InOutRule *inout = dynamic_cast<InOutRule*>(r)) {
    //     Serial.println("intout");
    //   } else if (OutInRule *outin = dynamic_cast<OutInRule*>(r)) {
    //     Serial.println("outin");
    //   } else {
    //     Serial.println("Not cool");
    //   }
    // }

    server.send(204);
  });
  server.onNotFound([this]() { handleNotFound(); });

  const char *keys[] = {"Content-Type"};
  server.collectHeaders(keys, 1);

  server.begin();
  Serial.println("Web service initialized.");
}

void WebService::update() { 
  server.handleClient();
  for (OutputHandler *handler: this->outputHandlers) {
    handler->update();
  }
}

void WebService::handleNotFound() {
  server.send(200, "text/plain", "This is route *");
}
