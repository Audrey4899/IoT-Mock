#ifndef WEBSERVICE_H
#define WEBSERVICE_H

// #include <Arduino.h>
#include <ESP8266WebServer.h>
#include <list>
#include "model/InOutRule.h"
#include "OutputHandler.h"

class WebService {
 private:
  ESP8266WebServer server;
  std::list<OutputHandler*> outputHandlers;
  void handleNotFound();
  void handleRulesPOST();
  void handleConfigPOST();

 public:
  void update();
  void start();
};

#endif
