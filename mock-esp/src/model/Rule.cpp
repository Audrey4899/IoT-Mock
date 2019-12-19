#include "Rule.h"

Rule::Rule(Request &request, Response &response) {
  this->request = &request;
  this->response = &response;
}

Request Rule::getRequest() { return *request; }

Response Rule::getResponse() { return *response; }
