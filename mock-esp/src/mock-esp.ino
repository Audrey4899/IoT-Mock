#include <Arduino.h>
#include <EEPROM.h>

#include "ConfigManager.h"
#include "WifiManager.h"
#include "WebService.h"

WebService webservice;


void setup() {
  Serial.begin(9600);
  Serial.println("Hello world !");

  initWifi();
  webservice.start();
}

void loop() { webservice.update(); }


void initWifi() {
  String ssid, password;
  bool configured = ConfigManager::load(ssid, password);
  if(configured) {
    bool connected = WifiManager::connect(ssid, password);
    if(!connected) WifiManager::startAP();
  } else {
    WifiManager::startAP();
  }
}
