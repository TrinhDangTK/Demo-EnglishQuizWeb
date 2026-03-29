package EnglishQuiz.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
@Entity
@Data
@Table(name = "answer")
public class Answer {
    @Id
    private Integer id;
    
    @Column(name = "question_id")
    private Integer questionId;
    
    private String label;
    private String text;
    
    @Column(name = "is_correct")
    private boolean isCorrect;
}