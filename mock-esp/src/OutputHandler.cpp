#include <Arduino.h>
#include <ESP8266HTTPClient.h>
#include "OutputHandler.h"

OutputHandler::OutputHandler(OutInRule &rule) {
  this->rule = &rule;
  this->count = 0;
  this->startTime = 0;
  this->lastTime = 0;
  this->hasStarted = false;
}

void OutputHandler::update() {
  if (isDone()) return;
  if (this->startTime == 0) this->startTime = millis();
  if (!this->hasStarted && this->rule->getTimeout() < (long)(millis() - this->startTime)) {
    sendRequest();
    this->hasStarted = true;
  }
  if (hasStarted && this->rule->getInterval() < (long)(millis() - this->lastTime)) {
    sendRequest();
  }
}

void OutputHandler::sendRequest() {
  this->count++;
  this->lastTime = millis();

  WiFiClient client;
  HTTPClient http;
  http.begin(client, rule->getRequest().getPath());

  for (auto &&h : rule->getRequest().getHeaders()) {
    http.addHeader(h.first, h.second);
  }
  int test = http.sendRequest((char *)(rule->getRequest().getMethod().c_str()), rule->getRequest().getBody());

  if (test < 0) Serial.println(http.errorToString(test));
  else Serial.println(test);

  http.end();
  // unsigned long tmp = millis();
  // Serial.println(tmp - test);
  // test = tmp;
}

bool OutputHandler::isDone() {
  return (count >= rule->getRepeat() && rule->getRepeat() != 0);
}
