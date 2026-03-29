package EnglishQuiz.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "user_account", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_account_username", columnNames = "username")
})
public class UserAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "role_id")
    private Integer roleId;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
