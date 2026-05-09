package kz.noisemap.gamificationservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import kz.noisemap.gamificationservice.dto.GamificationDto;
import kz.noisemap.gamificationservice.service.GamificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gamification")
@RequiredArgsConstructor
@Tag(name = "Gamification", description = "Motivation system: points, achievements, levels, leaderboard. "
        + "Users earn 10 points per recording plus bonuses for unlocking achievements.")
public class GamificationController {

    private final GamificationService gamificationService;

    @GetMapping("/me")
    @Operation(summary = "My gamification profile",
               description = "Returns points, level, current streak, and list of unlocked achievements. "
                       + "Achievements: First Step (1 recording), Activist (10), Explorer (50), "
                       + "Night Owl (recording 23:00–5:00), Peace & Quiet (<40 dBA), and more.")
    public ResponseEntity<GamificationDto.ProfileResponse> getMyProfile(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(gamificationService.getProfile(userId));
    }

    @GetMapping("/leaderboard")
    @Operation(summary = "Leaderboard",
               description = "Top users ranked by points. Returns rank, points, total recordings, and level.")
    public ResponseEntity<List<GamificationDto.LeaderboardEntry>> getLeaderboard(
            @Parameter(description = "Number of leaderboard entries to return", example = "20")
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(gamificationService.getLeaderboard(limit));
    }
}
