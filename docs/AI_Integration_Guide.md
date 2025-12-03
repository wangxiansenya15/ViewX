# 🤖 ViewX AI 能力集成指南

本文档介绍了如何集成和使用 AI 能力（基于 OpenAI 兼容接口，如阿里云百炼）。

## 1. 📦 依赖配置

虽然代码已经使用了 `RestTemplate` 进行解耦，但为了更好的体验，建议在 `pom.xml` 中添加 `spring-ai` 相关依赖（如果环境允许）。

目前实现依赖于以下配置 (`application.yml`)：

```yaml
spring:
  ai:
    openai:
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1  # 阿里云百炼兼容地址
      api-key: sk-xxxxxxxxxxxxxxxxxxxxxxxx  # 您的 API Key
```

## 2. ✨ 已实现功能

### A. 📝 AI 内容分析 (Content Analysis)
*   **功能**: 根据视频标题和描述，自动生成摘要、标签和情感分析。
*   **接口**: `POST /ai/analyze`
*   **Payload**: `{ "title": "...", "description": "..." }`
*   **实现**: 调用 LLM (如 Qwen-Turbo) 进行文本理解。

### B. 🔍 语义搜索 (Semantic Search / RAG)
*   **功能**: 支持自然语言搜索视频（例如搜“做饭的视频”能搜到标题只有“烹饪”的视频）。
*   **接口**: `GET /ai/search?query=做饭`
*   **实现**:
    1.  调用 Embedding 模型 (`text-embedding-v1`) 将用户 Query 转为向量。
    2.  利用 PostgreSQL `pgvector` 插件进行向量相似度查询 (`<->` 运算符)。

## 3. 🚀 未来扩展思路 (Roadmap)

### C. 🖼️ 多模态理解 (Multimodal)
*   **思路**: 截取视频关键帧（Keyframes），发送给 Vision 模型（如 GPT-4o 或 Qwen-VL）。
*   **应用**: 自动生成视频封面、检测视频中的物体/明星、审核不良画面。

### D. 🧠 智能体 (Agents / MCP)
*   **思路**: 引入 Model Context Protocol (MCP)。
*   **应用**: 创建一个“视频助手”，它可以：
    *   读取你的观看历史。
    *   调用 `RecommendService` 修改你的偏好。
    *   帮你自动回复评论。

### E. 📚 RAG 增强 (Advanced RAG)
*   **思路**: 将视频字幕 (SRT) 切片存入向量库。
*   **应用**: “视频问答” —— 用户问“视频里第几分钟提到了Java？”，AI 精确跳转到对应时间戳。

## 4. ⚠️ 注意事项
*   请确保 PostgreSQL 已安装 `vector` 扩展 (`CREATE EXTENSION vector;`)。
*   API Key 需要有足够的额度。
