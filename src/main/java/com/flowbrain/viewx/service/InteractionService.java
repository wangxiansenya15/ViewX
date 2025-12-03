package com.flowbrain.viewx.service;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.dao.InteractionMapper;
import com.flowbrain.viewx.pojo.vo.UserActionMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class InteractionService {

    @Autowired
    private InteractionMapper interactionMapper;

    @Autowired
    private RecommendService recommendService;

    @Autowired
    private ActionProducer actionProducer;

    @Transactional
    public Result<String> toggleLike(Long userId, Long videoId) {
        if (interactionMapper.checkLike(userId, videoId) > 0) {
            interactionMapper.deleteLike(userId, videoId);
            interactionMapper.decrementVideoLikeCount(videoId);
            // Async update score via MQ
            actionProducer.sendAction(new UserActionMessage(userId, videoId, "unlike", System.currentTimeMillis()));
            return Result.success("Unliked");
        } else {
            interactionMapper.insertLike(userId, videoId);
            interactionMapper.incrementVideoLikeCount(videoId);
            // Async update score via MQ
            actionProducer.sendAction(new UserActionMessage(userId, videoId, "like", System.currentTimeMillis()));
            return Result.success("Liked");
        }
    }

    @Transactional
    public Result<String> toggleFavorite(Long userId, Long videoId) {
        if (interactionMapper.checkFavorite(userId, videoId) > 0) {
            interactionMapper.deleteFavorite(userId, videoId);
            return Result.success("Unfavorited");
        } else {
            interactionMapper.insertFavorite(userId, videoId);
            // If we tracked favorite count, we would update it here
            recommendService.updateVideoScore(videoId); // Favorites also boost score usually
            return Result.success("Favorited");
        }
    }
    
    public boolean isLiked(Long userId, Long videoId) {
        return interactionMapper.checkLike(userId, videoId) > 0;
    }
    
    public boolean isFavorited(Long userId, Long videoId) {
        return interactionMapper.checkFavorite(userId, videoId) > 0;
    }
}
