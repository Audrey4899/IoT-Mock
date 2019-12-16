#include "Attacker.h"
#include "OutputHandler.h"
#include <map>

/**
 * The constructor adds the rules and XSS scripts to the different lists.
 * @param rules: mock rules given by the user.
 */
Attacker::Attacker(std::list<OutInRule*> &rules) {
  this->rules = rules;
  this->scripts.push_back("<script>alert('XSS')</script>");
  this->scripts.push_back("<img src=javascript:alert('XSS')>");
  this->scripts.push_back("</script><script>alert('XSS')</script>");
  this->scripts.push_back("<svg onload=alert('XSS')>");
}

/**
 * @return The list of attack rules created
 */
std::list<OutInRule*> Attacker::attack() {
  return attackRules;
}

/**
 * This method calls the 3 different XSS methods.
 * The first one uses a different script because the script has to be encoded to be sent
 * in the http request query parameters.
 */
void Attacker::XSSAttacks() {
  XSSQueryParams();
  for(String script: scripts) {
    XSSHeaders(script);
    XSSBody(script);
  }
}

/**
 * This method extracts all the query parameters from the http request URI and changes each
 * parameter value to the XSS script.
 */
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

/**
 * This method extracts all the headers from the http request and changes each
 * value to the XSS script.
 * @param script: the XSS script which is gonna be injected.
 */
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

/**
 * This method extracts the body from the http request and changes its value to the XSS script.
 * @param script: the XSS script which is gonna be injected.
 */
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

/**
 * For each OutIn rules, it gets the path and extracts the IP address or the domain name.
 * @return The list of the paths contained in OutIn rules.
 */
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

/**
 * Add each random alphanumeric character to the request body.
 * For each connected object, it creates an OutInRule with the random String generated in the request body.
 */ 
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

/**
 * This method calls all or selected robustness attacks.
 */
void Attacker::robustnessAttack() {
  verbNotExist();
  emptyVerb();
  specialChar();
}

/**
 * For each existing OutInRule, it replaces the original method with an illegal HTTP method
 * and adds it in the attackRules list.
 */
void Attacker::verbNotExist() {
  for (OutInRule* rule : rules) {
    Request* request = new Request("WrongVerb", rule->getRequest().getPath(), rule->getRequest().getHeaders(), rule->getRequest().getBody());
    Response* response = new Response(0, std::map<String, String>(), "");
    attackRules.push_back(new OutInRule(*request, *response, 0L, 1, 0L));
  }
}

/**
 * For each existing OutInRule, it replaces the original method with an empty parameter.
 */
void Attacker::emptyVerb() {
  for (OutInRule* rule : rules) {
    Request* request = new Request("", rule->getRequest().getPath(), rule->getRequest().getHeaders(), rule->getRequest().getBody());
    Response* response = new Response(0, std::map<String, String>(), "");
    attackRules.push_back(new OutInRule(*request, *response, 0L, 1, 0L));
  }
}

/**
 * For each existing OutInRule, it adds two special characters in the original HTTP method.
 */
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
