package load;

import model.*;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.parser.ParserException;
import org.yaml.snakeyaml.scanner.ScannerException;

import java.util.*;


public class YamlLoader implements Loader {

    private static final String KEY_TYPE = "type";
    private static final String KEY_TIMEOUT = "timeout";
    private static final String KEY_REPEAT = "repeat";
    private static final String KEY_INTERVAL = "interval";
    private static final String KEY_REQUEST = "request";
    private static final String KEY_RESPONSE = "response";
    private static final String KEY_METHOD = "method";
    private static final String KEY_STATUS = "status";
    private static final String KEY_PATH = "path";
    private static final String KEY_HEADERS = "headers";
    private static final String KEY_BODY = "body";

    private static final String VALUE_INOUT = "inout";
    private static final String VALUE_OUTIN = "outin";

    /**
     * Parse and deserialize a Yaml formatted String
     *
     * @param yaml The Yaml formatted String from which to load the rules
     * @return The list of loaded rules
     */
    public List<Rule> load(String yaml) throws LoaderException {
        Yaml parser = new Yaml();
        List<Map> parsedRules;
        try {
            parsedRules = parser.load(yaml);
        } catch (ClassCastException e) {
            throw new LoaderException("Body must be a list.");
        } catch (ParserException |ScannerException e) {
            throw new LoaderException("Error while parsing yaml.");
        }

        if (parsedRules == null) throw new LoaderException("Body must not be empty");

        try {
            return loadRules(parsedRules);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new LoaderException(e.getMessage());
        }
    }

    private List<Rule> loadRules(List<Map> parsedRules) throws LoaderException {
        List<Rule> rules = new ArrayList<>();
        for (Map r : parsedRules) {
            String type = getCheckAndCast(r, KEY_TYPE, String.class);
            if (type == null) throw new LoaderException("Parameter type is required.");

            if (Objects.equals(type, VALUE_INOUT)) {
                rules.add(loadInOut(r));
            } else if (Objects.equals(type, VALUE_OUTIN)) {
                rules.add(loadOutIn(r));
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
    private InOutRule loadInOut(Map rule) throws LoaderException {
        Request req = loadRequest(getCheckAndCast(rule, KEY_REQUEST, Map.class));
        Response res = loadResponse(getCheckAndCast(rule, KEY_RESPONSE, Map.class));
        return new InOutRule(req, res);
    }

    /**
     * Load an OutInRule
     *
     * @param rule The parsed rule
     * @return The loaded InOutRule
     */
    private OutInRule loadOutIn(Map rule) throws LoaderException {
        Request req = loadRequest(getCheckAndCast(rule, KEY_REQUEST, Map.class));
        Response res = loadResponse(getCheckAndCast(rule, KEY_RESPONSE, Map.class));
        Long timeout = getCheckAndCast(rule, KEY_TIMEOUT, Long.class);
        Integer repeat = getCheckAndCast(rule, KEY_REPEAT, Integer.class);
        Long interval = getCheckAndCast(rule, KEY_INTERVAL, Long.class);
        return new OutInRule(req, res, timeout, repeat, interval);
    }

    /**
     * Load a Request
     *
     * @param req The request object as a Map
     * @return The loaded Request
     */
    private Request loadRequest(Map req) throws LoaderException {
        if (req == null) return null;

        String method = getCheckAndCast(req, KEY_METHOD, String.class);
        String path = getCheckAndCast(req, KEY_PATH, String.class);
        Map<String, String> headers = loadHeaders(getCheckAndCast(req, KEY_HEADERS, Map.class));
        String body = getCheckAndCast(req, KEY_BODY, String.class);
        return new Request(method, path, headers, body);
    }

    /**
     * Load a Response
     *
     * @param res The response object as a Map
     * @return The loaded Response
     */
    private Response loadResponse(Map res) throws LoaderException {
        if (res == null) return null;

        Integer status = getCheckAndCast(res, KEY_STATUS, Integer.class);
        Map<String, String> headers = loadHeaders(getCheckAndCast(res, KEY_HEADERS, Map.class));
        String body = getCheckAndCast(res, KEY_BODY, String.class);
        return new Response(status, headers, body);
    }

    private Map<String, String> loadHeaders(Map headers) throws LoaderException {
        if (headers == null) return null;
        Map<String, String> h = new HashMap<>();
        for (Object key : headers.keySet()) {
            try {
                h.put((String) key, (String) headers.get(key));
            } catch (ClassCastException e) {
                throw new LoaderException("Wrong type for key-value pair in headers, expected 'string: string'");
            }
        }
        return h;
    }

    /**
     * Get the key's value from the map and cast it
     *
     * @param m    The map to get the value from
     * @param key  The get of the value to get
     * @param type The type of the value to return
     * @param <T>  The type of the value to return
     * @return The casted value
     */
    private <T> T getCheckAndCast(Map m, String key, Class<T> type) throws LoaderException {
        if(m == null) return null;
        try {
            if (m.get(key) instanceof Integer && type.equals(Long.class)) {
                return type.cast(((Integer) m.get(key)).longValue());
            }
            return type.cast(m.get(key));
        } catch (ClassCastException e) {
            throw new LoaderException(String.format("Wrong type for '%s', expected %s", key, type.getSimpleName()));
        }
    }
}
