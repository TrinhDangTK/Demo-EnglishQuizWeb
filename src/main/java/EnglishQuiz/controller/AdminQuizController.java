package EnglishQuiz.controller;

import EnglishQuiz.model.Answer;
import EnglishQuiz.model.Category;
import EnglishQuiz.model.Level;
import EnglishQuiz.model.Question;
import EnglishQuiz.repository.AnswerRepository;
import EnglishQuiz.repository.CategoryRepository;
import EnglishQuiz.repository.LevelRepository;
import EnglishQuiz.repository.QuestionRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Controller
public class AdminQuizController {
    private final CategoryRepository categoryRepository;
    private final LevelRepository levelRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;

    public AdminQuizController(CategoryRepository categoryRepository,
                               LevelRepository levelRepository,
                               QuestionRepository questionRepository,
                               AnswerRepository answerRepository) {
        this.categoryRepository = categoryRepository;
        this.levelRepository = levelRepository;
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
    }

    @GetMapping("/admin/quizzes")
    public String adminPage(Model model) {
        model.addAttribute("categories", sortById(categoryRepository.findAll()));
        model.addAttribute("levels", sortById(levelRepository.findAll()));
        model.addAttribute("questions", sortById(questionRepository.findAll()));
        model.addAttribute("answers", sortById(answerRepository.findAll()));
        return "admin-quizzes";
    }

    @PostMapping("/admin/categories/create")
    public String createCategory(@RequestParam Integer id,
                                 @RequestParam String title,
                                 RedirectAttributes redirectAttributes) {
        if (categoryRepository.existsById(id)) {
            redirectAttributes.addFlashAttribute("error", "Category id already exists.");
            return "redirect:/admin/quizzes";
        }
        Category category = new Category();
        category.setId(id);
        category.setTitle(title == null ? "" : title.trim());
        categoryRepository.save(category);
        redirectAttributes.addFlashAttribute("success", "Category created.");
        return "redirect:/admin/quizzes";
    }

    @PostMapping("/admin/categories/update")
    public String updateCategory(@RequestParam Integer id,
                                 @RequestParam String title,
                                 RedirectAttributes redirectAttributes) {
        Category category = categoryRepository.findById(id).orElse(null);
        if (category == null) {
            redirectAttributes.addFlashAttribute("error", "Category not found.");
            return "redirect:/admin/quizzes";
        }
        category.setTitle(title == null ? "" : title.trim());
        categoryRepository.save(category);
        redirectAttributes.addFlashAttribute("success", "Category updated.");
        return "redirect:/admin/quizzes";
    }

    @PostMapping("/admin/categories/delete")
    public String deleteCategory(@RequestParam Integer id, RedirectAttributes redirectAttributes) {
        try {
            categoryRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Category deleted.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Cannot delete category. Remove dependent levels first.");
        }
        return "redirect:/admin/quizzes";
    }

    @PostMapping("/admin/levels/create")
    public String createLevel(@RequestParam Integer id,
                              @RequestParam Integer categoryId,
                              @RequestParam String levelName,
                              RedirectAttributes redirectAttributes) {
        if (levelRepository.existsById(id)) {
            redirectAttributes.addFlashAttribute("error", "Level id already exists.");
            return "redirect:/admin/quizzes";
        }
        Level level = new Level();
        level.setId(id);
        level.setCategoryId(categoryId);
        level.setLevelName(levelName == null ? "" : levelName.trim());
        levelRepository.save(level);
        redirectAttributes.addFlashAttribute("success", "Level created.");
        return "redirect:/admin/quizzes";
    }

    @PostMapping("/admin/levels/update")
    public String updateLevel(@RequestParam Integer id,
                              @RequestParam Integer categoryId,
                              @RequestParam String levelName,
                              RedirectAttributes redirectAttributes) {
        Level level = levelRepository.findById(id).orElse(null);
        if (level == null) {
            redirectAttributes.addFlashAttribute("error", "Level not found.");
            return "redirect:/admin/quizzes";
        }
        level.setCategoryId(categoryId);
        level.setLevelName(levelName == null ? "" : levelName.trim());
        levelRepository.save(level);
        redirectAttributes.addFlashAttribute("success", "Level updated.");
        return "redirect:/admin/quizzes";
    }

    @PostMapping("/admin/levels/delete")
    public String deleteLevel(@RequestParam Integer id, RedirectAttributes redirectAttributes) {
        try {
            levelRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Level deleted.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Cannot delete level. Remove dependent questions first.");
        }
        return "redirect:/admin/quizzes";
    }

