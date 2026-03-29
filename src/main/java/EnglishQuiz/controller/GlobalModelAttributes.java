package EnglishQuiz.controller;

import EnglishQuiz.model.RememberLoginToken;
import EnglishQuiz.model.UserAccount;
import EnglishQuiz.repository.RememberLoginTokenRepository;
import EnglishQuiz.repository.RoleRepository;
import EnglishQuiz.repository.UserAccountRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.LocalDateTime;

@ControllerAdvice
public class GlobalModelAttributes {
    private final RememberLoginTokenRepository rememberLoginTokenRepository;
    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;

    public GlobalModelAttributes(RememberLoginTokenRepository rememberLoginTokenRepository,
                                 UserAccountRepository userAccountRepository,
                                 RoleRepository roleRepository) {
        this.rememberLoginTokenRepository = rememberLoginTokenRepository;
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
    }

    @ModelAttribute
    public void addAuthAttributes(HttpServletRequest request,
                                  HttpServletResponse response,
                                  HttpSession session,
                                  Model model) {
        restoreSessionFromRememberCookie(request, response, session);
        Object currentUsername = session.getAttribute(AuthController.SESSION_USER_KEY);
        Object role = session.getAttribute(AuthController.SESSION_ROLE_KEY);
        model.addAttribute("loggedIn", currentUsername != null);
        model.addAttribute("currentUsername", currentUsername);
        model.addAttribute("isAdmin", AuthController.ROLE_ADMIN.equals(role));
    }

    private void restoreSessionFromRememberCookie(HttpServletRequest request, HttpServletResponse response, HttpSession session) {
        if (session.getAttribute(AuthController.SESSION_USER_KEY) != null) {
            return;
        }
        String token = extractCookieValue(request, AuthController.REMEMBER_ME_COOKIE);
        if (token == null || token.isBlank()) {
            return;
        }
        RememberLoginToken rememberToken = rememberLoginTokenRepository
                .findByTokenAndExpiresAtAfter(token, LocalDateTime.now())
                .orElse(null);
        if (rememberToken == null) {
            clearRememberCookie(response);
            return;
        }
        UserAccount user = userAccountRepository.findByUsernameIgnoreCase(rememberToken.getUsername()).orElse(null);
        if (user == null) {
            rememberLoginTokenRepository.findByToken(token)
                    .ifPresent(rememberLoginTokenRepository::delete);
            clearRememberCookie(response);
            return;
        }
        String roleName = roleRepository.findById(user.getRoleId())
                .map(r -> r.getName() == null ? AuthController.ROLE_USER : r.getName().trim().toUpperCase())
                .orElse(AuthController.ROLE_USER);
        if (!AuthController.ROLE_ADMIN.equals(roleName)) {
            roleName = AuthController.ROLE_USER;
        }
        session.setAttribute(AuthController.SESSION_USER_KEY, user.getUsername());
        session.setAttribute(AuthController.SESSION_ROLE_KEY, roleName);
    }

    private String extractCookieValue(HttpServletRequest request, String name) {
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

    private void clearRememberCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(AuthController.REMEMBER_ME_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
