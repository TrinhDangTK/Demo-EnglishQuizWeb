package EnglishQuiz.repository;

import EnglishQuiz.model.RememberLoginToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RememberLoginTokenRepository extends JpaRepository<RememberLoginToken, Long> {
    Optional<RememberLoginToken> findByTokenAndExpiresAtAfter(String token, LocalDateTime now);

    Optional<RememberLoginToken> findByToken(String token);

    void deleteByUsername(String username);
}
