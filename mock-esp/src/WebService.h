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
  std::list<InOutRule*> inOutRules;
  std::list<OutInRule*> outInRules;
  std::list<OutputHandler*> outputHandlers;
  std::list<String> collectedHeaders = {"Content-Type"};
  void handleNotFound();
  void handleRulesPOST();
  void handleConfigPOST();
  void handleAttackPOST();

 public:
  void update();
  void start();
};

#endif
