package EnglishQuiz.controller;

import EnglishQuiz.dto.QuizSession;
import EnglishQuiz.model.Category;
import EnglishQuiz.model.Level;
import EnglishQuiz.model.UserQuizProgress;
import EnglishQuiz.repository.CategoryRepository;
import EnglishQuiz.repository.LevelRepository;
import EnglishQuiz.repository.UserQuizProgressRepository;
import EnglishQuiz.service.QuizProgressService;
import EnglishQuiz.service.QuizService;
import EnglishQuiz.util.SessionUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
public class ProgressController {

    private final UserQuizProgressRepository progressRepository;
    private final CategoryRepository categoryRepository;
    private final LevelRepository levelRepository;
    private final QuizProgressService quizProgressService;
    private final QuizService quizService;

    public ProgressController(UserQuizProgressRepository progressRepository,
                              CategoryRepository categoryRepository,
                              LevelRepository levelRepository,
                              QuizProgressService quizProgressService,
                              QuizService quizService) {
        this.progressRepository = progressRepository;
        this.categoryRepository = categoryRepository;
        this.levelRepository = levelRepository;
        this.quizProgressService = quizProgressService;
        this.quizService = quizService;
    }

    @GetMapping("/progress")
    public String progressPage(HttpSession session, Model model) {
        String username = SessionUtils.getCurrentUsername(session);
        if (username == null) {
            return "redirect:/login?redirect=" + URLEncoder.encode("/progress", StandardCharsets.UTF_8);
        }

        // Index all progress records by "catId_levelId"
        List<UserQuizProgress> allProgress = progressRepository.findByUsername(username);
        Map<String, UserQuizProgress> progressMap = allProgress.stream()
                .collect(Collectors.toMap(
                        p -> p.getCategoryId() + "_" + p.getLevelId(),
                        Function.identity()
                ));

        // Build category progress DTOs
        List<Category> categories = categoryRepository.findAllByOrderByTitleAsc();
        List<CategoryProgressData> catProgressList = new ArrayList<>();

        int totalCompleted = 0;
        int totalInProgress = 0;
        int totalNotStarted = 0;

        for (Category cat : categories) {
            List<Level> levels = levelRepository.findByCategoryId(cat.getId());
            List<LevelProgressData> levelList = new ArrayList<>();
            int catCompleted = 0;
            int catInProgress = 0;

            for (Level level : levels) {
                String key = cat.getId() + "_" + level.getId();
                UserQuizProgress prog = progressMap.get(key);
                LevelProgressData lpd;

                if (prog == null) {
                    lpd = new LevelProgressData(level, "NOT_STARTED",
                            0, 0, null, null, null);
                    totalNotStarted++;

                } else if (prog.isSubmitted()) {
                    Integer score = null;
                    Integer scoreTotal = null;
                    try {
                        QuizSession qs = quizProgressService
                                .load(username, cat.getId(), level.getId())
                                .orElse(null);
                        if (qs != null) {
                            QuizService.RichResult result = quizService.gradeAnswers(qs);
                            score = result.getScore();
                            scoreTotal = result.getTotal();
                        }
                    } catch (Exception ignored) {}
                    lpd = new LevelProgressData(level, "COMPLETED",
                            0, 0, score, scoreTotal, prog.getUpdatedAt());
                    catCompleted++;
                    totalCompleted++;

                } else {
                    int answeredCount = 0;
                    int totalQ = 0;
                    try {
                        QuizSession qs = quizProgressService
                                .load(username, cat.getId(), level.getId())
                                .orElse(null);
                        if (qs != null) {
                            answeredCount = qs.getAnsweredCount();
                            totalQ = qs.getTotalQuestions();
                        }
                    } catch (Exception ignored) {}
                    lpd = new LevelProgressData(level, "IN_PROGRESS",
                            answeredCount, totalQ, null, null, prog.getUpdatedAt());
                    catInProgress++;
                    totalInProgress++;
                }

                levelList.add(lpd);
            }

            int totalLevels = levels.size();
            int completionPct = totalLevels > 0 ? (catCompleted * 100 / totalLevels) : 0;
            catProgressList.add(new CategoryProgressData(
                    cat, totalLevels, catCompleted, catInProgress, completionPct, levelList));
        }

        // Remove categories with no levels
        catProgressList.removeIf(c -> c.totalLevels() == 0);

        model.addAttribute("catProgressList", catProgressList);
        model.addAttribute("totalCompleted", totalCompleted);
        model.addAttribute("totalInProgress", totalInProgress);
        model.addAttribute("totalNotStarted", totalNotStarted);
        model.addAttribute("totalAttempted", totalCompleted + totalInProgress);
        return "progress";
    }

    // ── DTOs ─────────────────────────────────────────────────

    public record LevelProgressData(
            Level level,
            String status,
            int answeredCount,
            int totalQuestions,
            Integer score,
            Integer scoreTotal,
            LocalDateTime updatedAt) {

        /** Percentage of questions answered (used when IN_PROGRESS). */
        public int answeredPct() {
            if (totalQuestions <= 0) return 0;
            return answeredCount * 100 / totalQuestions;
        }

        /** Percentage of correct answers (used when COMPLETED). */
        public int scorePct() {
            if (scoreTotal == null || scoreTotal <= 0) return 0;
            return score * 100 / scoreTotal;
        }
    }

    public record CategoryProgressData(
            Category category,
            int totalLevels,
            int completedCount,
            int inProgressCount,
            int completionPct,
            List<LevelProgressData> levels) {
    }
}
