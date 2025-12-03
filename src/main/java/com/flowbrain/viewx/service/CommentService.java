package com.flowbrain.viewx.service;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.dao.CommentMapper;
import com.flowbrain.viewx.pojo.entity.VideoComment;
import com.flowbrain.viewx.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private RecommendService recommendService;

    @Transactional
    public Result<VideoComment> addComment(Long userId, Long videoId, String content, Long parentId) {
        VideoComment comment = new VideoComment();
        comment.setId(IdGenerator.nextId());
        comment.setUserId(userId);
        comment.setVideoId(videoId);
        comment.setContent(content);
        comment.setParentId(parentId);

        commentMapper.insertComment(comment);
        commentMapper.incrementVideoCommentCount(videoId);
        
        recommendService.updateVideoScore(videoId);

        return Result.success("Comment added", comment);
    }

    public Result<List<VideoComment>> getComments(Long videoId) {
        List<VideoComment> comments = commentMapper.selectRootComments(videoId);
        // Ideally we fetch replies here too or let frontend fetch them lazily
        return Result.success(comments);
    }
    
    public Result<List<VideoComment>> getReplies(Long parentId) {
        return Result.success(commentMapper.selectReplies(parentId));
    }
}
