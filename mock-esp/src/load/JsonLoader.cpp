#include "JsonLoader.h"

String JsonLoader::load(String str, std::list<Rule *> &rules) {
  DynamicJsonDocument doc(2048);
  DeserializationError error = deserializeJson(doc, str);
  if (error.code() != error.Ok) return "LoaderError: " + String(error.c_str());

  JsonArray json = doc.as<JsonArray>();
  if (json.isNull()) return "LoaderError: Body must be an array.";

  for (size_t i = 0; i < json.size(); i++) {
    JsonObject object = json[i];
    if (object.isNull()) return "LoaderError: Rule must be an object.";
    String type = object["type"].as<String>();
    String ok;

    if (type.equals("inout")) {
      InOutRule *rule;
      ok = loadInOutRule(object, rule);
      rules.push_back(rule);
    } else if (type.equals("outin")) {
      OutInRule *rule;
      ok = loadOutInRule(object, rule);
      rules.push_back(rule);
    } else {
      return "LoaderError: Unsupported rule type.";
    }
    if(!ok.equals("OK")) return ok;
  }

  return "OK";
}

String JsonLoader::loadInOutRule(JsonObject &rule, InOutRule *&inOutRule) {
  Request *request;
  Response *response;
  String ok = loadRequest(rule, request);
  if(!ok.equals("OK")) return ok;
  ok = loadResponse(rule, response);
  if(!ok.equals("OK")) return ok;
  if(request==nullptr) return "LoaderError: Request is required.";
  if(response==nullptr) return "LoaderError: Response is required.";

  if(request->getPath().charAt(0) != '/') return "LoaderError: InOutRule Request path must start with '/'.";
  inOutRule = new InOutRule(*request, *response);
  return "OK";
}

String JsonLoader::loadOutInRule(JsonObject &rule, OutInRule *&outInRule) {
  Request *request;
  Response *response;
  String ok = loadRequest(rule, request);
  if(!ok.equals("OK")) return ok;
  ok = loadResponse(rule, response);
  if(!ok.equals("OK")) return ok;
  if(request==nullptr) return "LoaderError: Request is required.";

  long timeout = (rule["timeout"].is<long>())? rule["timeout"].as<long>(): 0;
  int repeat = (rule["repeat"].is<int>())? rule["repeat"].as<int>(): 1;
  long interval = (rule["interval"].is<long>())? rule["interval"].as<long>(): 1000;
  
  String proto = request->getPath().substring(0, 7);
  if(!proto.equals("http://") && !proto.equals("https:/")) return "LoaderError: OutInRules Request path must start with 'http' or 'https'.";
  
  outInRule = new OutInRule(*request, *response, timeout, repeat, interval);
  return "OK";
}

String JsonLoader::loadRequest(JsonObject &rule, Request *&request) {
  JsonObject req = rule["request"];
  if(req.isNull()) return "OK";
  String method = req["method"].as<String>();
  method = method.equals("null") ? "" : method;
  String path = req["path"].as<String>();
  std::map<String, String> *headers;
  loadHeaders(req, headers);
  String body = req["body"].as<String>();
  body = body.equals("null") ? "" : body;
  if(method.equals("")) return "LoaderError: Method/Verb cannot be empty.";
  request = new Request(method, path, *headers, body);
  return "OK";
}

String JsonLoader::loadResponse(JsonObject &rule, Response *&response) {
  JsonObject res = rule["response"];
  if(res.isNull()) return "OK";
  int status = res["status"].as<int>();
  std::map<String, String> *headers;
  loadHeaders(res, headers);
  String body = res["body"].as<String>();
  body = body.equals("null") ? "" : body;
  if(status < 100 || status >= 600) return "LoaderError: Status must be between 100 and 600.";
  response = new Response(status, *headers, body);
  return "OK";
}

String JsonLoader::loadHeaders(JsonObject &obj, std::map<String, String> *&headers) {
  JsonObject heads = obj["headers"];
  headers = new std::map<String, String>();
  for (JsonPair header : heads) {
    headers->insert({header.key().c_str(), header.value().as<String>()});
  }
  return "OK";
}
