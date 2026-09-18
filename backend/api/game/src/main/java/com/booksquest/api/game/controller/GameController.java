package com.booksquest.api.game.controller;

import com.booksquest.api.game.dto.GameSessionDTO;
import com.booksquest.api.game.dto.MoveDTO;
import com.booksquest.api.game.service.GameService;
import com.booksquest.shared.config.AppConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(AppConfig.API_PREFIX + "/games")
@RequiredArgsConstructor
public class GameController {
    private final GameService gameService;

    @PostMapping("/start/{bookId}")
    public ResponseEntity<GameSessionDTO> startGame(@PathVariable Long bookId, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(gameService.createGameSession(userId, bookId));
    }

    @GetMapping("/{gameSessionId}")
    public ResponseEntity<GameSessionDTO> getGameSession(@PathVariable Long gameSessionId, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(gameService.getGameSession(gameSessionId, userId));
    }

    @PostMapping("/{gameSessionId}/move")
    public ResponseEntity<GameSessionDTO> makeMove(
            @PathVariable Long gameSessionId,
            @RequestParam Long bookId,
            @RequestBody MoveDTO moveDTO,
            Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(gameService.makeMove(gameSessionId, userId, moveDTO.getNextSectionId(), bookId));
    }

    @PostMapping("/{gameSessionId}/save")
    public ResponseEntity<Void> saveGame(@PathVariable Long gameSessionId, Authentication authentication) {
        Long userId = (Long) authentication.getPrincipal();
        gameService.saveGame(gameSessionId, userId);
        return ResponseEntity.ok().build();
    }
}
