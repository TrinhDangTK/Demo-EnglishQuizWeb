package EnglishQuiz.repository;

import EnglishQuiz.model.Category;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CategoryRepository extends CrudRepository<Category, Integer> {
    List<Category> findByTitleContainingIgnoreCaseOrderByTitleAsc(String keyword);

    List<Category> findAllByOrderByTitleAsc();
}