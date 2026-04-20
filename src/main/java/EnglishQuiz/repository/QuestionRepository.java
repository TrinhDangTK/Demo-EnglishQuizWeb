package EnglishQuiz.repository;

import EnglishQuiz.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Integer> {
    List<Question> findByLevelId(int levelId);
}