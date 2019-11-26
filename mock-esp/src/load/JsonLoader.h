#ifndef YAMLLOADER_H
#define YAMLLOADER_H

#include "Loader.h"
#include <ArduinoJson.hpp>
#include "model/Request.h"
#include "model/Response.h"
#include "model/InOutRule.h"
#include "model/OutInRule.h"

using namespace ArduinoJson;

class JsonLoader : public Loader {
 private:
  String loadInOutRule(JsonObject &rule, InOutRule *&inOutRule);
  String loadOutInRule(JsonObject &rule, OutInRule *&outInRule);
  String loadRequest(JsonObject &rule, Request *&request);
  String loadResponse(JsonObject &rule, Response *&response);

 public:
  String load(String str, std::list<Rule *> &rules) override;
};

#endif
