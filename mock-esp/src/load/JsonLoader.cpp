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

    if (type.equals("inout")) {
      InOutRule *rule;
      loadInOutRule(object, rule);
      rules.push_back(rule);
    } else if (type.equals("outin")) {
      OutInRule *rule;
      loadOutInRule(object, rule);
      rules.push_back(rule);
    } else {
      return "LoaderError: Unusupported rule type.";
    }
  }

  return "OK";
}

String JsonLoader::loadInOutRule(JsonObject &rule, InOutRule *&inOutRule) {
  Request *request;
  Response *response;
  loadRequest(rule, request);
  loadResponse(rule, response);
  // TODO: check OK

  inOutRule = new InOutRule(*request, *response);
  return "OK";
}

String JsonLoader::loadOutInRule(JsonObject &rule, OutInRule *&outInRule) {
  Request *request;
  Response *response;
  loadRequest(rule, request);
  loadResponse(rule, response);
  long timeout = rule["timeout"].as<long>();
  int repeat = rule["repeat"].as<int>();
  long interval = rule["interval"].as<long>();
  // TODO: check OK
  outInRule = new OutInRule(*request, *response, timeout, repeat, interval);
  return "OK";
}

String JsonLoader::loadRequest(JsonObject &rule, Request *&request) {
  JsonObject req = rule["request"];
  String method = req["method"].as<String>();
  String path = req["path"].as<String>();
  // TODO: load headers
  String body = req["body"].as<String>();
  // TODO: check OK

  request = new Request(method, path, std::map<String, String>(), body);
  return "OK";
}

String JsonLoader::loadResponse(JsonObject &rule, Response *&response) {
  JsonObject res = rule["response"];
  int status = res["status"].as<int>();
  // TODO: load headers
  String body = res["body"].as<String>();
  // TODO: create Response & check OK

  response = new Response(status, std::map<String, String>(), body);
  return "OK";
}
