package load;

import model.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonLoader implements Loader {
    /**
     * Parse and deserialize a JSON formatted String
     *
     * @param json The JSON formatted String from which to load the rules
     * @return The list of loaded rules
     */
    @Override
    public List<Rule> load(String json) throws LoaderException {
        List<Rule> rules = new ArrayList<>();
        JSONArray array;
        try {
            array = new JSONArray(json);
        } catch (ClassCastException e) {
            throw new LoaderException("Body must be a list.");
        } catch (JSONException e) {
            throw new LoaderException("JSON parsing error");
        }

        try {
            for (int i = 0; i < array.length(); i++) {
                JSONObject rule = array.getJSONObject(i);
                String type = getCheckAndCast(rule, "type", String.class);
                if (type == null) throw new LoaderException("Parameter type is required.");
                if (type.equals("inout")) {
                    rules.add(loadInOut(rule));
                } else if (type.equals("outin")) {
                    rules.add(loadOutIn(rule));
                } else {
                    throw new LoaderException("Unsupported rule type");
                }
            }
            return rules;
        } catch (IllegalArgumentException | NullPointerException e){
            throw new LoaderException(e.getMessage());
        }
    }

    /**
     * Load an InOutRule
     *
     * @param rule The parsed rule
     * @return The loaded InOutRule
     */
    private InOutRule loadInOut(JSONObject rule) throws LoaderException {
        Request req = loadRequest(getCheckAndCast(rule, "request", JSONObject.class));
        Response res = loadResponse(getCheckAndCast(rule, "response", JSONObject.class));
        return new InOutRule(req, res);
    }

    /**
     * Load an OutInRule
     *
     * @param rule The parsed rule
     * @return The loaded InOutRule
     */
    private OutInRule loadOutIn(JSONObject rule) throws LoaderException {
        Request req = loadRequest(getCheckAndCast(rule, "request", JSONObject.class));
        Response res = loadResponse(getCheckAndCast(rule, "response", JSONObject.class));
        Long timeout = getCheckAndCast(rule, "timeout", Long.class);
        Integer repeat = getCheckAndCast(rule, "repeat", Integer.class);
        Long interval = getCheckAndCast(rule, "interval", Long.class);
        return new OutInRule(req, res, timeout, repeat, interval);
    }

    /**
     * Load a Request
     *
     * @param req The request object as a Map
     * @return The loaded Request
     */
    private Request loadRequest(JSONObject req) throws LoaderException {
        if (req == null) return null;

        String method = getCheckAndCast(req, "method", String.class);
        String path = getCheckAndCast(req, "path", String.class);
        Map<String, String> headers = loadHeaders(req);
        String body = getCheckAndCast(req, "body", String.class);
        return new Request(method, path, headers, body);
    }

    /**
     * Load a Response
     *
     * @param res The response object as a Map
     * @return The loaded Response
     */
    private Response loadResponse(JSONObject res) throws LoaderException {
        if (res == null) return null;

        Integer status = getCheckAndCast(res, "status", Integer.class);
        Map<String, String> headers = loadHeaders(res);
        String body = getCheckAndCast(res, "body", String.class);
        return new Response(status, headers, body);
    }

    /**
     * Load a Headers
     *
     * @param headers The JSONObject to get it headers
     * @return The loaded Headers
     */
    private Map<String, String> loadHeaders(JSONObject headers) throws LoaderException {
        JSONObject headersObj = getCheckAndCast(headers, "headers", JSONObject.class);
        if(headersObj == null) return null;
        Map<String, String> headersList = new HashMap<>();
        for (String k: headersObj.keySet()) {
            headersList.put(k, getCheckAndCast(headersObj, k, String.class));
        }
        return headersList;
    }

    /**
     * Get the key's value from the JSONObject and cast it
     *
     * @param o    The JSONObject to get the value from
     * @param key  The get of the value to get
     * @param type The type of the value to return
     * @param <T>  The type of the value to return
     * @return The casted value
     */
    private <T> T getCheckAndCast(JSONObject o, String key, Class<T> type) throws LoaderException {
        try {
            if (o.has(key)) {
                if (o.get(key) instanceof Integer && type.equals(Long.class)) {
                    return type.cast(((Integer) o.get(key)).longValue());
                }
                return type.cast(o.get(key));
            } else
                return null;
        } catch (ClassCastException e) {
            throw new LoaderException(String.format("Wrong type for '%s', expected %s", key, type.getSimpleName()));
        }
    }
}