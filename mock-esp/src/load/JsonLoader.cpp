#include "JsonLoader.h"

/**
 * This method parses the JSON string and adds the rules in the list. * 
 * @param str: The JSON string from which to load the rules.
 * @param rules: The rules list in which it adds each rule loaded.
 * @return A success/error message.
 */
String JsonLoader::load(String str, std::list<Rule *> &rules) {
  DynamicJsonDocument doc(ESP.getFreeHeap() / 2);
  DeserializationError error = deserializeJson(doc, str);
  if (error.code() != error.Ok) return "LoaderError: " + String(error.c_str());

  JsonArray json = doc.as<JsonArray>();
  if (json.isNull()) return F("LoaderError: Body must be an array.");

  for (size_t i = 0; i < json.size(); i++) {
    JsonObject object = json[i];
    if (object.isNull()) return F("LoaderError: Rule must be an object.");
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
      return F("LoaderError: Unsupported rule type.");
    }
    if (!ok.equals("OK")) return ok;
  }

  return F("OK");
}

/**
 * This method loads an InOutRule.
 * @param rule: The parsed rule.
 * @param inOutRule: The InOutRule which is gonna be instancied.
 * @return A success/error message.
 */
String JsonLoader::loadInOutRule(JsonObject &rule, InOutRule *&inOutRule) {
  Request *request = nullptr;
  Response *response = nullptr;
  String ok = loadRequest(rule, request);
  if (!ok.equals("OK")) return ok;
  ok = loadResponse(rule, response);
  if (!ok.equals("OK")) return ok;
  if (request == nullptr) return F("LoaderError: Request is required.");
  if (response == nullptr) return F("LoaderError: Response is required.");

  if (request->getPath().charAt(0) != '/') return F("LoaderError: InOutRule Request path must start with '/'.");
  inOutRule = new InOutRule(*request, *response);
  return F("OK");
}

/**
 * This method loads an OutInRule.
 * @param rule: The parsed rule.
 * @param outInRule: The OutInRule which is gonna be instancied.
 * @return A success/error message.
 */
String JsonLoader::loadOutInRule(JsonObject &rule, OutInRule *&outInRule) {
  Request *request = nullptr;
  Response *response = nullptr;
  String ok = loadRequest(rule, request);
  if (!ok.equals("OK")) return ok;
  ok = loadResponse(rule, response);
  if (!ok.equals("OK")) return ok;
  if (request == nullptr) return F("LoaderError: Request is required.");

  long timeout = (rule["timeout"].is<long>()) ? rule["timeout"].as<long>() : 0;
  int repeat = (rule["repeat"].is<int>()) ? rule["repeat"].as<int>() : 1;
  long interval = (rule["interval"].is<long>()) ? rule["interval"].as<long>() : 1000;

  String proto = request->getPath().substring(0, 7);
  if (!proto.equals("http://") && !proto.equals("https:/")) return F("LoaderError: OutInRules Request path must start with 'http' or 'https'.");

  outInRule = new OutInRule(*request, *response, timeout, repeat, interval);
  return F("OK");
}

/**
 * This method loads a rule request.
 * @param rule: The parsed rule.
 * @param request: The request which is gonna be instancied.
 * @return A success/error message.
 */
String JsonLoader::loadRequest(JsonObject &rule, Request *&request) {
  JsonObject req = rule["request"];
  if (req.isNull()) return "OK";
  String method = req["method"].as<String>();
  method = method.equals("null") ? "" : method;
  String path = req["path"].as<String>();
  std::map<String, String> *headers;
  loadHeaders(req, headers);
  String body = req["body"].as<String>();
  body = body.equals("null") ? "" : body;
  if (method.equals("")) return F("LoaderError: Method/Verb cannot be empty.");
  request = new Request(method, path, *headers, body);
  return F("OK");
}

/**
 * This method loads a rule response.
 * @param rule: The parsed rule.
 * @param request: The response which is gonna be instancied.
 * @return A success/error message.
 */
String JsonLoader::loadResponse(JsonObject &rule, Response *&response) {
  JsonObject res = rule["response"];
  if (res.isNull()) return "OK";
  int status = res["status"].as<int>();
  std::map<String, String> *headers;
  loadHeaders(res, headers);
  String body = res["body"].as<String>();
  body = body.equals("null") ? "" : body;
  if (status < 100 || status >= 600) return F("LoaderError: Status must be between 100 and 600.");
  response = new Response(status, *headers, body);
  return F("OK");
}

/**
 * This method loads a rule headers.
 * @param obj: The JSON object to get the rule headers.
 * @param headers: The headers map.
 * @return A success/error message.
 */
String JsonLoader::loadHeaders(JsonObject &obj, std::map<String, String> *&headers) {
  JsonObject heads = obj["headers"];
  headers = new std::map<String, String>();
  for (JsonPair header : heads) {
    headers->insert({header.key().c_str(), header.value().as<String>()});
  }
  return F("OK");
}
