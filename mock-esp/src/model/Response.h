#ifndef RESPONSE_H
#define RESPONSE_H

#include <Arduino.h>
#include <map>

class Response {
 private:
  int status;
  std::map<String, String> headers;
  String body;

 public:
  Response(int status, std::map<String, String> headers, String body);
  int getStatus();
  std::map<String, String> getHeaders();
  String getBody();
};

#endif
