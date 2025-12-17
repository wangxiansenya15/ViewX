# Chat and Follow Status Improvements

## Overview
Fixed two issues with the social features:
1. **Direct messaging** - Messages page now automatically opens conversation when navigating from user profile
2. **Mutual follow display** - Profile page now shows "相互关注" (Mutual Follow) status

## Changes Made

### 1. Messages Page - Auto-Open Conversation

#### File: `Messages.vue`

**Added Features:**
- Route query parameter support (`/messages?userId=123`)
- Automatic conversation selection when userId is provided
- Route watcher to handle dynamic userId changes

**Implementation:**
```typescript
// Check for userId query parameter on mount
if (route.query.userId) {
  await openConversationByUserId(route.query.userId as string)
}

// Watch for route changes
watch(() => route.query.userId, (newUserId) => {
  if (newUserId && chatStore.conversations.length > 0) {
    openConversationByUserId(newUserId as string)
  }
})
```

**Function: `openConversationByUserId`**
- Searches existing conversations for the target user
- Automatically selects conversation if found
- Logs message if conversation doesn't exist (TODO: create new conversation)

### 2. Profile Page - Mutual Follow Status

#### File: `Profile.vue`

**Added State:**
```typescript
const isMutualFollow = ref(false)
```

**API Change:**
- Changed from `interactionApi.isFollowing()` (returns boolean)
- To `interactionApi.getDetailedFollowStatus()` (returns FollowStatusVO)

**FollowStatusVO Structure:**
```typescript
interface FollowStatusVO {
    isFollowing: boolean      // Current user follows this user
    isFollower: boolean       // This user follows current user
    isMutual: boolean         // Mutual follow relationship
    statusText: string        // "关注" | "已关注" | "相互关注"
}
```

**Button Text Logic:**
```typescript
{{ followLoading ? '处理中...' : (isMutualFollow ? '相互关注' : (isFollowing ? '已关注' : '关注')) }}
```

**Display Priority:**
1. Loading: "处理中..."
2. Mutual: "相互关注"
3. Following: "已关注"
4. Not following: "关注"

## User Flow

### Starting a Chat from Profile

**Before:**
1. User A visits User B's profile
2. Clicks "发消息" button
3. Navigates to `/messages`
4. ❌ Shows empty chat window
5. User has to manually find User B in conversation list

**After:**
1. User A visits User B's profile
2. Clicks "发消息" button
3. Navigates to `/messages?userId={userBId}`
4. ✅ Automatically opens chat with User B
5. User can start typing immediately

### Viewing Mutual Follow Status

**Before:**
1. User A visits User B's profile
2. Button shows "已关注" (Following)
3. ❌ No indication if User B also follows User A

**After:**
1. User A visits User B's profile
2. If mutual: Button shows "相互关注" (Mutual Follow)
3. If one-way: Button shows "已关注" (Following)
4. ✅ Clear indication of relationship type

## API Endpoints Used

### Get Detailed Follow Status
```
GET /api/interactions/follow/detailed-status/{userId}

Response:
{
  "code": 200,
  "message": "success",
  "data": {
    "isFollowing": true,
    "isFollower": true,
    "isMutual": true,
    "statusText": "相互关注"
  }
}
```

## TODO: Create New Conversation

Currently, if a conversation doesn't exist, the system only logs a message. Future implementation should:

1. **Fetch user info** via API
2. **Create temporary conversation object**:
```typescript
const tempConversation: ConversationVO = {
  conversationId: 0, // Temporary ID
  otherUserId: numericUserId,
  otherUserUsername: userData.username,
  otherUserNickname: userData.nickname,
  otherUserAvatar: userData.avatarUrl,
  isOnline: false,
  lastMessage: '',
  lastMessageType: 'TEXT',
  lastMessageTime: new Date().toISOString(),
  unreadCount: 0
}
```
3. **Select the temporary conversation**
4. **First message will create the conversation** in database

## Testing

### Test Chat Navigation
1. Login as User A
2. Navigate to User B's profile (`/profile/{userBId}`)
3. Click "发消息" button
4. Verify URL changes to `/messages?userId={userBId}`
5. Verify chat window opens with User B
6. Send a test message

### Test Mutual Follow Display
1. Login as User A
2. Follow User B
3. Login as User B (different browser/incognito)
4. Follow User A
5. Login as User A again
6. Navigate to User B's profile
7. Verify button shows "相互关注"

### Test One-Way Follow Display
1. Login as User A
2. Follow User C (who doesn't follow back)
3. Navigate to User C's profile
4. Verify button shows "已关注"

## Files Modified

### Frontend
1. **Messages.vue**
   - Added route parameter handling
   - Added `openConversationByUserId()` function
   - Added route watcher
   - Imported `useRoute` from vue-router

2. **Profile.vue**
   - Added `isMutualFollow` state
   - Changed to use `getDetailedFollowStatus()` API
   - Updated button text logic
   - Reset `isMutualFollow` on profile load

## Benefits

### User Experience
- ✅ Faster access to chat conversations
- ✅ Clear relationship status indication
- ✅ Reduced clicks to start chatting
- ✅ Better understanding of social connections

### Technical
- ✅ Reusable conversation opening logic
- ✅ URL-based state management
- ✅ Detailed follow status caching
- ✅ Proper state reset on navigation

## Known Limitations

1. **New Conversations**: Currently doesn't create new conversations automatically
   - Workaround: User must send first message to create conversation
   - Future: Implement automatic conversation creation

2. **Conversation Not Found**: If user hasn't chatted before, shows empty window
   - Workaround: Log message in console
   - Future: Show "Start new conversation" UI

3. **Follow Status Caching**: Status fetched on every profile load
   - Current: Fresh data every time
   - Future: Consider caching with TTL

## Future Enhancements

1. **Auto-create conversations** when clicking "发消息"
2. **Show mutual follow badge** next to username
3. **Quick actions** in mutual follow state (e.g., "Send Message" shortcut)
4. **Follow suggestions** based on mutual connections
5. **Notification** when someone becomes mutual follower

---

**Status**: ✅ Complete
**Date**: 2025-12-17
**Impact**: Improved chat navigation and follow status visibility
