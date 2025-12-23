# Redis 缓存内存优化方案

## 📊 内存使用分析

### 1. 原始方案内存占用估算

假设系统规模：
- 用户数：100,000
- 视频数：100,000
- 点赞记录：1,000,000
- 收藏记录：500,000

#### 方案对比

| 缓存方案 | 单条数据 | 总数据量 | 内存占用 | 说明 |
|---------|---------|---------|---------|------|
| **传统 Hash 存储** | 100 bytes | 1,000,000 | ~95 MB | 每条点赞记录存储完整信息 |
| **Redis Bitmap** | 1 bit | 10,000,000,000 | ~1.2 GB | 100,000 视频 × 100,000 用户 |
| **优化 Bitmap** | 1 bit | 实际点赞数 | ~120 KB | 仅存储实际点赞的视频 |

### 2. 内存压力问题

**问题：**
- 如果使用全量 Bitmap，内存占用确实很大（1.2 GB）
- 视频列表缓存可能占用大量内存
- 用户会话数据可能膨胀

**解决方案：**
- ✅ 使用稀疏 Bitmap（只缓存热门视频）
- ✅ 设置合理的过期时间
- ✅ 使用 LRU 淘汰策略
- ✅ 分级缓存策略

## 🎯 优化后的缓存策略

### 策略 1：分级缓存

```
┌─────────────────────────────────────┐
│  L1: 本地缓存 (Caffeine)            │
│  - 极热数据                          │
│  - 容量：1000 条                     │
│  - 过期：5 分钟                      │
│  - 内存：~10 MB                      │
└─────────────────────────────────────┘
              ↓ Miss
┌─────────────────────────────────────┐
│  L2: Redis 缓存                      │
│  - 热数据                            │
│  - 容量：10000 条                    │
│  - 过期：30 分钟                     │
│  - 内存：~100 MB                     │
└─────────────────────────────────────┘
              ↓ Miss
┌─────────────────────────────────────┐
│  L3: 数据库                          │
│  - 全量数据                          │
└─────────────────────────────────────┘
```

### 策略 2：只缓存热门数据

**规则：**
- 只缓存最近 7 天的视频
- 只缓存浏览量 > 100 的视频
- 只缓存活跃用户的数据

**效果：**
- 缓存命中率：80-90%（略低于全量缓存）
- 内存占用：减少 90%

### 策略 3：智能过期时间

```java
// 根据数据热度动态调整过期时间
public int calculateTTL(Long viewCount) {
    if (viewCount > 10000) {
        return 3600; // 热门视频：1 小时
    } else if (viewCount > 1000) {
        return 1800; // 中等热度：30 分钟
    } else {
        return 300;  // 冷门视频：5 分钟
    }
}
```

## 💡 优化后的实现

### 1. 本地缓存 + Redis 二级缓存

```java
@Service
public class OptimizedInteractionCacheService {
    
    // L1: 本地缓存（Caffeine）
    private final Cache<String, Boolean> localCache = Caffeine.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(5, TimeUnit.MINUTES)
        .build();
    
    // L2: Redis 缓存
    private final RedisTemplate<String, Object> redisTemplate;
    
    public boolean isLiked(Long userId, Long videoId) {
        String key = "like:" + userId + ":" + videoId;
        
        // 1. 先查本地缓存
        Boolean cached = localCache.getIfPresent(key);
        if (cached != null) {
            return cached;
        }
        
        // 2. 再查 Redis
        Boolean redisResult = (Boolean) redisTemplate.opsForValue().get(key);
        if (redisResult != null) {
            localCache.put(key, redisResult);
            return redisResult;
        }
        
        // 3. 最后查数据库
        boolean result = interactionService.checkLikeFromDB(userId, videoId);
        
        // 只缓存热门视频的点赞状态
        if (isHotVideo(videoId)) {
            redisTemplate.opsForValue().set(key, result, 30, TimeUnit.MINUTES);
            localCache.put(key, result);
        }
        
        return result;
    }
    
    private boolean isHotVideo(Long videoId) {
        // 检查视频是否为热门（浏览量 > 100）
        Long viewCount = getVideoViewCount(videoId);
        return viewCount != null && viewCount > 100;
    }
}
```

### 2. 稀疏 Bitmap 实现

```java
@Service
public class SparseBitmapCacheService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 只为热门视频创建 Bitmap
     * 冷门视频直接查数据库
     */
    public boolean isLikedSparse(Long userId, Long videoId) {
        // 检查是否为热门视频
        if (!isHotVideo(videoId)) {
            // 冷门视频不缓存，直接查数据库
            return interactionService.checkLikeFromDB(userId, videoId);
        }
        
        // 热门视频使用 Bitmap
        String key = "like:bitmap:" + videoId;
        Boolean result = redisTemplate.opsForValue().getBit(key, userId);
        
        if (result == null) {
            boolean liked = interactionService.checkLikeFromDB(userId, videoId);
            redisTemplate.opsForValue().setBit(key, userId, liked);
            // 设置过期时间
            redisTemplate.expire(key, 1, TimeUnit.HOURS);
            return liked;
        }
        
        return result;
    }
}
```

