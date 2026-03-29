package EnglishQuiz.repository;

import EnglishQuiz.model.Question;
import org.springframework.data.repository.CrudRepository;
import java.util.List;

public interface QuestionRepository extends CrudRepository<Question, Integer> {
    List<Question> findByLevelId(int levelId);
}