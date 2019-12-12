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
  
std::list<String> Attacker::getPaths() {
    std::list<String> paths;
    String group;
    size_t pos = 0;
    for (OutInRule* rule : rules)
    {
      String path = rule->getRequest().getPath();
      pos = path.indexOf("/");
      pos = path.indexOf("/", pos+2);
      path =  path.substring(0, pos);
    }
    return paths;
}

void Attacker::httpFloodAttack() {
  String body = "";
  String allowedChar = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
  int size = 1000;
  for (int i = 0; i < size; i++) {
    long rdmNbr = random(0, allowedChar.length());
    char rdmChar = allowedChar[rdmNbr];
    body.concat(rdmChar);
  }
  for (String path : getPaths()) {
    Response* response = new Response(0, std::map<String, String>(), "");
    Request* request = new Request("POST", path, std::map<String, String>(), body);
    attackRules.push_back(new OutInRule(*request, *response, 0L, 50, 0L));
  }
}

void Attacker::robustnessAttack() {
  verbNotExist();
  emptyVerb();
  specialChar();
}

void Attacker::verbNotExist() {
  for (OutInRule* rule : rules) {
    Request* request = new Request("WrongVerb", rule->getRequest().getPath(), rule->getRequest().getHeaders(), rule->getRequest().getBody());
    Response* response = new Response(0, std::map<String, String>(), "");
    attackRules.push_back(new OutInRule(*request, *response, 0L, 1, 0L));
  }
}

void Attacker::emptyVerb() {
  for (OutInRule* rule : rules) {
    Request* request = new Request("", rule->getRequest().getPath(), rule->getRequest().getHeaders(), rule->getRequest().getBody());
    Response* response = new Response(0, std::map<String, String>(), "");
    attackRules.push_back(new OutInRule(*request, *response, 0L, 1, 0L));
  }
}

void Attacker::specialChar() {
  for (OutInRule* rule : rules) {
    String method = rule->getRequest().getMethod();
    method.setCharAt(1, '@');
    method.setCharAt(3, '&');
    Request* request = new Request(method, rule->getRequest().getPath(), rule->getRequest().getHeaders(), rule->getRequest().getBody());
    Response* response = new Response(0, std::map<String, String>(), "");
    attackRules.push_back(new OutInRule(*request, *response, 0L, 1, 0L));
  }
}
