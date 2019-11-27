#ifndef LOADER_H
#define LOADER_H

#include <list>
#include "model/Rule.h"

class Loader {
 public:
  virtual ~Loader(){};
  virtual String load(String str, std::list<Rule *> &rules) = 0;
};

#endif
