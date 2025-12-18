package com.flowbrain.viewx.service;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.dao.CommentMapper;
import com.flowbrain.viewx.pojo.entity.VideoComment;
import com.flowbrain.viewx.pojo.vo.CommentVO;
import com.flowbrain.viewx.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

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

    public Result<List<CommentVO>> getComments(Long videoId) {
        List<VideoComment> comments = commentMapper.selectRootComments(videoId);

        // Convert to VO and load replies
        List<CommentVO> commentVOs = comments.stream().map(comment -> {
            CommentVO vo = convertToVO(comment);

            // Load replies
            List<VideoComment> replies = commentMapper.selectReplies(comment.getId());
            vo.setReplies(replies.stream().map(this::convertToVO).collect(Collectors.toList()));
            vo.setReplyCount(replies.size());

            return vo;
        }).collect(Collectors.toList());

        return Result.success(commentVOs);
    }

    public Result<List<CommentVO>> getReplies(Long parentId) {
        List<VideoComment> replies = commentMapper.selectReplies(parentId);
        List<CommentVO> replyVOs = replies.stream().map(this::convertToVO).collect(Collectors.toList());
        return Result.success(replyVOs);
    }

    private CommentVO convertToVO(VideoComment comment) {
        CommentVO vo = new CommentVO();
        BeanUtils.copyProperties(comment, vo);
        vo.setIsLiked(false); // TODO: 根据当前用户判断是否点赞
        return vo;
    }
}
