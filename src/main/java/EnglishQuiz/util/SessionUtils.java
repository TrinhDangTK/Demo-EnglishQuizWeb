package EnglishQuiz.util;

import EnglishQuiz.controller.AuthController;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Shared helper methods for session and cookie handling.
 * Eliminates duplication across controllers and advice classes.
 */
public final class SessionUtils {

    private SessionUtils() { /* utility class */ }

    /**
     * Returns the currently logged-in username, or {@code null} if not authenticated.
     */
    public static String getCurrentUsername(HttpSession session) {
        Object value = session.getAttribute(AuthController.SESSION_USER_KEY);
        if (value instanceof String username && !username.isBlank()) {
            return username;
        }
        return null;
    }

    /**
     * Extracts a named cookie value from the request.
     *
     * @return the cookie value, or {@code null} if not found
     */
    public static String extractCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
     * Clears a "remember me" cookie by setting its max-age to 0.
     */
    public static void clearRememberCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(AuthController.REMEMBER_ME_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
