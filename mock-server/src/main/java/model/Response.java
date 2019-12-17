package model;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import static java.util.Objects.requireNonNull;

public class Response {
    private int status;
    private Map<String, String> headers;
    private String body;

    public Response(Integer status, Map<String, String> headers, String body) {
        this.status = requireNonNull(status, "Parameter status is required.");
        if(status < 100 || status >= 600) throw new IllegalArgumentException("Wrong status code. Must be between 100 and 600.");
        this.headers = (headers != null)? headers : new TreeMap<>();
        this.body = (body != null) ? body : "";
        this.body = this.body.replace("\\r\\n", String.valueOf('\n'));
        this.body = this.body.replace("\\n", String.valueOf('\n'));
    }

    public int getStatus() {
        return status;
    }

    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    public String getBody() {
        return body;
    }
}
