package load;

import model.*;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.parser.ParserException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class YamlLoader implements Loader {

    /**
     * Parse and deserialize a Yaml formatted String
     * @param yaml The Yaml formatted String from which to load the rules
     * @return The list of loaded rules
     */
    public List<Rule> load(String yaml) throws LoaderException {
        List<Rule> rules = new ArrayList<>();
        Yaml parser = new Yaml();
        List<Map> parsedRules;
        try {
            parsedRules = parser.load(yaml);
        } catch (ClassCastException e) {
            throw new LoaderException("Body must be a list.");
        } catch (ParserException e) {
            throw new LoaderException(e);
        }

        if (parsedRules == null) throw new LoaderException("Body must not be empty");

        for (Map r: parsedRules) {
            String type = (String) r.get("type");

            if (Objects.equals(type, "inOut")) {
                rules.add(loadInOut(r));
            } else if (Objects.equals(type, "outIn")) {
                rules.add(loadOutIn(r));
            } else {
                throw new LoaderException();
            }
        }

        return rules;
    }

    /**
     * Load an InOutRule
     * @param rule The parsed rule
     * @return The loaded InOutRule
     */
    private InOutRule loadInOut(Map rule) throws LoaderException {
        Request req = loadRequest(rule);
        Response res = loadResponse(rule);
        if (req == null || res == null) throw new LoaderException();
        if(!req.getPath().startsWith("/")) throw new LoaderException(String.format("Wrong path format: '%s'. Must start with /", req.getPath()));
        return new InOutRule(req, res);
    }

    /**
     * Load an OutInRule
     * @param rule The parsed rule
     * @return The loaded InOutRule
     */
    private OutInRule loadOutIn(Map rule) throws LoaderException {
        Request req = loadRequest(rule);
        Response res = loadResponse(rule);
        if (req == null) throw new LoaderException();
        if(!req.getPath().matches("^https?://.*$")) throw new LoaderException(String.format("Wrong path format: '%s'. Must start with http:// or https://", req.getPath()));
        Long timeout = getCheckAndCast(rule, "timeout", Long.class);
        Integer repeat = getCheckAndCast(rule, "repeat", Integer.class);
        Long interval = getCheckAndCast(rule, "interval", Long.class);
        return new OutInRule(req, res, (timeout != null) ? timeout : 0, (repeat != null) ? repeat : 1, (interval != null) ? interval : 1000);
    }

    /**
     * Load a Request
     * @param rule The parsed rule containing the request
     * @return The loaded Request
     */
    private Request loadRequest(Map rule) throws LoaderException {
        Map req = getCheckAndCast(rule, "req", Map.class);
        if (req == null) return null;

        String method = getCheckAndCastNotNull(req, "method", String.class);
        String path = getCheckAndCastNotNull(req, "path", String.class);
        Map<String, String> headers = getCheckAndCast(req, "headers", Map.class);
        String body = getCheckAndCast(req, "body", String.class);
        return new Request(method, path, headers, body);
    }

    /**
     * Load a Response
     * @param rule The parsed rule containing the response
     * @return The loaded Response
     */
    private Response loadResponse(Map rule) throws LoaderException {
        Map res = getCheckAndCast(rule, "res", Map.class);
        if (res == null) return null;

        Integer status = getCheckAndCastNotNull(res, "status", Integer.class);
        Map<String, String> headers = getCheckAndCast(res, "headers", Map.class);
        String body = getCheckAndCast(res, "body", String.class);
        return new Response(status, headers, body);
    }

    /**
     * Get the key's value from the map and cast it
     * @param m The map to get the value from
     * @param key The get of the value to get
     * @param type The type of the value to return
     * @param <T> The type of the value to return
     * @return The casted value
     */
    private <T> T getCheckAndCast(Map m, String key, Class<T> type) throws LoaderException {
        try {
            if(m.get(key) instanceof Integer && type.equals(Long.class)) {
                return type.cast(((Integer) m.get(key)).longValue());
            }
            return type.cast(m.get(key));
        } catch (ClassCastException e) {
            throw new LoaderException(String.format("Wrong type for '%s', expected %s", key, type.getSimpleName()));
        }
    }

    /**
     * Get the key's value from the map and cast it
     * @param m The map to get the value from
     * @param key The get of the value to get
     * @param type The type of the value to return
     * @param <T> The type of the value to return
     * @return The casted non-null value
     */
    private <T> T getCheckAndCastNotNull(Map m, String key, Class<T> type) throws LoaderException {
        T t = getCheckAndCast(m, key, type);
        if (t == null) throw new LoaderException(String.format("Missing parameter '%s'", key));
        return t;
    }
}
