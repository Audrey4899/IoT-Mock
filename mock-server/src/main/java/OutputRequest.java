import model.OutInRule;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Level;
import java.util.logging.Logger;


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
                outInRule.getRequest().getHeaders().forEach(builder::header);
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

    /**
     * Send an HTTP Request in a new Thread
     * @param request
     */
    private void requestAsync(HttpRequest request) {
        new Thread(() -> {
            try {
                HttpResponse<String> res = client.send(request, HttpResponse.BodyHandlers.ofString());
                LoggerFactory.getLogger("MOCK").info(String.format("Request: %s %s -- %d", request.method(), request.uri(), res.statusCode()));
            } catch (IOException | InterruptedException e) {
                LoggerFactory.getLogger("MOCK").error(String.format("Request: %s %s -- ERROR %s", request.method(), request.uri(), e.getClass().getSimpleName()));
            }
        }).start();
    }
}
