package EnglishQuiz.model;

import jakarta.persistence.Column;
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

    /** If true, only VIP users (tier >= 2) can access this category. */
    @Column(name = "vip_only", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean vipOnly = false;
    
    @Transient
    private List<Level> levels = new ArrayList<>();
}