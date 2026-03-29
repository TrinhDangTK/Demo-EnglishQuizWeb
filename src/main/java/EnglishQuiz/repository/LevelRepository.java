package EnglishQuiz.repository;

import EnglishQuiz.model.Level;
import org.springframework.data.repository.CrudRepository;
import java.util.List;

public interface LevelRepository extends CrudRepository<Level, Integer> {
    List<Level> findByCategoryId(int categoryId);
}