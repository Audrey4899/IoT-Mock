#include "Attacker.h"
#include "OutputHandler.h"
#include <map>
#include <regex>

Attacker::Attacker(std::list<OutInRule*> &rules) {
  this->rules = rules;
  this->scripts.push_back("<script>alert('XSS')</script>");
  this->scripts.push_back("<img src=javascript:alert('XSS')>");
  this->scripts.push_back("</script><script>alert('XSS')</script>");
  this->scripts.push_back("<svg onload=alert('XSS')>");
}

std::list<OutInRule*> Attacker::attack() {
  return attackRules;
}

void Attacker::XSSAttacks() {
  XSSQueryParams();
  for(String script: scripts) {
    XSSHeaders(script);
    XSSBody(script);
  }
}

void Attacker::XSSQueryParams() {
  String path, token, tempQueryParams;
  size_t pos = 0;
  std::list<String> queryParams;
  std::map<String,String> params;
  std::map<String,String>::iterator it;
  for(OutInRule* rule: rules) {
    params.clear(); 
    path = rule->getRequest().getPath();
    pos = path.indexOf("?");
    tempQueryParams = path.substring(pos+1, path.length());
    path.remove(pos+1, path.length()-1);
    while ((pos = tempQueryParams.indexOf("&")) != std::string::npos) {
      token = tempQueryParams.substring(0, pos);
      tempQueryParams.remove(0,pos + 1);
      queryParams.push_back(token);
    }
    queryParams.push_back(tempQueryParams);
    for(String param: queryParams) {
      pos = param.indexOf("=");
      token = param.substring(0, pos);
      params.insert({token, "%3Cscript%3Ealert%28%22XSS%22%29%3C%2Fscript%3E"});
      //TODO: tester avec scripts de base ?
    }
    for(it = params.begin(); it != params.end(); it++) {
      path += it->first + "=" + it->second + "&";
    }
    path.remove(path.length()-1);
    Request* request = new Request(rule->getRequest().getMethod(),path,rule->getRequest().getHeaders(),rule->getRequest().getBody());
    Response* response = new Response(0,std::map<String,String>(),"");
    attackRules.push_back(new OutInRule(*request,*response,0L,1,0L));
  }
}

void Attacker::XSSHeaders(String script) {
  std::map<String,String> headers;
  for(OutInRule* rule: rules) {
    if(rule->getRequest().getHeaders().size() != 0) {
      for (auto &&h : rule->getRequest().getHeaders()) {
        headers.insert({h.first, script});
      }
      Request* request = new Request(rule->getRequest().getMethod(),rule->getRequest().getPath(),headers,rule->getRequest().getBody());
      Response* response = new Response(0,std::map<String,String>(),"");
      attackRules.push_back(new OutInRule(*request,*response,0L,1,0L));
    } else return;
  }
}

void Attacker::XSSBody(String script) {
  String body = "";
  for(OutInRule* rule: rules) {
    body = rule->getRequest().getBody();
    if(!body.equals("")) {
      body = script;
      Request* request = new Request(rule->getRequest().getMethod(),rule->getRequest().getPath(),rule->getRequest().getHeaders(),body);
      Response* response = new Response(0,std::map<String,String>(),"");
      attackRules.push_back(new OutInRule(*request,*response,0L,1,0L));
    } else return;
  }
}

void Attacker::httpFloodAttack() {

}

void Attacker::robustnessAttack() {

}

std::list<String> Attacker::getPaths() {
  std::list<String> rien;
  return rien;
}

void Attacker::generateBigFile() {

}

String Attacker::readFile() {
  String rien = "";
  return rien;
}

void Attacker::verbNotExist() {

}

void Attacker::emptyVerb() {

}

void Attacker::specialChar() {

}
