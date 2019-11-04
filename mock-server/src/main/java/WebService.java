import io.javalin.Javalin;
import model.Component;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

@WebServlet(urlPatterns = {"/*"})
public class WebService extends HttpServlet {
    private Javalin app;
    private List<Component> components;

    public WebService() {
        app = Javalin.createStandalone();

        app.get("/", ctx -> ctx.result("It works !"));
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        app.servlet().service(req, resp);
    }
}