    @PostMapping("/admin/questions/create")
    public String createQuestion(@RequestParam Integer id,
                                 @RequestParam Integer levelId,
                                 @RequestParam String title,
                                 @RequestParam String type,
                                 @RequestParam(required = false) String explaination,
                                 @RequestParam(required = false) String mediaUrl,
                                 RedirectAttributes redirectAttributes) {
        if (questionRepository.existsById(id)) {
            redirectAttributes.addFlashAttribute("error", "Question id already exists.");
            return "redirect:/admin/quizzes";
        }
        Question question = new Question();
        question.setId(id);
        question.setLevelId(levelId);
        question.setTitle(title == null ? "" : title.trim());
        question.setType("M".equalsIgnoreCase(type) ? "M" : "S");
        question.setExplaination(explaination == null ? null : explaination.trim());
        question.setMediaUrl(mediaUrl == null ? null : mediaUrl.trim());
        questionRepository.save(question);
        redirectAttributes.addFlashAttribute("success", "Question created.");
        return "redirect:/admin/quizzes";
    }

    @PostMapping("/admin/questions/update")
    public String updateQuestion(@RequestParam Integer id,
                                 @RequestParam Integer levelId,
                                 @RequestParam String title,
                                 @RequestParam String type,
                                 @RequestParam(required = false) String explaination,
                                 @RequestParam(required = false) String mediaUrl,
                                 RedirectAttributes redirectAttributes) {
        Question question = questionRepository.findById(id).orElse(null);
        if (question == null) {
            redirectAttributes.addFlashAttribute("error", "Question not found.");
            return "redirect:/admin/quizzes";
        }
        question.setLevelId(levelId);
        question.setTitle(title == null ? "" : title.trim());
        question.setType("M".equalsIgnoreCase(type) ? "M" : "S");
        question.setExplaination(explaination == null ? null : explaination.trim());
        question.setMediaUrl(mediaUrl == null ? null : mediaUrl.trim());
        questionRepository.save(question);
        redirectAttributes.addFlashAttribute("success", "Question updated.");
        return "redirect:/admin/quizzes";
    }

    @PostMapping("/admin/questions/delete")
    public String deleteQuestion(@RequestParam Integer id, RedirectAttributes redirectAttributes) {
        try {
            List<Answer> answers = answerRepository.findByQuestionId(id);
            for (Answer answer : answers) {
                answerRepository.deleteById(answer.getId());
            }
            questionRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Question deleted.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Cannot delete question.");
        }
        return "redirect:/admin/quizzes";
    }

    @PostMapping("/admin/answers/create")
    public String createAnswer(@RequestParam Integer id,
                               @RequestParam Integer questionId,
                               @RequestParam String label,
                               @RequestParam String text,
                               @RequestParam(defaultValue = "false") boolean isCorrect,
                               RedirectAttributes redirectAttributes) {
        if (answerRepository.existsById(id)) {
            redirectAttributes.addFlashAttribute("error", "Answer id already exists.");
            return "redirect:/admin/quizzes";
        }
        Answer answer = new Answer();
        answer.setId(id);
        answer.setQuestionId(questionId);
        answer.setLabel(label == null ? "" : label.trim());
        answer.setText(text == null ? "" : text.trim());
        answer.setCorrect(isCorrect);
        answerRepository.save(answer);
        redirectAttributes.addFlashAttribute("success", "Answer created.");
        return "redirect:/admin/quizzes";
    }

    @PostMapping("/admin/answers/update")
    public String updateAnswer(@RequestParam Integer id,
                               @RequestParam Integer questionId,
                               @RequestParam String label,
                               @RequestParam String text,
                               @RequestParam(defaultValue = "false") boolean isCorrect,
                               RedirectAttributes redirectAttributes) {
        Answer answer = answerRepository.findById(id).orElse(null);
        if (answer == null) {
            redirectAttributes.addFlashAttribute("error", "Answer not found.");
            return "redirect:/admin/quizzes";
        }
        answer.setQuestionId(questionId);
        answer.setLabel(label == null ? "" : label.trim());
        answer.setText(text == null ? "" : text.trim());
        answer.setCorrect(isCorrect);
        answerRepository.save(answer);
        redirectAttributes.addFlashAttribute("success", "Answer updated.");
        return "redirect:/admin/quizzes";
    }

    @PostMapping("/admin/answers/delete")
    public String deleteAnswer(@RequestParam Integer id, RedirectAttributes redirectAttributes) {
        try {
            answerRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Answer deleted.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Cannot delete answer.");
        }
        return "redirect:/admin/quizzes";
    }

    private <T> List<T> sortById(Iterable<T> data) {
        List<T> list = new ArrayList<>();
        data.forEach(list::add);
        list.sort(Comparator.comparingInt(item -> extractId(item)));
        return list;
    }

    private int extractId(Object obj) {
        if (obj instanceof Category c) return c.getId() == null ? 0 : c.getId();
        if (obj instanceof Level l) return l.getId() == null ? 0 : l.getId();
        if (obj instanceof Question q) return q.getId() == null ? 0 : q.getId();
        if (obj instanceof Answer a) return a.getId() == null ? 0 : a.getId();
        return 0;
    }
}
