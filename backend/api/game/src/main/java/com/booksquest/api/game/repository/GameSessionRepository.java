package com.booksquest.api.game.repository;

import com.booksquest.api.game.entity.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository("gameSessionRepository")
public interface GameSessionRepository extends JpaRepository<GameSession, Long> {
    Optional<GameSession> findByIdAndUserId(Long id, Long userId);
    Optional<GameSession> findByIdAndUserIdAndBookId(Long id, Long userId, Long bookId);
    Optional<GameSession> findFirstByUserIdAndBookIdOrderByUpdatedAtDesc(Long userId, Long bookId);
    void deleteByUserIdAndBookIdAndStatus(Long userId, Long bookId, String status);
}
