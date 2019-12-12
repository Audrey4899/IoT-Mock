#ifndef ATTACKER_H
#define ATTACKER_H

#include <list>
#include <Arduino.h>
#include "model/OutInRule.h"
#include "OutputHandler.h"

class Attacker {
 private:
  std::list<OutputHandler*> outputHandlers;
  std::list<OutInRule*> rules;
  std::list<OutInRule*> attackRules;
  std::list<String> scripts;
  std::list<String> getPaths();
  void XSSQueryParams();
  void XSSHeaders(String script);
  void XSSBody(String script);
  void verbNotExist();
  void emptyVerb();
  void specialChar();

 public:
  Attacker(std::list<OutInRule*> &rules);
  std::list<OutInRule*> attack();
  void XSSAttacks();
  void httpFloodAttack();
  void robustnessAttack();
};

#endif
