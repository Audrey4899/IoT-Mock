#ifndef LOADER_H
#define LOADER_H

#include <list>
#include "model/Rule.h"

class Loader {
 public:
  virtual String load(String str, std::list<Rule*> &rules);
};

#endif
