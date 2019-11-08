import io.javalin.Javalin;
import io.javalin.http.Handler;
import io.javalin.http.HandlerType;
import load.Loader;
import load.LoaderException;
import load.YamlLoader;
import model.Component;
import model.InOutRule;
import model.OutInRule;
import model.Rule;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

@WebServlet(urlPatterns = {"/*"})
public class WebService extends HttpServlet {
    private Javalin app;
    private List<Component> components;

    public WebService() {
        app = Javalin.createStandalone();
        app.exception(LoaderException.class, (exception, ctx) -> {
            ctx.status(400);
            ctx.result(List.of(exception.getClass().toString(), (exception.getMessage() != null) ? exception.getMessage() : "").toString());
        });
        app.exception(Exception.class, (exception, ctx) -> {
            exception.printStackTrace();
            ctx.status(500);
            ctx.result(List.of(exception.getClass().toString(), (exception.getMessage() != null) ? exception.getMessage() : "").toString());
        });

        app.get("/", ctx -> ctx.result("It works !"));
        app.post("/addRules", addRulesHandler());
    }

    private Handler addRulesHandler() {
        return ctx -> {
            Loader loader;
            if(Objects.equals(ctx.header("Content-Type"), "text/yaml")) {
                loader = new YamlLoader();
            } else {
                throw new LoaderException("Wrong content type");
            }
            List<Rule> rules = loader.load(ctx.body());
            initRules(rules);
            ctx.result("Rules added");
        };
    }

    private void initRules(List<Rule> rules) {
        rules.forEach(rule -> {
            if(rule instanceof InOutRule) {
                app.addHandler(HandlerType.valueOf(rule.getRequest().getMethod()), rule.getRequest().getPath(), ctx -> {
                    ctx.status(rule.getResponse().getStatus());
                    ctx.result(rule.getResponse().getBody());
                    rule.getResponse().getHeaders().forEach(ctx::header);
                });
            } else if(rule instanceof OutInRule) {
                new OutputRequest((OutInRule) rule).start();
            }
        });
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        app.servlet().service(req, resp);
    }
}
