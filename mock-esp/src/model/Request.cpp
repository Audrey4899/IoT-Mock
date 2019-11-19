#include "Request.h"

Request::Request(String method, String path, std::map<String, String> headers, String body) {
  this->method = method;
  this->path = path;
  this->headers = headers;
  this->body = body;
}

String Request::getMethod() {
  return this->method;
}

String Request::getPath() {
  return this->path;
}

std::map<String, String> Request::getHeaders() {
  return this->headers;
}

String Request::getBody() {
  return this->body;
}
