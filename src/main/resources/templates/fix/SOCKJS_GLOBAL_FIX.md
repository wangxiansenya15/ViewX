# SockJS Global Variable Fix

## Problem

When navigating to the Messages page (`/messages`), the application crashed with the following error:

```
ReferenceError: global is not defined
    at node_modules/sockjs-client/lib/utils/browser-crypto.js
```

## Root Cause

The `sockjs-client` library (used for WebSocket connections) expects a `global` variable to be available in the browser environment. This is a Node.js global that doesn't exist in browsers by default.

In Vite-based projects, this polyfill needs to be explicitly provided.

## Solution

Added a `define` configuration in `vite.config.ts` to map `global` to `globalThis`:

```typescript
define: {
  // Fix for sockjs-client: provide global variable
  global: 'globalThis'
}
```

## Files Modified

- **vite.config.ts**: Added `define` configuration

## Testing

After this fix:
1. Restart the Vite dev server: `npm run dev`
2. Navigate to Messages page (`/messages`)
3. The page should load without errors
4. WebSocket connection should establish successfully

## Related Issues Fixed

This fix resolves:
- ✅ Messages page navigation error
- ✅ WebSocket connection initialization
- ✅ Chat functionality in Messages view

## Technical Details

### What is `globalThis`?

`globalThis` is a standard JavaScript global object that works across all environments:
- In browsers: `globalThis === window`
- In Node.js: `globalThis === global`
- In Web Workers: `globalThis === self`

By mapping `global` to `globalThis`, we ensure that `sockjs-client` can access the global object regardless of the environment.

### Why SockJS Needs This

SockJS uses the `global` variable to:
1. Access crypto APIs for random number generation
2. Store connection state
3. Handle cross-browser compatibility

## Additional Notes

### Alternative Solutions (Not Recommended)

1. **Using a polyfill script**: Adding `<script>var global = globalThis;</script>` in `index.html`
   - ❌ Not recommended: Pollutes global scope

2. **Replacing sockjs-client**: Using native WebSocket or other libraries
   - ❌ Not recommended: Requires rewriting WebSocket service

3. **Using Vite plugin**: Installing `@esbuild-plugins/node-globals-polyfill`
   - ❌ Overkill for this simple fix

### Current Solution Benefits

✅ Clean and minimal
✅ No additional dependencies
✅ Standard Vite configuration
✅ Works in production builds
✅ No runtime overhead

## Verification

To verify the fix is working:

```bash
# 1. Restart dev server
npm run dev

# 2. Open browser console
# 3. Navigate to /messages
# 4. Check console for:
#    - No "global is not defined" errors
#    - "WebSocket 连接成功" message
#    - No route navigation errors
```

## Related Files

- `src/utils/websocket.ts` - WebSocket service using SockJS
- `src/stores/chat.ts` - Chat store that initializes WebSocket
- `src/views/Messages.vue` - Messages page that triggers WebSocket connection

---

**Status**: ✅ Fixed
**Date**: 2025-12-17
**Impact**: Messages page now loads correctly
