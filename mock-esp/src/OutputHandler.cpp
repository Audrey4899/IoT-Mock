#include <Arduino.h>
#include <ESP8266HTTPClient.h>
#include <ESP8266WiFiMulti.h>
#include "OutputHandler.h"

/**
 * The constructor creates an OutputHandler from a given OutIn rule.
 * @param rule: The given rule.
 */ 
OutputHandler::OutputHandler(OutInRule &rule) {
  this->rule = &rule;
  this->count = 0;
  this->startTime = 0;
  this->lastTime = 0;
  this->hasStarted = false;
}

/**
 * This method updates the OutputHandler.
 */ 
void OutputHandler::update() {
  if (isDone()) return;
  if (this->startTime == 0) this->startTime = millis();
  if (!this->hasStarted && this->rule->getTimeout() < (long)(millis() - this->startTime)) {
    sendRequest();
    this->hasStarted = true;
    return;
  }
  if (hasStarted && this->rule->getInterval() < (long)(millis() - this->lastTime)) {
    sendRequest();
  }
}

/**
 * This method sends the rule request.
 */
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

  if (test < 0) Serial.println("Request: " + rule->getRequest().getMethod() + " " + rule->getRequest().getPath() + " -- " + "ERROR " + http.errorToString(test));
  else Serial.println("Request: " + rule->getRequest().getMethod() + " " + rule->getRequest().getPath() + " -- " + test);

  http.end();
}

/**
 * This method checks if the rule request has to don't be send anymore.
 * @return True if the rule request has to don't be send anymore, otherwise false.
 */
bool OutputHandler::isDone() {
  return (count >= rule->getRepeat() && rule->getRepeat() != 0);
}
