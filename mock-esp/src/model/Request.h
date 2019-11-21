#ifndef REQUEST_H
#define REQUEST_H

#include <Arduino.h>
#include <map>


class Request {
 private:
  String method;
  String path;
  std::map<String, String> headers;
  String body;

 public:
  Request() {};
  Request(String method, String path, std::map<String, String> headers, String body);
  String getMethod();
  String getPath();
  std::map<String, String> getHeaders();
  String getBody();
};

#endif
