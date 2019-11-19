#ifndef WIFIMANAGER_H
#define WIFIMANAGER_H

#include <Arduino.h>

class WifiManager {
 public:
  static void startAP();
  static void connect(String ssid, String password);
};

#endif
