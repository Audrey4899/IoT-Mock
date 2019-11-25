#ifndef OUTPUTHANDLER_H
#define OUTPUTHANDLER_H

#include "model/OutInRule.h"

class OutputHandler {
 private:
  OutInRule rule;
  int count;
  unsigned long startTime;
  unsigned long lastTime;
  bool hasStarted;

  unsigned long test = 0;

  void sendRequest();
 public:
  OutputHandler(OutInRule rule);
  void update();
  bool isDone();
};

#endif
