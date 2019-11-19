#ifndef WEBSERVICE_H
#define WEBSERVICE_H

// #include <Arduino.h>
#include <ESP8266WebServer.h>

class WebService {
 private:
  ESP8266WebServer server;

  void handleNotFound();

 public:
  void update();
  void start();
};

#endif
