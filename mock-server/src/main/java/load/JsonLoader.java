package load;
import model.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class JsonLoader implements Loader {
    @Override
    public List<Rule> load(String json){
        List<Rule> rules = new ArrayList<>();
        JSONArray array = new JSONArray(json);
        for (int i = 0; i < array.length(); i++){
            try{
                JSONObject rule = array.getJSONObject(i);
                if (!rule.has("type")) {
                    throw new IllegalArgumentException("La requête doit contenir un type");
                }
                String type = ((String) rule.get("type"));
                if (type.isEmpty()) {
                    throw new IllegalArgumentException("Le type de la requête doit être spécifié");
                }
                if (type.equals("inOut")) {
                    Request req;
                    if (rule.has("req")) {
                        if (!rule.getJSONObject("req").isEmpty()) {
                            JSONObject request = rule.getJSONObject("req");
                            req = parseRequest(request);
                        } else {
                            throw new IllegalArgumentException("inOut doit spécifier le contenu de la requête reçue.");
                        }
                    } else {
                        throw new IllegalArgumentException("inOut doit contenir la requête reçue");
                    }
                    Response res;
                    if (rule.has("res")) {
                        if (!rule.getJSONObject("res").isEmpty()) {
                            JSONObject response = rule.getJSONObject("res");
                            res = parseResponse(response);
                        } else {
                            throw new IllegalArgumentException("inOut doit contenir la réponse à envoyer");
                        }
                    } else {
                        throw new IllegalArgumentException("inOut doit spécifier le contenu de la réponse à envoyer.");
                    }
                    rules.add(new InOutRule(req, res));
                }
                if (type.equals("outIn")) {
                    Request req;
                    long timeout;
                    int repeat;
                    long interval;
                    if (rule.has("req")) {
                        if (!rule.getJSONObject("req").isEmpty()) {
                            //Request
                            JSONObject request = rule.getJSONObject("req");
                            if (request.has("method") && request.has("path") && request.has("timeout")
                                    && request.has("repeat") && request.has("interval")) {
                                String method = ((String) request.get("method"));
                                String path = ((String) request.get("path"));
                                String body = ((String) request.get("body"));
                                JSONObject headersObj = request.getJSONObject("headers");
                                Map<String, String> headers = parseHeaders(headersObj);
                                req = new Request(method, path, headers, body);
                                timeout = ((Long) request.get("timeout"));
                                repeat = ((int) request.get("repeat"));
                                interval = ((Long) request.get("interval"));
                            } else {
                                throw new IllegalArgumentException("La requête à envoyer doit contenir les champs method et path ainsi que timeout, interval et repeat");
                            }
                        } else {
                            throw new IllegalArgumentException("outIn doit spécifier le contenu de la requête à envoyer");
                        }
                    } else {
                        throw new IllegalArgumentException("outIn doit contenir la requête à envoyer");
                    }
                    Response res;
                    if (rule.has("res")) {
                        if (!rule.getJSONObject("res").isEmpty()) {
                            //Response
                            JSONObject response = rule.getJSONObject("res");
                            res = parseResponse(response);
                        } else {
                            throw new IllegalArgumentException("outIn doit contenir une réponse");
                        }
                    } else {
                        throw new IllegalArgumentException("outIn doit spécifier le contenu de la réponse");
                    }
                    rules.add(new OutInRule(req, res, timeout, repeat, interval));
                }
            }
            catch (ClassCastException e){
                e.getMessage();
            }
        }
        return rules;
    }
    private Map<String, String> parseHeaders(JSONObject headersObj){
        Map<String, String> headers = new TreeMap<>();
        if (headersObj.keys().hasNext()){
            int nbKeys = headersObj.names().length();
            for (int i = 0; i < nbKeys; i++){
                String keyName = (String) headersObj.names().get(i);
                String content = headersObj.getString(keyName);
                headers.put(keyName, content);
            }
        }
        return headers;
    }
    private Request parseRequest(JSONObject request){
        if (request.has("method") && request.has("path")) {
            String method = ((String) request.get("method"));
            String path = ((String) request.get("path"));
            Map<String, String> headers;
            String body;
            if (!request.has("body") && request.has("headers")){
                JSONObject headersObj = request.getJSONObject("headers");
                headers = parseHeaders(headersObj);
                return new Request(method, path, headers, null);
            }
            if (!request.has("headers") && request.has("body")){
                body = ((String) request.get("body"));
                return new Request(method, path, null, body);
            }
            if (!request.has("headers") && !request.has("body")){
                return new Request(method, path, null, null);
            }
            JSONObject headersObj = request.getJSONObject("headers");
            headers = parseHeaders(headersObj);
            body = ((String) request.get("body"));
            return new Request(method, path, headers, body);
        } else {
            throw new IllegalArgumentException("La requête reçue doit contenir les champs method et path");
        }
    }
    private Response parseResponse(JSONObject response){
        if (response.has("status")) {
            Map<String, String> headers;
            String body;
            int status = response.getInt("status");
            if (!response.has("body") && response.has("headers")){
                JSONObject headersObj = response.getJSONObject("headers");
                headers = parseHeaders(headersObj);
                return new Response(status, headers,null);
            }
            if (!response.has("headers") && response.has("body")){
                body = ((String) response.get("body"));
                return new Response(status, null, body);
            }
            if (!response.has("headers") && !response.has("body")){
                return new Response(status,null, null);
            }
            JSONObject headersObj = response.getJSONObject("headers");
            headers = parseHeaders(headersObj);
            body = ((String) response.get("body"));
            return new Response(status, headers, body);
        } else {
            throw new IllegalArgumentException("La réponse doit au moins contenir un status");
        }
    }
}
