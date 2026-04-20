package EnglishQuiz.controller;

import EnglishQuiz.model.UserAccount;
import EnglishQuiz.repository.UserAccountRepository;
import EnglishQuiz.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
public class ProfileController {

    private static final int FULL_NAME_MAX = 120;
    private static final int EMAIL_MAX = 255;

    private final UserAccountRepository userAccountRepository;

    public ProfileController(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @GetMapping("/profile")
    public String profilePage(HttpSession session, Model model) {
        UserAccount user = requireCurrentUser(session);
        if (user == null) {
            return "redirect:/login?redirect=" + URLEncoder.encode("/profile", StandardCharsets.UTF_8);
        }
        model.addAttribute("username", user.getUsername());
        model.addAttribute("fullName", user.getFullName() != null ? user.getFullName() : "");
        model.addAttribute("email", user.getEmail() != null ? user.getEmail() : "");
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam(required = false) String fullName,
                                @RequestParam(required = false) String email,
                                @RequestParam(required = false) String currentPassword,
                                @RequestParam(required = false) String newPassword,
                                @RequestParam(required = false) String confirmPassword,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        UserAccount user = requireCurrentUser(session);
        if (user == null) {
            return "redirect:/login?redirect=" + URLEncoder.encode("/profile", StandardCharsets.UTF_8);
        }

        String trimmedFull = fullName == null ? "" : fullName.trim();
        if (trimmedFull.length() > FULL_NAME_MAX) {
            redirectAttributes.addFlashAttribute("error", "Full name must be at most " + FULL_NAME_MAX + " characters.");
            return "redirect:/profile";
        }

        String trimmedEmail = email == null ? "" : email.trim();
        if (trimmedEmail.length() > EMAIL_MAX) {
            redirectAttributes.addFlashAttribute("error", "Email must be at most " + EMAIL_MAX + " characters.");
            return "redirect:/profile";
        }
        if (!trimmedEmail.isEmpty() && !isValidEmail(trimmedEmail)) {
            redirectAttributes.addFlashAttribute("error", "Please enter a valid email address or leave it empty.");
            return "redirect:/profile";
        }

        boolean wantPasswordChange = hasText(newPassword) || hasText(confirmPassword) || hasText(currentPassword);
        if (wantPasswordChange) {
            if (!hasText(currentPassword) || !BCrypt.checkpw(currentPassword, user.getPasswordHash())) {
                redirectAttributes.addFlashAttribute("error", "Current password is incorrect.");
                return "redirect:/profile";
            }
            if (!hasText(newPassword) || newPassword.length() < 6) {
                redirectAttributes.addFlashAttribute("error", "New password must be at least 6 characters.");
                return "redirect:/profile";
            }
            if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "New password and confirmation do not match.");
                return "redirect:/profile";
            }
            user.setPasswordHash(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        }

        user.setFullName(trimmedFull.isEmpty() ? null : trimmedFull);
        user.setEmail(trimmedEmail.isEmpty() ? null : trimmedEmail);
        userAccountRepository.save(user);

        session.setAttribute(AuthController.SESSION_DISPLAY_NAME_KEY, AuthController.resolveDisplayName(user));
        redirectAttributes.addFlashAttribute("success", "Profile updated.");
        return "redirect:/profile";
    }

    // ── Helpers ──────────────────────────────────────────────

    private UserAccount requireCurrentUser(HttpSession session) {
        String username = SessionUtils.getCurrentUsername(session);
        if (username == null) {
            return null;
        }
        return userAccountRepository.findByUsernameIgnoreCase(username.trim()).orElse(null);
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
}
