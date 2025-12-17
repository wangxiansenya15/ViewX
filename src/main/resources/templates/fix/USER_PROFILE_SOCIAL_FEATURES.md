# User Profile Social Features Implementation

## Overview
Added social interaction features to user profiles, including follow/unfollow functionality, direct messaging, and clickable avatars in notifications to view user profiles.

## Features Implemented

### 1. User Profile Actions (Profile.vue)

#### For Other Users' Profiles
When viewing another user's profile (not your own), you now see:

**Follow Button**
- Shows "关注" (Follow) when not following
- Shows "已关注" (Following) when already following
- Gradient button style when not following (indigo to purple)
- Gray style when already following
- Displays loading state during API call
- Updates follower count in real-time
- Shows success/info messages using ElMessage

**Message Button**
- "发消息" (Send Message) button
- Navigates to Messages page with the user ID as a query parameter
- The Messages page will automatically open a conversation with that user

**Share Button**
- Remains available for all profiles

#### For Own Profile
- Edit Profile button (编辑资料)
- Share button

### 2. Notification Interactions (NotificationBell.vue)

#### Clickable Elements
**Avatar**
- Click sender's avatar to navigate to their profile
- Hover effect: ring-2 ring-indigo-500
- Smooth transition animations

**Nickname**
- Click sender's nickname to navigate to their profile
- Hover effect: text color changes to indigo-500
- Smooth transition

**Notification Item**
- Click anywhere on notification to:
  - Navigate to related video (if `relatedVideoId` exists)
  - Navigate to sender's profile (if no video, but `senderId` exists)
  - Mark notification as read

### 3. Follow Status Management

#### State Variables
```typescript
const isFollowing = ref(false)
const followLoading = ref(false)
```

#### API Integration
- Fetches follow status when loading another user's profile
- Uses `interactionApi.isFollowing(userId)` to check status
- Uses `interactionApi.toggleFollow(userId)` to follow/unfollow

#### Real-time Updates
- Follower count updates immediately after follow/unfollow
- UI state changes instantly
- Error handling with user-friendly messages

### 4. Chat Integration

#### Start Chat Flow
1. User clicks "发消息" button on profile
2. Navigates to `/messages?userId={userId}`
3. Messages page receives the userId from query params
4. Messages page can auto-select or create conversation with that user

## Files Modified

### Frontend

1. **Profile.vue**
   - Added Follow and Message buttons for other users
   - Added `isFollowing` and `followLoading` state
   - Implemented `toggleFollow()` method
   - Implemented `startChat()` method
   - Added `useRouter` import
   - Added `ElMessage` import
   - Fetches follow status on profile load

2. **NotificationBell.vue**
   - Made sender avatar clickable
   - Made sender nickname clickable
   - Added `goToUserProfile(userId)` method
   - Updated `handleNotificationClick` to use `/profile/` route
   - Added hover effects for clickable elements

## API Endpoints Used

### Follow/Unfollow
```typescript
// Check if following
GET /api/interactions/following/{userId}
Response: boolean

// Toggle follow
POST /api/interactions/follow/{userId}
Response: string ("关注成功" or "取消关注")
```

### User Profile
```typescript
// Get user profile
GET /api/user/profile/{userId}
Response: UserProfileVO
```

## User Experience Flow

### Scenario 1: Follow a User from Their Profile
1. User A visits User B's profile (`/profile/{userBId}`)
2. Profile loads and checks follow status
3. User A sees "关注" button
4. User A clicks "关注"
5. Button shows "处理中..." loading state
6. API call completes
7. Button changes to "已关注"
8. Follower count increases by 1
9. Success message appears: "关注成功"

### Scenario 2: Start a Chat from Profile
1. User A visits User B's profile
2. User A clicks "发消息" button
3. Navigates to `/messages?userId={userBId}`
4. Messages page opens with User B's conversation

### Scenario 3: View Profile from Notification
1. User A receives a notification (like, comment, follow)
2. User A clicks notification bell
3. Sees notification from User B
4. User A clicks User B's avatar or nickname
5. Navigates to User B's profile (`/profile/{userBId}`)
6. Dropdown closes automatically

## Styling

### Follow Button States

**Not Following**
```css
bg-gradient-to-r from-indigo-500 to-purple-600
hover:from-indigo-600 hover:to-purple-700
text-white
shadow-lg shadow-indigo-500/20
```

**Following**
```css
bg-gray-500/20
hover:bg-gray-500/30
text-[var(--text)]
```

### Message Button
```css
bg-white/5
hover:bg-white/10
border border-white/10
hover:border-white/20
```

### Clickable Avatar/Nickname
```css
cursor-pointer
hover:ring-2 hover:ring-indigo-500 (avatar)
hover:text-indigo-500 (nickname)
transition-all / transition-colors
```

## Error Handling

### Follow/Unfollow Errors
- Catches API errors
- Shows error message using `ElMessage.error()`
- Reverts UI state if operation fails
- Logs error to console for debugging

### Profile Load Errors
- Shows error state in UI
- Provides retry button
- Logs error to console

## Future Enhancements

1. **Mutual Follow Badge**
   - Show special badge if users follow each other
   - Enable direct messaging only for mutual follows

2. **Follow Suggestions**
   - Show "People you may know" section
   - Based on mutual follows or interests

3. **Follow Notifications**
   - Real-time notification when someone follows you
   - Notification badge update

4. **Block User**
   - Add block functionality
   - Hide blocked users' content

5. **Follow List Preview**
   - Show preview of mutual followers
   - Quick access to follower/following lists

## Testing Checklist

- [ ] Follow button works correctly
- [ ] Unfollow button works correctly
- [ ] Follower count updates in real-time
- [ ] Message button navigates to Messages page
- [ ] Avatar click navigates to user profile
- [ ] Nickname click navigates to user profile
- [ ] Follow status persists after page reload
- [ ] Error messages display correctly
- [ ] Loading states show during API calls
- [ ] Buttons disabled during loading
- [ ] Own profile shows Edit button instead of Follow/Message
- [ ] Notification dropdown closes after navigation

## Dependencies

- `vue-router` - Navigation
- `element-plus` - ElMessage for notifications
- `@/api` - API calls (interactionApi, userApi)

---

**Status**: ✅ Complete
**Date**: 2025-12-17
**Impact**: Enhanced social interaction features on user profiles
