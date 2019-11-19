#include "Response.h"

Response::Response(int status, std::map<String, String> headers, String body) {
  this->status = status;
  this->headers = headers;
  this->body = body;
}

int Response::getStatus() {
  return this->status;
}


std::map<String, String> Response::getHeaders() {
  return this->headers;
}

String Response::getBody() {
  return this->body;
}
