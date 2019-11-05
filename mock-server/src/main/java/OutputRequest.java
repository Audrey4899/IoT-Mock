import model.OutInRule;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.net.http.HttpClient;


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
        for (int i = 0; i < outInRule.getRepeat(); i++) {
            RequestBody body = (outInRule.getRequest().getMethod().equals("GET")) ? null : RequestBody.create(outInRule.getRequest().getBody(), null);
            Request r = new Request.Builder()
                    .method(outInRule.getRequest().getMethod(), body)
                    .url(outInRule.getRequest().getPath()).build();
            requestAsync(r);
            try {
                Thread.sleep(outInRule.getInterval());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void requestAsync(Request request) {
        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {
                response.body();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
