# 🔄 ViewX 系统业务流程图 (System Flowcharts)

本文档使用 Mermaid 语法展示 ViewX 核心业务的流程逻辑。

## 1. 📹 视频上传与处理流程 (Video Upload & Processing)

展示视频从用户上传到最终发布的完整生命周期，包括转码和 AI 分析。

```mermaid
sequenceDiagram
    participant User as 用户
    participant API as 后端 API
    participant OSS as 对象存储 (MinIO/OSS)
    participant DB as 数据库
    participant MQ as RabbitMQ
    participant Transcode as 转码服务
    participant AI as AI 分析服务

    User->>API: 1. 请求上传凭证 (VideoCreateDTO)
    API->>DB: 创建视频记录 (Status: PENDING)
    API-->>User: 返回上传 URL/凭证
    
    User->>OSS: 2. 直传视频文件
    OSS-->>API: 回调通知上传完成
    
    API->>DB: 更新状态 (Status: PROCESSING)
    API->>MQ: 发送处理消息 (VideoId)
    
    par 并行处理
        MQ->>Transcode: 3. 触发转码任务
        Transcode->>OSS: 获取源文件
        Transcode->>OSS: 存储转码后文件 (m3u8/mp4)
        Transcode-->>API: 转码完成回调
    and
        MQ->>AI: 4. 触发 AI 分析
        AI->>OSS: 获取视频/封面
        AI->>AI: 生成摘要、标签、情感分析
        AI->>AI: 生成向量 Embedding
        AI-->>API: 分析结果回调
    end
    
    API->>DB: 5. 保存转码地址 & AI 元数据
    API->>DB: 更新状态 (Status: PUBLIC)
    API->>MQ: 发送"新视频发布"通知
```

## 2. ❤️ 用户交互与异步通知 (Interaction & Notification)

展示点赞、评论等操作如何通过 MQ 实现削峰填谷和异步通知。

```mermaid
flowchart TD
    User[用户] -->|点击点赞/评论| API[交互接口]
    
    subgraph Synchronous_Processing
        API -->|1. 校验参数| Validate{校验通过?}
        Validate -->|No| Error[返回错误]
        Validate -->|Yes| Redis[Redis 缓存计数]
        Redis -->|2. 立即返回成功| Response[响应前端]
    end
    
    subgraph Asynchronous_Processing_RabbitMQ
        Response -.->|3. 发送消息| MQ[RabbitMQ Exchange]
        
        MQ -->|路由: log| Q_Log[日志队列]
        MQ -->|路由: recommend| Q_Rec[推荐计算队列]
        MQ -->|路由: notify| Q_Notify[通知队列]
        
        Q_Log --> C_Log[日志消费者] --> DB_Log[(行为日志表)]
        
        Q_Rec --> C_Rec[推荐消费者] 
        C_Rec --> DB_Video[(视频表更新分数)]
        C_Rec --> Redis_ZSet[(Redis热榜 ZSet)]
        
        Q_Notify --> C_Notify[通知消费者]
        C_Notify -->|判断非本人| DB_Notify[(通知表)]
        DB_Notify -->|WebSocket| Push[实时推送]
    end
```

## 3. 🧠 AI 分析与语义搜索 (AI Analysis & Semantic Search)

展示如何利用向量数据库实现语义搜索。

```mermaid
flowchart LR
    subgraph Data_Ingestion_Phase
        Video[视频内容] --> AI_Model[AI 模型]
        AI_Model -->|提取| Summary[文本摘要]
        AI_Model -->|提取| Tags[标签]
        
        Summary & Tags --> Embedding_Model[Embedding 模型]
        Embedding_Model -->|生成向量| Vector[512维向量]
        Vector --> DB[(PostgreSQL + pgvector)]
    end
    
    subgraph Search_Phase
        Query[用户搜索] --> Embed_Q[Embedding 模型]
        Embed_Q -->|生成查询向量| Vector_Q[查询向量]
        
        Vector_Q --> DB
        DB -->|余弦相似度计算| Results[相似视频列表]
        Results --> User[返回给用户]
    end
```

## 4. 🔐 OAuth2 第三方登录流程 (OAuth2 Login)

```mermaid
sequenceDiagram
    participant User as 用户
    participant Browser as 浏览器
    participant ViewX as ViewX 后端
    participant GitHub as GitHub 认证服务器
    participant DB as 数据库

    User->>Browser: 点击 "Login with GitHub"
    Browser->>ViewX: 请求授权 URL
    ViewX-->>Browser: 重定向到 GitHub
    
    Browser->>GitHub: 用户确认授权
    GitHub-->>Browser: 重定向回 ViewX (带 Code)
    
    Browser->>ViewX: GET /login/oauth2/code/github?code=xxx
    
    activate ViewX
    ViewX->>GitHub: 用 Code 换 Access Token
    GitHub-->>ViewX: 返回 Access Token
    
    ViewX->>GitHub: 用 Token 获取用户信息 (User Info)
    GitHub-->>ViewX: 返回 {id, name, email...}
    
    ViewX->>DB: 查询 social_users 表
    alt 已绑定
        ViewX->>DB: 获取 user_id
    else 未绑定
        ViewX->>DB: 创建新用户 (vx_users)
        ViewX->>DB: 绑定关联 (vx_social_users)
    end
    
    ViewX->>ViewX: 生成 JWT Token
    deactivate ViewX
    
    ViewX-->>Browser: 重定向到前端 /oauth/callback?token=jwt
    Browser->>User: 登录成功，显示首页
```
