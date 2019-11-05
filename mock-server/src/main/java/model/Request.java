package model;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class Request {
    private String method;
    private String path;
    private Map<String, String> headers;
    private String body;

    public Request(String method, String path, Map<String, String> headers, String body) {
        this.method = method;
        this.path = path;
        this.headers = (headers != null)? headers : new TreeMap<>();
        this.body = (body != null)? body : "";
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    public String getBody() {
        return body;
    }
}
