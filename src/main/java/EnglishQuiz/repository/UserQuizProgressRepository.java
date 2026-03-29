package EnglishQuiz.repository;

import EnglishQuiz.model.UserQuizProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserQuizProgressRepository extends JpaRepository<UserQuizProgress, Long> {
    Optional<UserQuizProgress> findByUsernameAndCategoryIdAndLevelId(String username, Integer categoryId, Integer levelId);
}
