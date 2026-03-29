package EnglishQuiz.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Table(name = "category")
public class Category {
    @Id
    private Integer id;
    private String title;
    
    @Transient
    private List<Level> levels = new ArrayList<>();
}