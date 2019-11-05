package model;

public class OutInRule extends Rule {
    private long timeout;
    private int repeat;
    private long interval;

    public OutInRule(Request request, Response response, long timeout, int repeat, long interval) {
        super(request, response);
        this.timeout = timeout;
        this.repeat = repeat;
        this.interval = interval;
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
