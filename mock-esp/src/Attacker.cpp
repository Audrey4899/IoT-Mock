#include "Attacker.h"
#include "OutputHandler.h"

Attacker::Attacker(std::list<OutInRule*> &rules) {
  this->rules = rules;
  this->scripts.push_back("<script>alert('XSS')</script>");
  this->scripts.push_back("<img src=javascript:alert('XSS')>");
  this->scripts.push_back("</script><script>alert('XSS')</script>");
  this->scripts.push_back("<svg onload=alert('XSS')>");
}

void Attacker::attack() {
  for(OutInRule* rule: attackRules) {
    outputHandlers.push_back(new OutputHandler(*rule)); 
  }
  for(OutputHandler* outputHandler: outputHandlers) {
    outputHandler->update();
  }
}
