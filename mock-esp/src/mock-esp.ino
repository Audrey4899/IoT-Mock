#include <Arduino.h>
#include <EEPROM.h>

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
  // WifiManager::startAP();
  WifiManager::connect("Wi-Fi", "4815162342");
}

void saveCredential(String ssid, String password) {
  String ok = "OK";
  EEPROM.begin(512);
  for (int i = 0; i < 512; ++i) {
    EEPROM.write(i, 255);
  }
  EEPROM.put(0, ssid);
  EEPROM.put(0 + sizeof(ssid), password);
  EEPROM.put(0 + sizeof(ssid) + sizeof(password), "OK");
  // EEPROM.commit();
  EEPROM.end();
}

boolean loadCredentials(String &ssid, String &password) {
  EEPROM.begin(512);
  // for (int i = 0; i < 512; ++i) {
  //   byte b = EEPROM.read(i);
  //   Serial.printf("%.2x ", b);
  // }
  EEPROM.get(0, ssid);
  EEPROM.get(0 + sizeof(ssid), password);
  String ok;
  // EEPROM.get(0 + sizeof(ssid) + sizeof(password), ok);
  EEPROM.end();
  if(ok.equals("OK")) return true;
  else return false;
}
