#ifndef OUTINRULE_H
#define OUTINRULE_H

#include "Rule.h"

class OutInRule : public Rule {
 private:
  long timeout;
  int repeat;
  long interval;

 public:
  OutInRule(Request &request, Response &response, long timeout, int repeat,
            long interval);
  long getTimeout();
  int getRepeat();
  long getInterval();
  String getClass() override { return F("OutInRule"); };
};

#endif
