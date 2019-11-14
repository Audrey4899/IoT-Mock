package model;

import static java.util.Objects.requireNonNull;

public class InOutRule extends Rule {

    public InOutRule(Request request, Response response) {
        super(requireNonNull(request, "Parameter request is required."), requireNonNull(response, "Parameter response is required."));
        if (!request.getPath().startsWith("/"))
            throw new IllegalArgumentException(String.format("Wrong path format: '%s'. Must start with /", request.getPath()));
    }
}
