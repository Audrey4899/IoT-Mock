#include "WifiManager.h"
#include <ESP8266WiFi.h>

void WifiManager::startAP() {
  IPAddress local_IP(192, 168, 1, 1);
  IPAddress gateway(192, 168, 1, 1);
  IPAddress subnet(255, 255, 255, 0);

  WiFi.mode(WIFI_AP);
  Serial.println("Setting up wifi Access Point.");

  WiFi.softAPConfig(local_IP, gateway, subnet);

  uint8_t mac[WL_MAC_ADDR_LENGTH];
  WiFi.softAPmacAddress(mac);
  String macID = String(mac[WL_MAC_ADDR_LENGTH - 2], HEX) +
                 String(mac[WL_MAC_ADDR_LENGTH - 1], HEX);
  macID.toUpperCase();
  String AP_NameString = "ESP8266 " + macID;

  WiFi.softAP(AP_NameString, "");
}

bool WifiManager::connect(String ssid, String password) {
  WiFi.mode(WIFI_STA);

  WiFi.begin(ssid, password);
  Serial.print("Connecting to " + ssid + " with password " + password);
  int elapsedTime = 0;
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
    if(elapsedTime>20000) return false;
    elapsedTime += 500;
  }
  Serial.println();
  Serial.println("Connected with IP: " + WiFi.localIP().toString());
  return true;
}
