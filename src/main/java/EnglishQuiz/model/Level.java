package EnglishQuiz.model;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
@Entity
@Data
@Table(name = "level")
public class Level {
    @Id
    private Integer id;
    
    @Column(name = "category_id")
    private Integer categoryId;
    
    @Column(name = "level_name")
    private String levelName;
}