### 3. 视频列表分页缓存

```java
@Service
public class OptimizedVideoCacheService {
    
    /**
     * 只缓存前几页数据
     * 深度分页不缓存
     */
    @Cacheable(value = "hot-videos", 
               key = "'page:' + #page", 
               condition = "#page <= 5") // 只缓存前 5 页
    public List<VideoListVO> getHotVideos(int page, int size) {
        return videoService.getHotVideosFromDB(page, size);
    }
}
```

## 📊 内存占用对比

### 优化前 vs 优化后

| 数据类型 | 优化前 | 优化后 | 节省 |
|---------|--------|--------|------|
| 点赞状态 | 1.2 GB | 12 MB | 99% ↓ |
| 视频列表 | 500 MB | 50 MB | 90% ↓ |
| 用户会话 | 200 MB | 20 MB | 90% ↓ |
| **总计** | **1.9 GB** | **82 MB** | **95% ↓** |

### 详细计算

#### 1. 点赞状态缓存

**优化前（全量 Bitmap）：**
```
100,000 视频 × 100,000 用户 × 1 bit = 1.2 GB
```

**优化后（只缓存热门视频）：**
```
假设热门视频占 10% = 10,000 个
10,000 视频 × 平均 100 个点赞 × 1 bit = 1.2 MB
再加上本地缓存 1000 条 × 100 bytes = 100 KB
总计：约 12 MB
```

#### 2. 视频列表缓存

**优化前（缓存所有分页）：**
```
假设 100 页 × 20 条/页 × 2 KB/条 = 4 MB
但多个分类、多个排序 = 4 MB × 100 = 400 MB
```

**优化后（只缓存前 5 页）：**
```
5 页 × 20 条/页 × 2 KB/条 = 200 KB
多个分类、多个排序 = 200 KB × 10 = 2 MB
加上本地缓存 = 5 MB
```

## ⚙️ Redis 配置优化

### 1. 设置最大内存限制

```conf
# redis.conf
maxmemory 512mb
maxmemory-policy allkeys-lru
```

### 2. 启用内存淘汰策略

```conf
# LRU 淘汰策略（推荐）
maxmemory-policy allkeys-lru

# 或者 LFU（最不常用）
maxmemory-policy allkeys-lfu
```

### 3. 监控内存使用

```bash
# 查看内存使用情况
redis-cli INFO memory

# 查看键空间统计
redis-cli INFO keyspace

# 查看最大内存配置
redis-cli CONFIG GET maxmemory
```

## 🎯 推荐配置

### 小型系统（< 1万用户）

```yaml
cache:
  local:
    max-size: 500
    expire: 5m
  redis:
    max-memory: 128mb
    ttl:
      hot-videos: 10m
      user-videos: 5m
      interaction: 15m
```

**预计内存占用：** ~50 MB

### 中型系统（1-10万用户）

```yaml
cache:
  local:
    max-size: 1000
    expire: 5m
  redis:
    max-memory: 512mb
    ttl:
      hot-videos: 30m
      user-videos: 10m
      interaction: 30m
    hot-video-threshold: 100  # 浏览量 > 100 才缓存
```

**预计内存占用：** ~200 MB

### 大型系统（> 10万用户）

```yaml
cache:
  local:
    max-size: 2000
    expire: 5m
  redis:
    max-memory: 2gb
    ttl:
      hot-videos: 60m
      user-videos: 20m
      interaction: 60m
    hot-video-threshold: 1000
  cluster:
    enabled: true
    nodes:
      - redis-1:6379
      - redis-2:6379
      - redis-3:6379
```

**预计内存占用：** ~1 GB（分布式）

## 📈 性能 vs 内存权衡

| 方案 | 缓存命中率 | 内存占用 | 推荐场景 |
|------|-----------|---------|---------|
| 无缓存 | 0% | 0 MB | 测试环境 |
| 仅本地缓存 | 60-70% | 10 MB | 单机小型系统 |
| 本地 + Redis（热数据） | 85-90% | 100 MB | **推荐方案** |
| 本地 + Redis（全量） | 95-98% | 2 GB | 大型系统 |

## ✅ 最终建议

### 1. 采用二级缓存

- **L1（本地）**：Caffeine，1000 条，5 分钟
- **L2（Redis）**：只缓存热门数据，30 分钟

### 2. 智能缓存策略

- 只缓存浏览量 > 100 的视频
- 只缓存前 5 页列表数据
- 根据热度动态调整 TTL

### 3. 内存监控

- 设置 Redis 最大内存：512 MB
- 启用 LRU 淘汰策略
- 定期监控内存使用

### 4. 预期效果

- **内存占用**：50-200 MB（根据系统规模）
- **缓存命中率**：85-90%
- **性能提升**：5-10 倍
- **成本**：极低（普通服务器即可）

这样既能保证性能提升，又不会造成内存压力！
