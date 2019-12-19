#ifndef WIFIMANAGER_H
#define WIFIMANAGER_H

#include <Arduino.h>

class WifiManager {
 private:
  static void parseBytes(const char* str, char sep, byte* bytes, int maxBytes, int base);
 public:
  static void startAP();
  static bool connect(String ssid, String password, String localIp);
};

#endif
