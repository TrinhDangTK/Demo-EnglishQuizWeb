package EnglishQuiz.service;

import EnglishQuiz.dto.QuizSession;
import EnglishQuiz.model.Answer;
import EnglishQuiz.model.Question;
import EnglishQuiz.repository.AnswerRepository;
import EnglishQuiz.repository.QuestionRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class QuizService {

    private final QuestionRepository questionRepo;
    private final AnswerRepository answerRepo;

    public QuizService(QuestionRepository questionRepo, AnswerRepository answerRepo) {
        this.questionRepo = questionRepo;
        this.answerRepo = answerRepo;
    }

    public List<Question> getQuestions(int levelId) {
        List<Question> questions = questionRepo.findByLevelId(levelId);
        for (Question q : questions) {
            q.setAnswers(answerRepo.findByQuestionId(q.getId()));
        }
        return questions;
    }

    public Question getQuestionById(int questionId) {
        Optional<Question> opt = questionRepo.findById(questionId);
        if (opt.isPresent()) {
            Question q = opt.get();
            q.setAnswers(answerRepo.findByQuestionId(q.getId()));
            return q;
        }
        return null;
    }

    public RichResult gradeAnswers(QuizSession quizSession) {
        List<Question> questions = new ArrayList<>();
        for (Integer qId : quizSession.getQuestionIds()) {
            Question q = getQuestionById(qId);
            if (q != null) {
                questions.add(q);
            }
        }

        int score = 0;
        int total = questions.size();
        List<ResultItem> items = new ArrayList<>();

        for (Question q : questions) {
            int qId = q.getId();
            List<Integer> chosenIds = quizSession.getAnswers()
                .getOrDefault(qId, Collections.emptyList());
            List<Answer> answers = q.getAnswers() == null ? 
                Collections.emptyList() : q.getAnswers();

            Set<Integer> correctSet = answers.stream()
                    .filter(Answer::isCorrect)
                    .map(Answer::getId)
                    .collect(Collectors.toSet());

            Set<Integer> chosenSet = new HashSet<>(chosenIds);
            boolean correct = !correctSet.isEmpty() && correctSet.equals(chosenSet);
            if (correct) score++;

            items.add(new ResultItem(q, chosenIds, correct));
        }

        return new RichResult(score, total, items);
    }

    public static class ResultItem {
        private final Question question;
        private final List<Integer> chosenIds;
        private final boolean correct;

        public ResultItem(Question question, List<Integer> chosenIds, boolean correct) {
            this.question = question;
            this.chosenIds = chosenIds;
            this.correct = correct;
        }

        public Question getQuestion() { return question; }
        public List<Integer> getChosenIds() { return chosenIds; }
        public boolean isCorrect() { return correct; }
    }

    public static class RichResult {
        private final int score;
        private final int total;
        private final List<ResultItem> items;

        public RichResult(int score, int total, List<ResultItem> items) {
            this.score = score;
            this.total = total;
            this.items = items;
        }

        public int getScore() { return score; }
        public int getTotal() { return total; }
        public List<ResultItem> getItems() { return items; }
    }
}