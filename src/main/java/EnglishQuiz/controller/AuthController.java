package EnglishQuiz.controller;

import EnglishQuiz.model.RememberLoginToken;
import EnglishQuiz.model.Role;
import EnglishQuiz.model.UserAccount;
import EnglishQuiz.repository.RememberLoginTokenRepository;
import EnglishQuiz.repository.RoleRepository;
import EnglishQuiz.repository.UserAccountRepository;
import EnglishQuiz.util.SessionUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@Controller
public class AuthController {

    // ── Session attribute keys ───────────────────────────────
    public static final String SESSION_USER_KEY = "currentUsername";
    public static final String SESSION_DISPLAY_NAME_KEY = "currentDisplayName";
    public static final String SESSION_ROLE_KEY = "currentUserRole";
    public static final String SESSION_TIER_KEY = "currentUserTier";

    // ── Tier constants ───────────────────────────────────────
    public static final int TIER_NORMAL = 1;
    public static final int TIER_VIP = 2;

    // ── Role constants ───────────────────────────────────────
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";

    // ── Remember-me ──────────────────────────────────────────
    public static final String REMEMBER_ME_COOKIE = "remember_login_token";
    private static final int REMEMBER_ME_DAYS = 30;

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final RememberLoginTokenRepository rememberLoginTokenRepository;

    public AuthController(UserAccountRepository userAccountRepository,
                          RoleRepository roleRepository,
                          RememberLoginTokenRepository rememberLoginTokenRepository) {
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.rememberLoginTokenRepository = rememberLoginTokenRepository;
    }

    // ── Login ────────────────────────────────────────────────

    @GetMapping("/login")
    public String loginPage(HttpSession session) {
        if (SessionUtils.getCurrentUsername(session) != null) {
            return "redirect:/";
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        @RequestParam(required = false, defaultValue = "false") boolean rememberMe,
                        @RequestParam(required = false) String redirect,
                        HttpServletRequest request,
                        HttpServletResponse response,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        String normalizedUsername = username == null ? "" : username.trim();
        if (normalizedUsername.isBlank() || password == null || password.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Username and password are required.");
            return buildLoginRedirect(redirect);
        }

        UserAccount user = userAccountRepository.findByUsernameIgnoreCase(normalizedUsername).orElse(null);
        if (user == null || !BCrypt.checkpw(password, user.getPasswordHash())) {
            redirectAttributes.addFlashAttribute("error", "Invalid username or password.");
            return buildLoginRedirect(redirect);
        }

        populateSession(session, user);
        handleRememberMe(user.getUsername(), rememberMe, request, response);
        redirectAttributes.addFlashAttribute("success", "Login successful.");

        if (isSafeLocalRedirect(redirect)) {
            return "redirect:" + redirect;
        }
        return "redirect:/";
    }

    // ── Register ─────────────────────────────────────────────

    @GetMapping("/register")
    public String registerPage(HttpSession session) {
        if (SessionUtils.getCurrentUsername(session) != null) {
            return "redirect:/";
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam("confirmPassword") String confirmPassword,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        String normalizedUsername = username == null ? "" : username.trim();
        if (!isValidUsername(normalizedUsername)) {
            redirectAttributes.addFlashAttribute("error", "Username must be 3-30 chars and only letters, numbers, underscore.");
            return "redirect:/register";
        }
        if (password == null || password.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Password must be at least 6 characters.");
            return "redirect:/register";
        }
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Confirm password does not match.");
            return "redirect:/register";
        }
        if (userAccountRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
            redirectAttributes.addFlashAttribute("error", "Username already exists.");
            return "redirect:/register";
        }

        UserAccount account = new UserAccount();
        account.setUsername(normalizedUsername);
        account.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
        account.setRoleId(resolveDefaultUserRoleId());
        userAccountRepository.save(account);

        populateSession(session, account);
        redirectAttributes.addFlashAttribute("success", "Account created and logged in.");
        return "redirect:/";
    }

    // ── Logout ───────────────────────────────────────────────

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response, HttpSession session) {
        String token = SessionUtils.extractCookieValue(request, REMEMBER_ME_COOKIE);
        if (token != null && !token.isBlank()) {
            rememberLoginTokenRepository.findByToken(token)
                    .ifPresent(rememberLoginTokenRepository::delete);
        }
        SessionUtils.clearRememberCookie(response);
        session.invalidate();
        return "redirect:/login";
    }

    // ── Helpers ──────────────────────────────────────────────

    /** Populates session attributes from a user entity. */
    private void populateSession(HttpSession session, UserAccount user) {
        session.setAttribute(SESSION_USER_KEY, user.getUsername());
        session.setAttribute(SESSION_DISPLAY_NAME_KEY, resolveDisplayName(user));
        session.setAttribute(SESSION_ROLE_KEY, resolveRoleName(user.getRoleId()));
        session.setAttribute(SESSION_TIER_KEY, user.getTier() != null ? user.getTier() : TIER_NORMAL);
    }

    private boolean isValidUsername(String username) {
        return username != null && username.matches("^[A-Za-z0-9_]{3,30}$");
    }

    private boolean isSafeLocalRedirect(String redirect) {
        return redirect != null && redirect.startsWith("/") && !redirect.startsWith("//");
    }

    private String buildLoginRedirect(String redirect) {
        if (isSafeLocalRedirect(redirect)) {
            String encoded = URLEncoder.encode(redirect, StandardCharsets.UTF_8);
            return "redirect:/login?redirect=" + encoded;
        }
        return "redirect:/login";
    }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return ROLE_USER;
        }
        String normalized = role.trim().toUpperCase();
        return ROLE_ADMIN.equals(normalized) ? ROLE_ADMIN : ROLE_USER;
    }

    private Integer resolveDefaultUserRoleId() {
        return roleRepository.findByNameIgnoreCase(ROLE_USER)
                .map(Role::getId)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setName(ROLE_USER);
                    return roleRepository.save(role).getId();
                });
    }

    String resolveRoleName(Integer roleId) {
        if (roleId == null) {
            return ROLE_USER;
        }
        return roleRepository.findById(roleId)
                .map(r -> normalizeRole(r.getName()))
                .orElse(ROLE_USER);
    }

    private void handleRememberMe(String username, boolean rememberMe,
                                  HttpServletRequest request, HttpServletResponse response) {
        String oldToken = SessionUtils.extractCookieValue(request, REMEMBER_ME_COOKIE);
        if (oldToken != null && !oldToken.isBlank()) {
            rememberLoginTokenRepository.findByToken(oldToken)
                    .ifPresent(rememberLoginTokenRepository::delete);
        }
        if (!rememberMe) {
            SessionUtils.clearRememberCookie(response);
            return;
        }

        String token = UUID.randomUUID() + "-" + UUID.randomUUID();
        RememberLoginToken rememberToken = new RememberLoginToken();
        rememberToken.setUsername(username);
        rememberToken.setToken(token);
        rememberToken.setExpiresAt(LocalDateTime.now().plusDays(REMEMBER_ME_DAYS));
        rememberLoginTokenRepository.save(rememberToken);

        Cookie cookie = new Cookie(REMEMBER_ME_COOKIE, token);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(REMEMBER_ME_DAYS * 24 * 60 * 60);
        response.addCookie(cookie);
    }

    /** Label shown in the navbar: full name if set, otherwise username. */
    public static String resolveDisplayName(UserAccount user) {
        if (user == null) {
            return "";
        }
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName().trim();
        }
        return user.getUsername();
    }
}
