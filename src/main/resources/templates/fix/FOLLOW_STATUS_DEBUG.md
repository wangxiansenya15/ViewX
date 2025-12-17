# Follow Status Not Showing - Debugging Guide

## Issue
After following a user through search, their profile still shows "关注" (Follow) button instead of "已关注" (Following).

## Recent Fix Applied

### Code Changes
1. **Reset follow status on profile load** - `isFollowing.value = false` at the start of `fetchProfile()`
2. **Moved follow status fetch** - Now fetches immediately after getting user profile
3. **Added console logging** - Logs follow status for debugging

### File Modified
- `ViewX-frontend/src/views/Profile.vue`

## Debugging Steps

### 1. Check Browser Console
Open the browser console (F12) and look for:

```
Follow status fetched: true/false for user: {userId}
```

This will tell you:
- ✅ If the API call is being made
- ✅ What the server is returning
- ✅ Which user ID is being checked

### 2. Check Network Tab
In browser DevTools → Network tab, look for:

**Request:**
```
GET /api/interactions/follow/status/{userId}
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "success",
  "data": true  // or false
}
```

### 3. Verify API Endpoint

#### Frontend API Call
```typescript
// In src/api/index.ts
isFollowing(userId: number) {
    return request.get<boolean>(`/interactions/follow/status/${userId}`)
}
```

#### Backend Endpoint
```java
// In InteractionController.java
@GetMapping("/follow/status/{userId}")
public Result<Boolean> isFollowing(@PathVariable Long userId) {
    Long currentUserId = getCurrentUserId();
    if (currentUserId == null) {
        return Result.success(false);
    }
    boolean following = interactionService.isFollowing(currentUserId, userId);
    return Result.success(following);
}
```

**Full URL:** `http://localhost:8080/api/interactions/follow/status/{userId}`

### 4. Common Issues and Solutions

#### Issue 1: API Returns 401 Unauthorized
**Cause:** User not logged in or token expired
**Solution:** 
- Check if `localStorage.getItem('token')` exists
- Try logging out and logging back in
- Check if token is being sent in request headers

#### Issue 2: API Returns 404 Not Found
**Cause:** Backend endpoint not available
**Solution:**
- Verify backend is running
- Check backend logs for errors
- Verify `InteractionController` is loaded

#### Issue 3: API Returns `false` Even After Following
**Cause:** Database not updated or cache issue
**Solution:**
- Check database: `SELECT * FROM vx_user_follows WHERE follower_id = ? AND followed_id = ?`
- Clear browser cache
- Check if `toggleFollow` API call succeeded

#### Issue 4: Console Shows No Logs
**Cause:** `fetchProfile()` not being called or error thrown
**Solution:**
- Check if there are any JavaScript errors in console
- Verify route parameter is correct: `/profile/{userId}`
- Check if `profile.value?.userId` is undefined

### 5. Manual Testing

#### Test Follow Status API Directly
```bash
# Replace {userId} with actual user ID
# Replace {token} with your JWT token

curl -X GET "http://localhost:8080/api/interactions/follow/status/123456789" \
  -H "Authorization: Bearer {token}"
```

**Expected Response:**
```json
{
  "code": 200,
  "message": "success",
  "data": true
}
```

#### Test Toggle Follow API
```bash
curl -X POST "http://localhost:8080/api/interactions/follow/123456789" \
  -H "Authorization: Bearer {token}"
```

### 6. Check Database Directly

```sql
-- Check if follow relationship exists
SELECT * FROM vx_user_follows 
WHERE follower_id = {your_user_id} 
  AND followed_id = {other_user_id};

-- If exists, should return 1 row
-- If not exists, should return 0 rows
```

### 7. Verify User ID Format

The user ID might be a large number (JavaScript precision issue).

**Check in console:**
```javascript
// In browser console on profile page
console.log('Route userId:', window.location.pathname.split('/').pop())
console.log('Profile userId:', profile.value?.userId)
```

Both should match exactly.

### 8. Force Refresh Follow Status

Add this temporary button to test:

```vue
<!-- In Profile.vue template -->
<button @click="testFollowStatus" class="px-4 py-2 bg-red-500 text-white">
  Test Follow Status
</button>
```

```typescript
// In Profile.vue script
const testFollowStatus = async () => {
  if (!profile.value?.userId) {
    console.log('No userId')
    return
  }
  
  console.log('Testing follow status for:', profile.value.userId)
  
  try {
    const status = await interactionApi.isFollowing(profile.value.userId)
    console.log('Follow status result:', status)
    isFollowing.value = status
    alert(`Follow status: ${status}`)
  } catch (err) {
    console.error('Error:', err)
    alert(`Error: ${err}`)
  }
}
```

## Expected Behavior

### Correct Flow
1. User A follows User B (via search or profile)
2. `toggleFollow()` API call succeeds
3. Database updated: `vx_user_follows` has new row
4. User A navigates to User B's profile
5. `fetchProfile()` is called
6. `isFollowing()` API call is made
7. Server checks database
8. Returns `true`
9. Frontend sets `isFollowing.value = true`
10. Button shows "已关注"

### If Button Shows "关注" Instead
Something failed in steps 2-9. Use debugging steps above to find where.

## Quick Fixes to Try

### Fix 1: Hard Refresh
```
Ctrl + Shift + R (Windows/Linux)
Cmd + Shift + R (Mac)
```

### Fix 2: Clear LocalStorage and Re-login
```javascript
// In browser console
localStorage.clear()
// Then login again
```

### Fix 3: Restart Backend
```bash
# Stop backend
# Restart backend
mvn spring-boot:run
```

### Fix 4: Check Backend Logs
Look for errors related to:
- `InteractionController`
- `isFollowing`
- Database connection

## Still Not Working?

### Collect This Information:
1. Browser console logs (screenshot)
2. Network tab showing the API call (screenshot)
3. Backend logs (text)
4. Database query result (text)
5. User IDs involved (both follower and followed)

### Temporary Workaround
Use the detailed follow status API instead:

```typescript
// Replace in Profile.vue
const followStatus = await interactionApi.getDetailedFollowStatus(profile.value.userId)
isFollowing.value = followStatus.isFollowing
```

This uses a different endpoint that might work better.

---

**Last Updated:** 2025-12-17
**Status:** Debugging in progress
