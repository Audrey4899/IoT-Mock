#include <Arduino.h>
#include "OutputHandler.h"

OutputHandler::OutputHandler(OutInRule rule) {
  this->rule = rule;
  this->count = 0;
  this->startTime = 0;
  this->lastTime = 0;
  this->hasStarted = false;
}

void OutputHandler::update() {
  if (isDone()) return;
  if (this->startTime == 0) this->startTime = millis();
  if (!this->hasStarted && this->rule.getTimeout() < (long)(millis() - this->startTime)) {
    sendRequest();
    this->hasStarted = true;
  }
  if (hasStarted && this->rule.getInterval() < (long)(millis() - this->lastTime)) {
    sendRequest();
  }
}

void OutputHandler::sendRequest() {
  this->count++;
  this->lastTime = millis();

  Serial.println("Sending...");
  // unsigned long tmp = millis();
  // Serial.println(tmp - test);
  // test = tmp;
}

bool OutputHandler::isDone() {
  return (count >= rule.getRepeat() && rule.getRepeat() != 0);
}
