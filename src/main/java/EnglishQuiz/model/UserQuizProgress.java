package EnglishQuiz.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "user_quiz_progress", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_quiz_progress_scope", columnNames = {"username", "category_id", "level_id"})
})
public class UserQuizProgress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    @Column(name = "level_id", nullable = false)
    private Integer levelId;

    @Column(name = "question_ids_json", columnDefinition = "longtext")
    private String questionIdsJson;

    @Column(name = "answers_json", columnDefinition = "longtext")
    private String answersJson;

    @Column(name = "current_index", nullable = false)
    private Integer currentIndex = 0;

    @Column(nullable = false)
    private boolean submitted = false;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
