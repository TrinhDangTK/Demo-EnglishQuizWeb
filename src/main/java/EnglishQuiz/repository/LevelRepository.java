package EnglishQuiz.repository;

import EnglishQuiz.model.Level;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LevelRepository extends JpaRepository<Level, Integer> {
    List<Level> findByCategoryId(int categoryId);
}