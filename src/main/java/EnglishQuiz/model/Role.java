package EnglishQuiz.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "roles", uniqueConstraints = {
        @UniqueConstraint(name = "uk_roles_name", columnNames = "name")
})
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 30)
    private String name;
}
