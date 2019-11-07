import model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.parser.ParserException;


public class YamlLoader {

    public List<Rule> load(String yaml) {
        List<Rule> rules = new ArrayList<>();
        Yaml parser = new Yaml();
        List<Map> test;
        try {
            test = parser.load(yaml);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Body must be a list.");
        } catch (ParserException e) {
            throw new IllegalArgumentException(e);
        }

        if (test == null) throw new IllegalArgumentException("Body must not be empty");

        test.forEach(r -> {
            String type = (String) r.get("type");

            if (Objects.equals(type, "inOut")) {
                rules.add(parseInOut(r));
            } else if (Objects.equals(type, "outIn")) {
                rules.add(parseOutIn(r));
            } else {
                throw new IllegalArgumentException();
            }
        });

        return rules;
    }

    private InOutRule parseInOut(Map rule) {
        Request req = parseRequest(rule);
        Response res = parseResponse(rule);
        if (req == null || res == null) throw new IllegalArgumentException();
        if(!req.getPath().startsWith("/")) throw new IllegalArgumentException(String.format("Wrong path format: '%s'. Must start with /", req.getPath()));
        return new InOutRule(req, res);
    }

    private OutInRule parseOutIn(Map rule) {
        Request req = parseRequest(rule);
        Response res = parseResponse(rule);
        if (req == null) throw new IllegalArgumentException();
        if(!req.getPath().matches("^https?://.*$")) throw new IllegalArgumentException(String.format("Wrong path format: '%s'. Must start with http:// or https://", req.getPath()));
        Long timeout = getCheckAndCast(rule, "timeout", Long.class);
        Integer repeat = getCheckAndCast(rule, "repeat", Integer.class);
        Long interval = getCheckAndCast(rule, "interval", Long.class);
        return new OutInRule(req, res, (timeout != null) ? timeout : 0, (repeat != null) ? repeat : 1, (interval != null) ? interval : 1000);
    }

    private Request parseRequest(Map rule) {
        Map req = getCheckAndCast(rule, "req", Map.class);
        if (req == null) return null;

        String method = getCheckAndCastNotNull(req, "method", String.class);
        String path = getCheckAndCastNotNull(req, "path", String.class);
        Map<String, String> headers = getCheckAndCast(req, "headers", Map.class);
        String body = getCheckAndCast(req, "body", String.class);
        return new Request(method, path, headers, body);
    }

    private Response parseResponse(Map rule) {
        Map res = getCheckAndCast(rule, "res", Map.class);
        if (res == null) return null;

        Integer status = getCheckAndCastNotNull(res, "status", Integer.class);
        Map<String, String> headers = getCheckAndCast(res, "headers", Map.class);
        String body = getCheckAndCast(res, "body", String.class);
        return new Response(status, headers, body);
    }

    private <T> T getCheckAndCast(Map m, String key, Class<T> type) {
        try {
            return type.cast(m.get(key));
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(String.format("Wrong type for '%s', expected %s", key, type.getSimpleName()));
        }
    }

    private <T> T getCheckAndCastNotNull(Map m, String key, Class<T> type) {
        T t = getCheckAndCast(m, key, type);
        if (t == null) throw new IllegalArgumentException(String.format("Missing parameter '%s'", key));
        return t;
    }
}
