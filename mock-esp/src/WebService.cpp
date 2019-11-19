#include "WebService.h"

void WebService::start() {
  server.on("/", [this]() { server.send(200); });
  server.onNotFound([this]() { handleNotFound(); });
  server.begin();
  Serial.println("Web service initialized.");
}

void WebService::update() { server.handleClient(); }

void WebService::handleNotFound() { server.send(404); }
