package EnglishQuiz.dto;

import java.util.*;

public class QuizSession {
    private int categoryId;
    private int levelId;
    private List<Integer> questionIds;
    private int currentIndex;
    private Map<Integer, List<Integer>> answers;
    private boolean submitted;

    public QuizSession() {
        this.questionIds = new ArrayList<>();
        this.answers = new HashMap<>();
        this.currentIndex = 0;
    }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public int getLevelId() { return levelId; }
    public void setLevelId(int levelId) { this.levelId = levelId; }

    public List<Integer> getQuestionIds() { return questionIds; }
    public void setQuestionIds(List<Integer> questionIds) { this.questionIds = questionIds; }

    public int getCurrentIndex() { return currentIndex; }
    public void setCurrentIndex(int currentIndex) { this.currentIndex = currentIndex; }

    public Map<Integer, List<Integer>> getAnswers() { return answers; }
    public void setAnswers(Map<Integer, List<Integer>> answers) { this.answers = answers; }

    public boolean isSubmitted() { return submitted; }
    public void setSubmitted(boolean submitted) { this.submitted = submitted; }

    public int getTotalQuestions() { return questionIds.size(); }
    
    public int getAnsweredCount() {
        return (int) answers.values().stream()
            .filter(list -> list != null && !list.isEmpty())
            .count();
    }

    public boolean isAnswered(int questionId) {
        List<Integer> ans = answers.get(questionId);
        return ans != null && !ans.isEmpty();
    }
}
