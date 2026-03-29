package EnglishQuiz.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
@Entity
@Data
@Table(name = "question")
public class Question {
    @Id
    private Integer id;
    
    @Column(name = "level_id")
    private Integer levelId;
    
    private String title;
    private String explaination;
    private String type;
    
    @Column(name = "media_url")
    private String mediaUrl;

    @Transient
    private List<Answer> answers;

    public void setAnswers(List<Answer> answers) {
        if (answers == null) {
            this.answers = Collections.emptyList();
        } else {
            this.answers = new ArrayList<>(answers);
        }
    }
}
