#ifndef CONFIGMANGER_H
#define CONFIGMANGER_H

#include <Arduino.h>
#include <FS.h>

/**
 * This class allows you to save and load the Wifi configuration on which the ESP will connect.
 */
class ConfigManager {
 public:
  /**
   * Saves the Wifi SSID and password on which the ESP will connect.
   * @param ssid: The Wifi SSID.
   * @param password: The Wifi password.
   */ 
  static void save(String ssid, String password, String ip) {
    SPIFFS.begin();
    File f = SPIFFS.open("WIFIConfig", "w");
    f.printf("%s;%s;%s;", ssid.c_str(), password.c_str(), ip.c_str());
    SPIFFS.end();
  }
  /**
   * Loads the Wifi configuration.
   * @param ssid: The Wifi SSID.
   * @param password: The Wifi password.
   * @return False if there isn't a configuration, otherwise true.
   */
  static bool load(String &ssid, String &password, String &ip) {
    SPIFFS.begin();
    if(!SPIFFS.exists("WIFIConfig")) return false;
    File f = SPIFFS.open("WIFIConfig", "r");
    ssid = f.readStringUntil(';');
    password = f.readStringUntil(';');
    ip = f.readStringUntil(';');
    SPIFFS.end();
    return true;
  }
};

#endif
