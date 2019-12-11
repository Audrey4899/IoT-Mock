import model.OutInRule;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;


public class OutputRequest extends Thread {
    private OutInRule outInRule;
    private OkHttpClient client = new OkHttpClient();

    public OutputRequest(OutInRule outInRule) {
        this.outInRule = outInRule;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(outInRule.getTimeout());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        int i = 0;
        boolean infinite = (outInRule.getRepeat() == 0);
        while ((i < outInRule.getRepeat() || infinite)) {
            Request.Builder builder = new Request.Builder()
                    .url(outInRule.getRequest().getPath());
            if(outInRule.getRequest().getMethod().equals("GET")) {
                builder.method(outInRule.getRequest().getMethod(), null);
            } else {
                builder.method(outInRule.getRequest().getMethod(), RequestBody.create(outInRule.getRequest().getBody(), null));
            }
            outInRule.getRequest().getHeaders().forEach(builder::addHeader);

            requestAsync(builder.build());

            try {
                Thread.sleep(outInRule.getInterval());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            i++;
        }
    }

    /**
     * Send an HTTP Request in a new Thread
     *
     * @param request
     */
    private void requestAsync(Request request) {
        new Thread(() -> {
            try (Response res = client.newCall(request).execute()) {
                String result = "Can't check response: not defined in rule";
                if(outInRule.getResponse() != null) {
                    boolean match = true;
                    if(outInRule.getResponse().getStatus() != res.code()) match = false;
                    final boolean[] doesHeadersMatch = {true};
                    outInRule.getResponse().getHeaders().forEach((s, s2) -> {
                        if(!s2.equals(res.header(s))) doesHeadersMatch[0] = false;
                    });
                    match = (doesHeadersMatch[0]) && match;
                    if(!Objects.equals(outInRule.getResponse().getBody(), res.body().string())) match = false;
                    result = match ? "Response match rule": "Response doesn't match rule";
                }
                LoggerFactory.getLogger("MOCK").info(String.format("Request: %s %s -- %d (%s)", request.method(), request.url(), res.code(), result));
            } catch (IOException e) {
                LoggerFactory.getLogger("MOCK").error(String.format("Request: %s %s -- ERROR %s", request.method(), request.url(), e.getClass().getSimpleName()));
            }
        }).start();
    }
}
