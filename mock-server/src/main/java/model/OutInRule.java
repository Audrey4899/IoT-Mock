package model;

import static java.util.Objects.requireNonNull;

public class OutInRule extends Rule {
    private long timeout;
    private int repeat;
    private long interval;

    public OutInRule(Request request, Response response, Long timeout, Integer repeat, Long interval) {
        super(requireNonNull(request, "Parameter request is required."), response);
        if (!request.getPath().matches("^https?://.*$"))
            throw new IllegalArgumentException(String.format("Wrong path format: '%s'. Must start with http:// or https://", request.getPath()));
        this.timeout = (timeout != null) ? timeout : 0;
        this.repeat = (repeat != null) ? repeat : 1;
        this.interval = (interval != null) ? interval : 1000;
    }

    public long getTimeout() {
        return timeout;
    }

    public int getRepeat() {
        return repeat;
    }

    public long getInterval() {
        return interval;
    }
}
