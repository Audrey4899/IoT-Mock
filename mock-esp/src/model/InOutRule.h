#ifndef INOUTRULE_H
#define INOUTRULE_H

#include "Rule.h"

class InOutRule : public Rule {
 public:
  InOutRule(Request &request, Response &response);
  String getClass() override { return "InOutRule"; };
};

#endif
