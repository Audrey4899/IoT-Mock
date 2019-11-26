#ifndef RULE_H
#define RULE_H

#include <Arduino.h>
#include "Request.h"
#include "Response.h"

class Rule {
 private:
  Request *request;
  Response *response;

 public:
  virtual ~Rule() {}
  Rule(Request &request, Response &response);
  Request getRequest();
  Response getResponse();
  virtual String getClass() = 0;
};

#endif
