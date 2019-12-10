import io.javalin.http.Context;
import io.javalin.http.Handler;
import model.InOutRule;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class InOutHandler implements Handler {
    private List<InOutRule> rules = new ArrayList<>();

    public void addRule(InOutRule rule) throws RuleAlreadyExistsException {
        for (InOutRule r : rules) {
            if (r.getRequest().getPath().equals(rule.getRequest().getPath())
                    && r.getRequest().getHeaders().equals(rule.getRequest().getHeaders())
                    && r.getRequest().getBody().equals(rule.getRequest().getBody())
            ) throw new RuleAlreadyExistsException("Rule already exists");
        }
        rules.add(rule);
    }

    public void removeRule(InOutRule rule) {
        rules.remove(rule);
    }

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        for (InOutRule rule : rules) {
            String path = (ctx.queryString() != null) ? String.format("%s?%s", ctx.path(), ctx.queryString()) : rule.getRequest().getPath();
            final boolean[] doesHeadersMatch = {true};
            rule.getRequest().getHeaders().forEach((s, s2) -> {
                if (!Objects.equals(ctx.headerMap().get(s.toLowerCase()), s2)) {
                    doesHeadersMatch[0] = false;
                }
            });
            if (ctx.body().equals(rule.getRequest().getBody())
                    && path.equals(rule.getRequest().getPath())
                    && doesHeadersMatch[0]
            ) {
                ctx.status(rule.getResponse().getStatus());
                ctx.result(rule.getResponse().getBody());
                rule.getResponse().getHeaders().forEach(ctx::header);
                ctx.header("content-length", String.valueOf(rule.getResponse().getBody().length()));
                return;
            }
        }
        ctx.status(400);
        ctx.result("Request not matching expectations.");
    }
}
