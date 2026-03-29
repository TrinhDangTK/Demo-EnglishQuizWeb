package EnglishQuiz.controller;

import EnglishQuiz.model.Category;
import EnglishQuiz.repository.CategoryRepository;
import EnglishQuiz.repository.LevelRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class CategoryController {
    private final CategoryRepository categoryRepository;
    private final LevelRepository levelRepository;

    public CategoryController(CategoryRepository categoryRepository, LevelRepository levelRepository) {
        this.categoryRepository = categoryRepository;
        this.levelRepository = levelRepository;
    }

    @GetMapping({"/", "/category", "/categories", "/category.html", "/categories.html"})
    public String index(Model model) {
        List<Category> categories = new ArrayList<>();
        categoryRepository.findAll().forEach(cat -> {
            cat.setLevels(levelRepository.findByCategoryId(cat.getId()));
            categories.add(cat);
        });
        model.addAttribute("categories", categories);
        return "categories";
    }

    @GetMapping("/api/categories/search")
    @ResponseBody
    public List<CategorySearchItem> searchCategories(@RequestParam(defaultValue = "") String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        List<Category> categories;
        if (normalizedKeyword.isBlank()) {
            categories = categoryRepository.findAllByOrderByTitleAsc();
        } else {
            categories = categoryRepository.findByTitleContainingIgnoreCaseOrderByTitleAsc(normalizedKeyword);
        }

        return categories.stream()
                .map(cat -> new CategorySearchItem(cat.getId(), cat.getTitle()))
                .collect(Collectors.toList());
    }

    public record CategorySearchItem(Integer id, String title) {}
}
