package model;

public class OutInRule extends Rule {
    private long timeout;
    private int repeat;
    private long interval;

    public OutInRule(Request request, Response response) {
        super(request, response);
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
