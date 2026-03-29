package EnglishQuiz.controller;

import EnglishQuiz.repository.LevelRepository;
import EnglishQuiz.model.Level;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Collections;
import java.util.List;

@Controller
public class LevelController {
    private final LevelRepository levelRepo;

    public LevelController(LevelRepository levelRepo) {
        this.levelRepo = levelRepo;
    }

    @GetMapping({"/category/{id}", "/categories/{id}", "/levels/{id}"})
    public String levels(@PathVariable int id, Model model) {
        List<Level> levels = levelRepo.findByCategoryId(id);
        model.addAttribute("categoryId", id);
        model.addAttribute("levels", levels == null ? Collections.emptyList() : levels);
        return "levels";
    }
}