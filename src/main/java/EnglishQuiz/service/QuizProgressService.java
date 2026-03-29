package EnglishQuiz.service;

import EnglishQuiz.dto.QuizSession;
import EnglishQuiz.model.UserQuizProgress;
import EnglishQuiz.repository.UserQuizProgressRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class QuizProgressService {
    private final UserQuizProgressRepository progressRepository;
    private final ObjectMapper objectMapper;

    public QuizProgressService(UserQuizProgressRepository progressRepository, ObjectMapper objectMapper) {
        this.progressRepository = progressRepository;
        this.objectMapper = objectMapper;
    }

    public Optional<QuizSession> load(String username, int categoryId, int levelId) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return progressRepository.findByUsernameAndCategoryIdAndLevelId(username, categoryId, levelId)
                .map(this::toQuizSession);
    }

    public void save(String username, QuizSession quizSession) {
        if (username == null || username.isBlank() || quizSession == null) {
            return;
        }
        UserQuizProgress progress = progressRepository
                .findByUsernameAndCategoryIdAndLevelId(username, quizSession.getCategoryId(), quizSession.getLevelId())
                .orElseGet(UserQuizProgress::new);

        progress.setUsername(username);
        progress.setCategoryId(quizSession.getCategoryId());
        progress.setLevelId(quizSession.getLevelId());
        progress.setCurrentIndex(quizSession.getCurrentIndex());
        progress.setSubmitted(quizSession.isSubmitted());
        progress.setQuestionIdsJson(writeAsJson(quizSession.getQuestionIds()));
        progress.setAnswersJson(writeAsJson(quizSession.getAnswers()));
        progressRepository.save(progress);
    }

    public void delete(String username, int categoryId, int levelId) {
        if (username == null || username.isBlank()) {
            return;
        }
        progressRepository.findByUsernameAndCategoryIdAndLevelId(username, categoryId, levelId)
                .ifPresent(progressRepository::delete);
    }

    private QuizSession toQuizSession(UserQuizProgress progress) {
        QuizSession session = new QuizSession();
        session.setCategoryId(progress.getCategoryId() == null ? 0 : progress.getCategoryId());
        session.setLevelId(progress.getLevelId() == null ? 0 : progress.getLevelId());
        session.setCurrentIndex(progress.getCurrentIndex() == null ? 0 : progress.getCurrentIndex());
        session.setSubmitted(progress.isSubmitted());
        session.setQuestionIds(readQuestionIds(progress.getQuestionIdsJson()));
        session.setAnswers(readAnswers(progress.getAnswersJson()));
        return session;
    }

    private String writeAsJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "[]";
        }
    }

    private List<Integer> readQuestionIds(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Integer>>() {});
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    private Map<Integer, List<Integer>> readAnswers(String json) {
        try {
            Map<String, List<Integer>> raw = objectMapper.readValue(json, new TypeReference<Map<String, List<Integer>>>() {});
            Map<Integer, List<Integer>> result = new HashMap<>();
            for (Map.Entry<String, List<Integer>> entry : raw.entrySet()) {
                result.put(Integer.parseInt(entry.getKey()), entry.getValue());
            }
            return result;
        } catch (Exception ex) {
            return new HashMap<>();
        }
    }
}
