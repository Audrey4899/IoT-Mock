#include "WifiManager.h"
#include <ESP8266WiFi.h>

/**
 * This method sets up the ESP Wifi access point.
 */
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

void WifiManager::parseBytes(const char* str, char sep, byte* bytes, int maxBytes, int base) {
  for (int i = 0; i < maxBytes; i++) {
    bytes[i] = strtoul(str, NULL, base);  // Convert byte
    str = strchr(str, sep);               // Find next separator
    if (str == NULL || *str == '\0') {
      break;  // No more separators, exit
    }
    str++;  // Point to next character after separator
  }
}

/**
 * This method connects the ESP to the given Wifi SSID with the given password.
 * @param ssid: The Wifi SSID.
 * @param password: The Wifi password.
 * @return True if the ESP has successfully connected to Wifi, otherwise false.
 */
bool WifiManager::connect(String ssid, String password, String localIp) {
  WiFi.mode(WIFI_STA);

  if (localIp != "" && localIp != "0.0.0.0") {
    byte ip[4];
    parseBytes(localIp.c_str(), '.', ip, 4, 10);
    byte subnet[4];
    subnet[0] = ip[0];
    subnet[1] = ip[1];
    subnet[2] = ip[2];
    subnet[3] = (int)0;

    WiFi.config(ip, IPAddress(0, 0, 0, 0), subnet);
  } else { 
    localIp = "auto";
  }

  WiFi.begin(ssid, password);
  Serial.print("Connecting to " + ssid + " with password " + password + " and ip " + localIp);
  int elapsedTime = 0;
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
    if (elapsedTime > 20000) return false;
    elapsedTime += 500;
  }
  Serial.println();
  Serial.println("Connected with IP: " + WiFi.localIP().toString());
  return true;
}
