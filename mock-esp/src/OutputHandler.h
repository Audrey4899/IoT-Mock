#ifndef OUTPUTHANDLER_H
#define OUTPUTHANDLER_H

#include "model/OutInRule.h"

class OutputHandler {
 private:
  OutInRule *rule;
  int count;
  unsigned long startTime;
  unsigned long lastTime;
  bool hasStarted;

  void sendRequest();
 public:
  explicit OutputHandler(OutInRule &rule);
  void update();
  bool isDone();
};

#endif
