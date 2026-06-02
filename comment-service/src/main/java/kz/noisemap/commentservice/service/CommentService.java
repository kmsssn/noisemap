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

    public CommentDto.Response create(UUID userId, String displayName, CommentDto.CreateRequest request) {
        Comment comment = Comment.builder()
                .userId(userId)
                .displayName(displayName)
                .location(new double[]{request.getLongitude(), request.getLatitude()})
                .text(request.getText())
                .noiseClass(request.getNoiseClass())
                .noiseLevelDba(request.getNoiseLevelDba())
                .deleted(false)
                .hidden(false)
                .build();

        comment = commentRepository.save(comment);
        log.info("Comment created: id={}, userId={}, lat={}, lng={}",
                comment.getId(), userId, request.getLatitude(), request.getLongitude());
        return toResponse(comment);
    }


    public Page<CommentDto.Response> getAll(Pageable pageable) {
        return commentRepository.findVisible(pageable)
                .map(this::toResponse);
    }

    public Page<CommentDto.Response> getForModeration(String userRole, Pageable pageable) {
        requireModerator(userRole);
        return commentRepository.findForModeration(pageable)
                .map(this::toResponse);
    }


    public List<CommentDto.Response> getInArea(Double minLat, Double minLng, Double maxLat, Double maxLng) {
        return commentRepository.findInBoundingBox(minLng, minLat, maxLng, maxLat)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }


    public List<CommentDto.Response> getNearby(Double lat, Double lng, Double radius) {
        return commentRepository.findNearPoint(lng, lat, radius)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }


    public Page<CommentDto.Response> getMyComments(UUID userId, Pageable pageable) {
        return commentRepository.findVisibleByUser(userId, pageable)
                .map(this::toResponse);
    }


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


    public CommentDto.Response hide(String commentId, String userRole) {
        requireModerator(userRole);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));
        comment.setHidden(true);
        comment = commentRepository.save(comment);
        log.info("Comment hidden: id={}", commentId);
        return toResponse(comment);
    }

    public CommentDto.Response unhide(String commentId, String userRole) {
        requireModerator(userRole);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));
        comment.setHidden(false);
        comment = commentRepository.save(comment);
        log.info("Comment unhidden: id={}", commentId);
        return toResponse(comment);
    }

    private void requireModerator(String userRole) {
        if (!"MODERATOR".equals(userRole) && !"ADMIN".equals(userRole)) {
            throw new SecurityException("Moderator or admin role required");
        }
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
                .hidden(Boolean.TRUE.equals(c.getHidden()))
                .createdAt(c.getCreatedAt())
                .build();
    }
}
