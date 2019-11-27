#ifndef CONFIGMANGER_H
#define CONFIGMANGER_H

#include <Arduino.h>
#include <EEPROM.h>

class ConfigManager {
 public:
  static void saveConfig(String ssid, String password) {
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
  static bool loadConfig(String &ssid, String &password) {
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
    Serial.println(ok);
    // if (ok.equals("OK"))
    //   return true;
    // else
    //   return false;
    return true;
  }
};

#endif
