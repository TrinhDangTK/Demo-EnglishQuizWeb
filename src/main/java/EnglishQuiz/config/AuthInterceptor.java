package EnglishQuiz.config;

import EnglishQuiz.controller.AuthController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(AuthController.SESSION_USER_KEY) != null) {
            return true;
        }

        String requestUri = request.getRequestURI();
        String query = request.getQueryString();
        String target = query == null || query.isBlank() ? requestUri : requestUri + "?" + query;
        String encoded = URLEncoder.encode(target, StandardCharsets.UTF_8);
        response.sendRedirect("/login?redirect=" + encoded);
        return false;
    }
}
