package EnglishQuiz.controller;

import EnglishQuiz.dto.QuizSession;
import EnglishQuiz.model.Level;
import EnglishQuiz.model.Question;
import EnglishQuiz.repository.LevelRepository;
import EnglishQuiz.service.QuizProgressService;
import EnglishQuiz.service.QuizService;
import EnglishQuiz.service.QuizService.RichResult;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class QuizController {
    private static final String ACTIVE_QUIZ_SESSION_KEY = "quizSession";

    private final QuizService quizService;
    private final LevelRepository levelRepository;
    private final QuizProgressService quizProgressService;

    public QuizController(QuizService quizService,
                          LevelRepository levelRepository,
                          QuizProgressService quizProgressService) {
        this.quizService = quizService;
        this.levelRepository = levelRepository;
        this.quizProgressService = quizProgressService;
    }

    @GetMapping("/quiz/{levelId}")
    public String startQuiz(@PathVariable int levelId, HttpSession session, Model model) {
        Level level = levelRepository.findById(levelId).orElse(null);
        if (level == null || level.getCategoryId() == null) {
            return "redirect:/";
        }
        return startQuiz(level.getCategoryId(), levelId, session, model);
    }

    @GetMapping("/quiz/{categoryId}/{levelId}")
    public String startQuiz(@PathVariable int categoryId, @PathVariable int levelId, HttpSession session, Model model) {
        Level level = levelRepository.findById(levelId).orElse(null);
        if (level == null || level.getCategoryId() == null) {
            return "redirect:/";
        }
        int resolvedCategoryId = level.getCategoryId();
        if (categoryId != resolvedCategoryId) {
            return "redirect:/quiz/" + resolvedCategoryId + "/" + levelId;
        }

        String username = getCurrentUsername(session);
        if (username != null) {
            QuizSession savedSession = quizProgressService.load(username, resolvedCategoryId, levelId).orElse(null);
            if (savedSession != null && !savedSession.getQuestionIds().isEmpty()) {
                session.setAttribute(ACTIVE_QUIZ_SESSION_KEY, savedSession);
                return savedSession.isSubmitted() ? "redirect:/quiz/result" : "redirect:/quiz/question";
            }
        }

        List<Question> questions = quizService.getQuestions(levelId);
        
        if (questions == null || questions.isEmpty()) {
            model.addAttribute("error", "No questions found for this level");
            return "redirect:/levels/" + resolvedCategoryId;
        }
        
        QuizSession quizSession = new QuizSession();
        quizSession.setCategoryId(resolvedCategoryId);
        quizSession.setLevelId(levelId);
        quizSession.setQuestionIds(questions.stream()
            .map(Question::getId)
            .collect(Collectors.toList()));
        quizSession.setCurrentIndex(0);
        
        session.setAttribute(ACTIVE_QUIZ_SESSION_KEY, quizSession);
        persistProgress(session, quizSession);
        return "redirect:/quiz/question";
    }

    @GetMapping("/quiz/question")
    public String showQuestion(HttpSession session, Model model) {
        QuizSession quizSession = (QuizSession) session.getAttribute(ACTIVE_QUIZ_SESSION_KEY);
        if (quizSession == null || quizSession.getQuestionIds().isEmpty()) {
            return "redirect:/";
        }
        if (quizSession.isSubmitted()) {
            return "redirect:/quiz/result";
        }
        if (quizSession.getCategoryId() <= 0) {
            Level level = levelRepository.findById(quizSession.getLevelId()).orElse(null);
            if (level != null && level.getCategoryId() != null) {
                quizSession.setCategoryId(level.getCategoryId());
            }
        }

        int currentIndex = quizSession.getCurrentIndex();
        int questionId = quizSession.getQuestionIds().get(currentIndex);
        Question question = quizService.getQuestionById(questionId);

        if (question == null) {
            return "redirect:/";
        }

        List<Integer> selectedAnswers = quizSession.getAnswers()
            .getOrDefault(questionId, new ArrayList<>());

        model.addAttribute("question", question);
        model.addAttribute("currentIndex", currentIndex);
        model.addAttribute("totalQuestions", quizSession.getTotalQuestions());
        model.addAttribute("selectedAnswers", selectedAnswers);
        model.addAttribute("answeredCount", quizSession.getAnsweredCount());
        model.addAttribute("quizSession", quizSession);
        model.addAttribute("isLastQuestion", currentIndex == quizSession.getTotalQuestions() - 1);
        
        return "quiz";
    }

    @PostMapping("/quiz/answer")
    public String saveAnswer(@RequestParam(required = false) List<Integer> answerIds,
                           @RequestParam String action,
                           HttpSession session) {
        QuizSession quizSession = (QuizSession) session.getAttribute(ACTIVE_QUIZ_SESSION_KEY);
        if (quizSession == null) {
            return "redirect:/";
        }
        if (quizSession.isSubmitted()) {
            return "redirect:/quiz/result";
        }

        int currentIndex = quizSession.getCurrentIndex();
        int questionId = quizSession.getQuestionIds().get(currentIndex);
        
        if (answerIds != null && !answerIds.isEmpty()) {
            quizSession.getAnswers().put(questionId, new ArrayList<>(answerIds));
        } else {
            quizSession.getAnswers().put(questionId, new ArrayList<>());
        }

        if ("next".equals(action) && currentIndex < quizSession.getTotalQuestions() - 1) {
            quizSession.setCurrentIndex(currentIndex + 1);
            persistProgress(session, quizSession);
            return "redirect:/quiz/question";
        } else if ("previous".equals(action) && currentIndex > 0) {
            quizSession.setCurrentIndex(currentIndex - 1);
            persistProgress(session, quizSession);
            return "redirect:/quiz/question";
        } else if ("submit".equals(action)) {
            quizSession.setSubmitted(true);
            persistProgress(session, quizSession);
            return "redirect:/quiz/result";
        } else if (action.startsWith("goto-")) {
            try {
                int targetIndex = Integer.parseInt(action.substring(5));
                if (targetIndex >= 0 && targetIndex < quizSession.getTotalQuestions()) {
                    quizSession.setCurrentIndex(targetIndex);
                }
            } catch (NumberFormatException ignored) {}
            persistProgress(session, quizSession);
            return "redirect:/quiz/question";
        }

        persistProgress(session, quizSession);
        return "redirect:/quiz/question";
    }

    @GetMapping("/quiz/result")
    public String showResult(HttpSession session, Model model) {
        QuizSession quizSession = (QuizSession) session.getAttribute(ACTIVE_QUIZ_SESSION_KEY);
        if (quizSession == null) {
            return "redirect:/";
        }
        for (Integer questionId : quizSession.getQuestionIds()) {
            quizSession.getAnswers().putIfAbsent(questionId, new ArrayList<>());
        }
        quizSession.setSubmitted(true);
        persistProgress(session, quizSession);

        RichResult result = quizService.gradeAnswers(quizSession);
        model.addAttribute("result", result);
        model.addAttribute("score", result.getScore());
        model.addAttribute("total", result.getTotal());
        model.addAttribute("categoryId", quizSession.getCategoryId());
        model.addAttribute("levelId", quizSession.getLevelId());
        return "result";
    }

    @PostMapping("/quiz/reset")
    public String resetQuiz(@RequestParam(required = false) Integer categoryId,
                            @RequestParam(required = false) Integer levelId,
                            HttpSession session) {
        if (categoryId == null || levelId == null || categoryId <= 0 || levelId <= 0) {
            session.removeAttribute(ACTIVE_QUIZ_SESSION_KEY);
            return "redirect:/";
        }
        String username = getCurrentUsername(session);
        if (username != null) {
            quizProgressService.delete(username, categoryId, levelId);
        }

        QuizSession activeSession = (QuizSession) session.getAttribute(ACTIVE_QUIZ_SESSION_KEY);
        if (activeSession != null && activeSession.getCategoryId() == categoryId && activeSession.getLevelId() == levelId) {
            session.removeAttribute(ACTIVE_QUIZ_SESSION_KEY);
        }
        return "redirect:/quiz/" + categoryId + "/" + levelId;
    }

    private void persistProgress(HttpSession session, QuizSession quizSession) {
        String username = getCurrentUsername(session);
        if (username != null) {
            quizProgressService.save(username, quizSession);
        }
    }

    private String getCurrentUsername(HttpSession session) {
        Object value = session.getAttribute(AuthController.SESSION_USER_KEY);
        if (value instanceof String username && !username.isBlank()) {
            return username;
        }
        return null;
    }
}
