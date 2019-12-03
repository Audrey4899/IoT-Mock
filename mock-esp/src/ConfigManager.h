#ifndef CONFIGMANGER_H
#define CONFIGMANGER_H

#include <Arduino.h>
#include <FS.h>

class ConfigManager {
 public:
  static void save(String ssid, String password) {
    SPIFFS.begin();
    File f = SPIFFS.open("WIFIConfig", "w");
    f.printf("%s;%s;", ssid.c_str(), password.c_str());
    SPIFFS.end();
  }
  static bool load(String &ssid, String &password) {
    SPIFFS.begin();
    if(!SPIFFS.exists("WIFIConfig")) return false;
    File f = SPIFFS.open("WIFIConfig", "r");
    ssid = f.readStringUntil(';');
    password = f.readStringUntil(';');
    SPIFFS.end();
    return true;
  }
};

#endif
