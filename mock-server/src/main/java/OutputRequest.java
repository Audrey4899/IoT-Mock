import model.OutInRule;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


public class OutputRequest extends Thread {
    private OutInRule outInRule;
    private HttpClient client = HttpClient.newHttpClient();

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
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder()
                        .method(outInRule.getRequest().getMethod(), HttpRequest.BodyPublishers.ofString(outInRule.getRequest().getBody()))
                        .uri(new URI(outInRule.getRequest().getPath()));

                requestAsync(builder.build());
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(outInRule.getInterval());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            i++;
        }
    }

    private void requestAsync(HttpRequest request) {
        new Thread(() -> {
            try {
                client.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
