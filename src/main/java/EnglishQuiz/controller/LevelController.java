package EnglishQuiz.controller;

import EnglishQuiz.model.Category;
import EnglishQuiz.model.Level;
import EnglishQuiz.repository.CategoryRepository;
import EnglishQuiz.repository.LevelRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Collections;
import java.util.List;

@Controller
public class LevelController {
    private final LevelRepository levelRepo;
    private final CategoryRepository categoryRepo;

    public LevelController(LevelRepository levelRepo, CategoryRepository categoryRepo) {
        this.levelRepo = levelRepo;
        this.categoryRepo = categoryRepo;
    }

    @GetMapping({"/category/{id}", "/categories/{id}", "/levels/{id}"})
    public String levels(@PathVariable int id, HttpSession session, Model model,
                         RedirectAttributes redirectAttributes) {
        Category category = categoryRepo.findById(id).orElse(null);
        if (category == null) {
            return "redirect:/";
        }

        // Block Normal users from VIP-only categories
        if (Boolean.TRUE.equals(category.getVipOnly())) {
            Object tierObj = session.getAttribute(AuthController.SESSION_TIER_KEY);
            int tier = (tierObj instanceof Integer t) ? t : AuthController.TIER_NORMAL;
            if (tier < AuthController.TIER_VIP) {
                redirectAttributes.addFlashAttribute("error",
                        "🔒 The \"" + category.getTitle() + "\" category requires a VIP account. Please upgrade to access it.");
                return "redirect:/upgrade";
            }
        }

        List<Level> levels = levelRepo.findByCategoryId(id);
        model.addAttribute("categoryId", id);
        model.addAttribute("category", category);
        model.addAttribute("levels", levels == null ? Collections.emptyList() : levels);
        return "levels";
    }
}