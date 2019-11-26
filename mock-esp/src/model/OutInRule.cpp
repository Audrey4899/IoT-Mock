#include "OutInRule.h"

OutInRule::OutInRule(Request &request, Response &response, long timeout, int repeat, long interval) : Rule(request, response) {
  this->timeout = timeout;
  this->repeat = repeat;
  this->interval = interval;
}

long OutInRule::getTimeout() {
  return this->timeout;
}

int OutInRule::getRepeat() {
  return this->repeat;
}

long OutInRule::getInterval() {
  return this->interval;
}
