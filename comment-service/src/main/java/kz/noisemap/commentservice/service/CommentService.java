package kz.noisemap.commentservice.service;

import kz.noisemap.commentservice.dto.CommentDto;
import kz.noisemap.commentservice.model.Comment;
import kz.noisemap.commentservice.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;

    /**
     * Создать комментарий на карте.
     */
    public CommentDto.Response create(UUID userId, String displayName, CommentDto.CreateRequest request) {
        Comment comment = Comment.builder()
                .userId(userId)
                .displayName(displayName)
                .location(new double[]{request.getLongitude(), request.getLatitude()})
                .text(request.getText())
                .noiseClass(request.getNoiseClass())
                .noiseLevelDba(request.getNoiseLevelDba())
                .deleted(false)
                .build();

        comment = commentRepository.save(comment);
        log.info("Comment created: id={}, userId={}, lat={}, lng={}",
                comment.getId(), userId, request.getLatitude(), request.getLongitude());
        return toResponse(comment);
    }

    /**
     * Все комментарии (пагинация) — для модераторов/публичный.
     */
    public Page<CommentDto.Response> getAll(Pageable pageable) {
        return commentRepository.findByDeletedFalseOrderByCreatedAtDesc(pageable)
                .map(this::toResponse);
    }

    /**
     * Комментарии в области карты (bounding box).
     */
    public List<CommentDto.Response> getInArea(Double minLat, Double minLng, Double maxLat, Double maxLng) {
        return commentRepository.findInBoundingBox(minLng, minLat, maxLng, maxLat)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Комментарии рядом с точкой.
     */
    public List<CommentDto.Response> getNearby(Double lat, Double lng, Double radius) {
        return commentRepository.findNearPoint(lng, lat, radius)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Мои комментарии.
     */
    public Page<CommentDto.Response> getMyComments(UUID userId, Pageable pageable) {
        return commentRepository.findByUserIdAndDeletedFalseOrderByCreatedAtDesc(userId, pageable)
                .map(this::toResponse);
    }

    /**
     * Удалить комментарий — мягкое удаление.
     * Пользователь может удалить только свой. Модератор/Admin — любой.
     */
    public void delete(String commentId, UUID userId, String userRole) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));

        boolean isOwner = comment.getUserId().equals(userId);
        boolean isModerator = "MODERATOR".equals(userRole) || "ADMIN".equals(userRole);

        if (!isOwner && !isModerator) {
            throw new SecurityException("You can only delete your own comments");
        }

        comment.setDeleted(true);
        commentRepository.save(comment);
        log.info("Comment deleted: id={}, by userId={}", commentId, userId);
    }

    private CommentDto.Response toResponse(Comment c) {
        return CommentDto.Response.builder()
                .id(c.getId())
                .userId(c.getUserId())
                .displayName(c.getDisplayName())
                .latitude(c.getLocation()[1])
                .longitude(c.getLocation()[0])
                .text(c.getText())
                .noiseClass(c.getNoiseClass())
                .noiseLevelDba(c.getNoiseLevelDba())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
