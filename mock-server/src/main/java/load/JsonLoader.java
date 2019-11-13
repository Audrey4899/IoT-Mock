package load;

import model.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
            throw new LoaderException("Missing body");
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject rule = array.getJSONObject(i);
            String type = getCheckAndCastNotNull(rule, "type", String.class);
            if (type.equals("inOut")) {
                rules.add(loadInOut(rule));
            } else if (type.equals("outIn")) {
                rules.add(loadOutIn(rule));
            } else {
                throw new LoaderException("Unsupported rule type");
            }
        }
        return rules;
    }

    /**
     * Load an InOutRule
     *
     * @param rule The parsed rule
     * @return The loaded InOutRule
     */
    private InOutRule loadInOut(JSONObject rule) throws LoaderException {
        Request req = loadRequest(getCheckAndCastNotNull(rule, "req", JSONObject.class));
        Response res = loadResponse(getCheckAndCastNotNull(rule, "res", JSONObject.class));
        if (req == null || res == null) throw new LoaderException();
        if (!req.getPath().startsWith("/"))
            throw new LoaderException(String.format("Wrong path format: '%s'. Must start with /", req.getPath()));
        return new InOutRule(req, res);
    }

    /**
     * Load an OutInRule
     *
     * @param rule The parsed rule
     * @return The loaded InOutRule
     */
    private OutInRule loadOutIn(JSONObject rule) throws LoaderException {
        Request req = loadRequest(getCheckAndCastNotNull(rule, "req", JSONObject.class));
        Response res = loadResponse(getCheckAndCast(rule, "res", JSONObject.class));
        if (req == null) throw new LoaderException();
        if (!req.getPath().matches("^https?://.*$"))
            throw new LoaderException(String.format("Wrong path format: '%s'. Must start with http:// or https://", req.getPath()));
        Long timeout = getCheckAndCast(rule, "timeout", Long.class);
        Integer repeat = getCheckAndCast(rule, "repeat", Integer.class);
        Long interval = getCheckAndCast(rule, "interval", Long.class);
        return new OutInRule(req, res, (timeout != null) ? timeout : 0, (repeat != null) ? repeat : 1, (interval != null) ? interval : 1000);
    }

    /**
     * Load a Request
     *
     * @param req The request object as a Map
     * @return The loaded Request
     */
    private Request loadRequest(JSONObject req) throws LoaderException {
        if (req == null) return null;

        String method = getCheckAndCastNotNull(req, "method", String.class);
        if (method.isEmpty()) throw new LoaderException(String.format("Parameter '%s' cannot be empty.", "method"));
        String path = getCheckAndCastNotNull(req, "path", String.class);
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

        Integer status = getCheckAndCastNotNull(res, "status", Integer.class);
        if (status < 100 || status >= 600) throw new LoaderException("Wrong status code. Must be between 100 and 600.");
        Map<String, String> headers = loadHeaders(res);
        String body = getCheckAndCast(res, "body", String.class);
        return new Response(status, headers, body);
    }

    /**
     * Load a Headers
     *
     * @param o The JSONObject to get it headers
     * @return The loaded Headers
     */
    private Map<String, String> loadHeaders(JSONObject o) throws LoaderException {
        JSONObject headersRes = getCheckAndCast(o, "headers", JSONObject.class);
        Map<String, String> headers = new TreeMap<>();
        if (headersRes != null) {
            if (headersRes.keys().hasNext()) {
                String keyName = headersRes.keys().next();
                String content = headersRes.getString(keyName);
                headers.put(keyName, content);
            }
            return headers;
        } else
            return null;
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

    /**
     * Get the key's value from the JSONObject and cast it
     *
     * @param o    The JSONObject to get the value from
     * @param key  The get of the value to get
     * @param type The type of the value to return
     * @param <T>  The type of the value to return
     * @return The casted non-null value
     */
    private <T> T getCheckAndCastNotNull(JSONObject o, String key, Class<T> type) throws LoaderException {
        T t = getCheckAndCast(o, key, type);
        if (t == null) throw new LoaderException(String.format("Missing parameter '%s'", key));
        return t;
    }
}