package com.flowbrain.viewx.service.impl;

import com.flowbrain.viewx.common.Result;
import com.flowbrain.viewx.dao.ConversationMapper;
import com.flowbrain.viewx.dao.MessageMapper;
import com.flowbrain.viewx.dao.UserMapper;
import com.flowbrain.viewx.pojo.dto.MessageDTO;
import com.flowbrain.viewx.pojo.entity.Conversation;
import com.flowbrain.viewx.pojo.entity.Message;
import com.flowbrain.viewx.pojo.entity.User;
import com.flowbrain.viewx.pojo.vo.ConversationVO;
import com.flowbrain.viewx.pojo.vo.MessageVO;
import com.flowbrain.viewx.service.ChatService;
import com.flowbrain.viewx.dao.FollowMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.flowbrain.viewx.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 聊天服务实现类
 */
@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private ConversationMapper conversationMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private LocalStorageStrategy storageStrategy;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private FollowMapper followMapper;

    private static final String ONLINE_USER_KEY = "chat:online:";

    @Override
    @Transactional
    public Result<MessageVO> sendMessage(Long senderId, MessageDTO messageDTO) {
        try {
            // 1. 验证接收者是否存在
            User receiver = userMapper.selectById(messageDTO.getReceiverId());
            if (receiver == null) {
                return Result.notFound("接收者不存在");
            }

            // 2. 检查关注关系
            boolean isMutualFollow = checkMutualFollow(senderId, messageDTO.getReceiverId());

            // 3. 如果不是互相关注，检查消息限制
            if (!isMutualFollow) {
                // 检查内容长度（限制100字）
                if (messageDTO.getContent() != null && messageDTO.getContent().length() > 100) {
                    return Result.badRequest("非互关用户发送消息不能超过100字");
                }

                // 检查是否已经发送过消息
                long sentCount = messageMapper.selectCount(
                        new QueryWrapper<Message>()
                                .eq("sender_id", senderId)
                                .eq("receiver_id", messageDTO.getReceiverId()));

                if (sentCount >= 1) {
                    return Result.forbidden("非互关用户只能发送一条消息，请先互相关注");
                }
            }

            // 4. 创建消息实体
            Message message = new Message();
            message.setId(IdGenerator.nextId());
            message.setSenderId(senderId);
            message.setReceiverId(messageDTO.getReceiverId());
            message.setContent(messageDTO.getContent());
            message.setMessageType(messageDTO.getMessageType() != null ? messageDTO.getMessageType() : "TEXT");
            message.setIsRead(false);
            message.setCreatedAt(LocalDateTime.now());

            // 5. 保存消息
            messageMapper.insert(message);

            // 6. 更新或创建会话
            updateConversation(senderId, messageDTO.getReceiverId(), message.getId(), message.getCreatedAt());

            // 7. 构建返回的 MessageVO
            MessageVO messageVO = new MessageVO();
            BeanUtils.copyProperties(message, messageVO);

            // 获取发送者信息
            User sender = userMapper.selectById(senderId);
            messageVO.setSenderUsername(sender.getUsername());
            messageVO.setSenderNickname(sender.getNickname());

            // 处理头像 URL
            if (sender.getDetails() != null && sender.getDetails().getAvatarUrl() != null) {
                String avatar = sender.getDetails().getAvatarUrl();
                if (!avatar.startsWith("http")) {
                    avatar = storageStrategy.getFileUrl(avatar);
                }
                messageVO.setSenderAvatar(avatar);
            }

            log.info("消息发送成功: {} -> {}", senderId, messageDTO.getReceiverId());
            return Result.success(messageVO);

        } catch (Exception e) {
            log.error("发送消息失败", e);
            return Result.serverError("发送消息失败");
        }
    }

    @Override
    public Result<List<MessageVO>> getChatHistory(Long userId, Long otherUserId, int page, int size) {
        try {
            int offset = (page - 1) * size;
            List<MessageVO> messages = messageMapper.getChatHistory(userId, otherUserId, offset, size);

            // 处理头像 URL
            for (MessageVO message : messages) {
                if (message.getSenderAvatar() != null && !message.getSenderAvatar().startsWith("http")) {
                    message.setSenderAvatar(storageStrategy.getFileUrl(message.getSenderAvatar()));
                }
            }

            return Result.success(messages);
        } catch (Exception e) {
            log.error("获取聊天历史失败", e);
            return Result.serverError("获取聊天历史失败");
        }
    }

    @Override
    public Result<List<ConversationVO>> getConversations(Long userId) {
        try {
            // 查询用户的所有会话
            List<Conversation> conversations = conversationMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Conversation>()
                            .and(wrapper -> wrapper.eq("user1_id", userId).or().eq("user2_id", userId))
                            .orderByDesc("last_message_time"));

            List<ConversationVO> conversationVOs = new ArrayList<>();

            for (Conversation conversation : conversations) {
                ConversationVO vo = new ConversationVO();
                vo.setConversationId(conversation.getId());

                // 确定对方用户 ID
                Long otherUserId = conversation.getUser1Id().equals(userId)
                        ? conversation.getUser2Id()
                        : conversation.getUser1Id();

                // 获取对方用户信息（包括详情）
                User otherUser = userMapper.selectUserWithDetailsById(otherUserId);
                if (otherUser != null) {
                    vo.setOtherUserId(otherUserId);
                    vo.setOtherUserUsername(otherUser.getUsername());
                    vo.setOtherUserNickname(otherUser.getNickname());
                    vo.setOtherUserAvatar(otherUser.getDetails().getAvatarUrl());

                    // 处理头像
                    if (otherUser.getDetails() != null && otherUser.getDetails().getAvatarUrl() != null) {
                        String avatar = otherUser.getDetails().getAvatarUrl();
                        if (!avatar.startsWith("http")) {
                            avatar = storageStrategy.getFileUrl(avatar);
                        }
                        vo.setOtherUserAvatar(avatar);
                    }

                    // 检查在线状态
                    Boolean isOnline = redisTemplate.hasKey(ONLINE_USER_KEY + otherUserId);
                    vo.setIsOnline(isOnline != null && isOnline);
                }

                // 获取最后一条消息
                if (conversation.getLastMessageId() != null) {
                    Message lastMessage = messageMapper.selectById(conversation.getLastMessageId());
                    if (lastMessage != null) {
                        vo.setLastMessage(lastMessage.getContent());
                        vo.setLastMessageType(lastMessage.getMessageType());
                    }
                }

                vo.setLastMessageTime(conversation.getLastMessageTime());

                // 获取未读数
                int unreadCount = conversation.getUser1Id().equals(userId)
                        ? conversation.getUnreadCountUser1()
                        : conversation.getUnreadCountUser2();
                vo.setUnreadCount(unreadCount);

                conversationVOs.add(vo);
            }

            return Result.success(conversationVOs);
        } catch (Exception e) {
            log.error("获取会话列表失败", e);
            return Result.serverError("获取会话列表失败");
        }
    }

    @Override
    @Transactional
    public Result<Void> markAsRead(Long userId, Long otherUserId) {
        try {
            messageMapper.markAsRead(userId, otherUserId);
            conversationMapper.clearUnreadCount(userId, otherUserId);
            return Result.success("标记已读成功");
        } catch (Exception e) {
            log.error("标记已读失败", e);
            return Result.serverError("标记已读失败");
        }
    }

    @Override
    public Result<Integer> getTotalUnreadCount(Long userId) {
        try {
            int count = messageMapper.getTotalUnreadCount(userId);
            return Result.success(count);
        } catch (Exception e) {
            log.error("获取未读消息数失败", e);
            return Result.serverError("获取未读消息数失败");
        }
    }

    @Override
    @Transactional
    public Result<Void> recallMessage(Long userId, Long messageId) {
        try {
            // 检查消息是否可以撤回
            boolean canRecall = messageMapper.canRecallMessage(messageId, userId);

            if (!canRecall) {
                return Result.badRequest("无法撤回该消息（只能撤回自己发送的消息，且在2分钟内）");
            }

            // 执行撤回
            int rows = messageMapper.recallMessage(messageId, userId);

            if (rows > 0) {
                log.info("消息撤回成功: messageId={}, userId={}", messageId, userId);
                return Result.success("消息撤回成功");
            } else {
                return Result.badRequest("消息撤回失败");
            }
        } catch (Exception e) {
            log.error("撤回消息失败", e);
            return Result.serverError("撤回消息失败");
        }
    }

    @Override
    @Transactional
    public Result<Void> deleteMessage(Long userId, Long messageId) {
        try {
            // 检查消息是否属于该用户
            boolean belongs = messageMapper.isMessageBelongsToUser(messageId, userId);

            if (!belongs) {
                return Result.forbidden("无权删除该消息");
            }

            // 执行删除（软删除）
            int rows = messageMapper.deleteMessage(messageId, userId);

            if (rows > 0) {
                log.info("消息删除成功: messageId={}, userId={}", messageId, userId);
                return Result.success("消息删除成功");
            } else {
                return Result.badRequest("消息删除失败");
            }
        } catch (Exception e) {
            log.error("删除消息失败", e);
            return Result.serverError("删除消息失败");
        }
    }

    /**
     * 更新或创建会话
     */
    private void updateConversation(Long user1Id, Long user2Id, Long messageId, LocalDateTime messageTime) {
        // 确保 user1Id < user2Id
        Long smallerId = Math.min(user1Id, user2Id);
        Long largerId = Math.max(user1Id, user2Id);

        Conversation conversation = conversationMapper.getConversation(smallerId, largerId);

        if (conversation == null) {
            // 创建新会话
            conversation = new Conversation();
            conversation.setId(IdGenerator.nextId());
            conversation.setUser1Id(smallerId);
            conversation.setUser2Id(largerId);
            conversation.setLastMessageId(messageId);
            conversation.setLastMessageTime(messageTime);
            conversation.setUnreadCountUser1(smallerId.equals(user2Id) ? 1 : 0);
            conversation.setUnreadCountUser2(largerId.equals(user2Id) ? 1 : 0);
            conversation.setCreatedAt(LocalDateTime.now());
            conversationMapper.insert(conversation);
        } else {
            // 更新现有会话
            conversationMapper.updateLastMessage(conversation.getId(), messageId, messageTime, user2Id);
        }
    }

    /**
     * 设置用户在线状态
     */
    public void setUserOnline(Long userId) {
        redisTemplate.opsForValue().set(ONLINE_USER_KEY + userId, true, 30, TimeUnit.MINUTES);
    }

    /**
     * 设置用户离线
     */
    public void setUserOffline(Long userId) {
        redisTemplate.delete(ONLINE_USER_KEY + userId);
    }

    /**
     * 检查两个用户是否互相关注
     */
    private boolean checkMutualFollow(Long user1Id, Long user2Id) {
        // 检查 user1 是否关注 user2
        int user1FollowsUser2 = followMapper.checkFollow(user1Id, user2Id);
        // 检查 user2 是否关注 user1
        int user2FollowsUser1 = followMapper.checkFollow(user2Id, user1Id);

        // 只有双方都关注了对方才返回 true
        return user1FollowsUser2 > 0 && user2FollowsUser1 > 0;
    }
}
