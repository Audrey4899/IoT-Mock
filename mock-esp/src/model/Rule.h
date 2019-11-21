#ifndef RULE_H
#define RULE_H

#include "Request.h"
#include "Response.h"

class Rule {
 private:
  Request request;
  Response response;

 public:
  Rule() {}
  Rule(Request request, Response response);
  virtual Request getRequest();
  virtual Response getResponse();
};

#endif
