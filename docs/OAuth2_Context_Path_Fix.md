# OAuth2 Context Path Fix Guide

## Problem
After adding `/api` as the context path in `application.yml`, the GitHub OAuth2 login flow is broken because:
1. All Spring Security OAuth2 endpoints are now prefixed with `/api`
2. GitHub OAuth App callback URL needs to be updated
3. Frontend needs to use the correct OAuth2 authorization URL

## Solution Steps

### 1. Update GitHub OAuth App Settings

Go to your GitHub OAuth App settings and update the **Authorization callback URL**:

**Old URL:**
```
http://localhost:8080/login/oauth2/code/github
```

**New URL (with /api prefix):**
```
http://localhost:8080/api/login/oauth2/code/github
```

**Steps:**
1. Go to https://github.com/settings/developers
2. Click on your OAuth App (or create a new one)
3. Update the "Authorization callback URL" to: `http://localhost:8080/api/login/oauth2/code/github`
4. Save changes

### 2. Frontend OAuth2 Login URL

Your frontend should initiate OAuth2 login using this URL:

```
http://localhost:8080/api/oauth2/authorization/github
```

**Example in your frontend code:**
```javascript
// In your login component
const handleGitHubLogin = () => {
  window.location.href = 'http://localhost:8080/api/oauth2/authorization/github';
};
```

Or if you're using environment variables:
```javascript
const handleGitHubLogin = () => {
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL; // http://localhost:8080/api
  window.location.href = `${apiBaseUrl}/oauth2/authorization/github`;
};
```

### 3. OAuth2 Flow with Context Path

Here's the complete OAuth2 flow with the `/api` context path:

```
1. User clicks "Login with GitHub" on frontend
   ↓
2. Frontend redirects to: http://localhost:8080/api/oauth2/authorization/github
   ↓
3. Spring Security redirects to GitHub: https://github.com/login/oauth/authorize?client_id=...
   ↓
4. User authorizes on GitHub
   ↓
5. GitHub redirects back to: http://localhost:8080/api/login/oauth2/code/github?code=...
   ↓
6. Spring Security exchanges code for access token
   ↓
7. Spring Security redirects to: http://localhost:8080/api/oauth2/success
   ↓
8. OAuth2Controller processes the login and redirects to frontend:
   http://localhost:5173/oauth/callback?token=<JWT_TOKEN>
   ↓
9. Frontend receives token and stores it
```

### 4. Backend Changes Made

The following changes have been made to `OAuth2Controller.java`:
- Error redirects now point to frontend URLs instead of backend paths
- Added better logging for debugging OAuth2 flow
- All redirects now use the `frontendUrl` configuration

### 5. Testing the OAuth2 Flow

1. **Start your backend server:**
   ```bash
   cd /home/arthur/Desktop/FinalStage/ViewX
   ./mvnw spring-boot:run
   ```

2. **Start your frontend server:**
   ```bash
   cd <your-frontend-directory>
   npm run dev
   ```

3. **Test the OAuth2 login:**
   - Click the "Login with GitHub" button on your frontend
   - You should be redirected to GitHub
   - After authorization, you should be redirected back with a token

4. **Check the logs:**
   - Backend logs should show: `OAuth2 Success: Provider=github, Attributes=...`
   - If there are errors, check the error messages in the logs

### 6. Common Issues and Solutions

#### Issue: 404 on callback
**Cause:** GitHub OAuth App callback URL is not updated
**Solution:** Update the callback URL in GitHub OAuth App settings (Step 1)

#### Issue: CORS errors
**Cause:** Frontend origin not allowed
**Solution:** Already configured in `SecurityConfig.java` - allows `http://localhost:*`

#### Issue: Invalid redirect URI error from GitHub
**Cause:** Callback URL mismatch between GitHub settings and Spring Security configuration
**Solution:** Ensure both use the same URL: `http://localhost:8080/api/login/oauth2/code/github`

### 7. Production Deployment

When deploying to production, remember to:

1. **Update GitHub OAuth App** with production callback URL:
   ```
   https://yourdomain.com/api/login/oauth2/code/github
   ```

2. **Update frontend OAuth2 URL** to use production API:
   ```
   https://yourdomain.com/api/oauth2/authorization/github
   ```

3. **Update `application-prod.yml`** with production frontend URL:
   ```yaml
   viewx:
     frontend:
       url: https://yourdomain.com
   ```

### 8. Alternative: Remove Context Path for OAuth2 Only

If you want to keep OAuth2 endpoints without the `/api` prefix while keeping it for other endpoints, you would need to:

1. Remove the global context path
2. Add `/api` prefix to all your REST controllers using `@RequestMapping("/api")`
3. Keep OAuth2 endpoints at the root level

**This is NOT recommended** as it's more complex and error-prone. The current solution (updating GitHub callback URL) is simpler and cleaner.

## Verification

After making these changes, verify the OAuth2 flow works by:

1. ✅ Frontend can redirect to GitHub authorization page
2. ✅ GitHub redirects back to your backend callback URL
3. ✅ Backend processes the OAuth2 response
4. ✅ Backend redirects to frontend with JWT token
5. ✅ Frontend receives and stores the token
6. ✅ User is logged in successfully

## Related Files

- `src/main/resources/application.yml` - Context path configuration
- `src/main/resources/application-dev.yml` - OAuth2 client configuration
- `src/main/java/com/flowbrain/viewx/config/SecurityConfig.java` - Security configuration
- `src/main/java/com/flowbrain/viewx/controller/OAuth2Controller.java` - OAuth2 callback handler
