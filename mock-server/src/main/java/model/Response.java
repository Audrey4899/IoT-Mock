package model;

import java.util.Collections;
import java.util.Map;

public class Response {
    private int status;
    private Map<String, String> headers;
    private String body;

    public Response(int status, Map<String, String> headers, String body) {
        this.status = status;
        this.headers = headers;
        this.body = body;
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
