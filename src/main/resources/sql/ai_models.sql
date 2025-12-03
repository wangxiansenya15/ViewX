-- AI模型版本表
CREATE TABLE vx_ai_models (
                              id BIGINT PRIMARY KEY,

    -- 模型信息
                              model_name VARCHAR(100) NOT NULL,
                              model_version VARCHAR(50) NOT NULL,
                              model_type VARCHAR(50) NOT NULL CHECK (model_type IN (
                                                                                    'RECOMMENDATION', 'SEARCH', 'CONTENT_ANALYSIS', 'SAFETY', 'TRANSCRIPTION', 'EMBEDDING', 'CLASSIFICATION'
                                  )),

    -- 模型配置
                              model_path VARCHAR(500),
                              model_config JSONB,
                              input_schema JSONB,
                              output_schema JSONB,

    -- 性能指标
                              accuracy_score DECIMAL(5,4),
                              precision_score DECIMAL(5,4),
                              recall_score DECIMAL(5,4),
                              inference_time_ms DECIMAL(8,2),

    -- 部署状态
                              is_active BOOLEAN DEFAULT FALSE,
                              canary_percentage INTEGER DEFAULT 0 CHECK (canary_percentage >= 0 AND canary_percentage <= 100),

    -- 时间戳
                              created_at TIMESTAMPTZ DEFAULT NOW(),
                              deployed_at TIMESTAMPTZ,
                              last_updated_at TIMESTAMPTZ DEFAULT NOW(),

                              CONSTRAINT uk_vx_ai_models_name_version UNIQUE (model_name, model_version)
);

-- AI任务队列表
CREATE TABLE vx_ai_tasks (
                             id BIGINT PRIMARY KEY,

    -- 任务信息
                             task_type VARCHAR(50) NOT NULL,
                             task_priority INTEGER DEFAULT 5 CHECK (task_priority >= 1 AND task_priority <= 10),
                             task_payload JSONB NOT NULL,

    -- 状态跟踪
                             status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN (
                                                                                    'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED', 'RETRY'
                                 )),
                             retry_count INTEGER DEFAULT 0,
                             max_retries INTEGER DEFAULT 3,

    -- 结果和错误
                             result_data JSONB,
                             error_message TEXT,
                             error_stack_trace TEXT,

    -- 时间戳
                             created_at TIMESTAMPTZ DEFAULT NOW(),
                             started_at TIMESTAMPTZ,
                             completed_at TIMESTAMPTZ,
                             next_retry_at TIMESTAMPTZ,

    -- 关联模型
                             model_id BIGINT,

                             CONSTRAINT fk_vx_ai_tasks_model FOREIGN KEY (model_id) REFERENCES vx_ai_models(id)
);


-- AI模型表索引
CREATE INDEX idx_vx_ai_models_type ON vx_ai_models(model_type);
CREATE INDEX idx_vx_ai_models_active ON vx_ai_models(is_active);
CREATE INDEX idx_vx_ai_models_created ON vx_ai_models(created_at DESC);

-- AI任务表索引
CREATE INDEX idx_vx_ai_tasks_status ON vx_ai_tasks(status);
CREATE INDEX idx_vx_ai_tasks_priority ON vx_ai_tasks(task_priority DESC, created_at);
CREATE INDEX idx_vx_ai_tasks_created_at ON vx_ai_tasks(created_at);
CREATE INDEX idx_vx_ai_tasks_type ON vx_ai_tasks(task_type);
CREATE INDEX idx_vx_ai_tasks_retry ON vx_ai_tasks(next_retry_at) WHERE status = 'RETRY';
CREATE INDEX idx_vx_ai_tasks_model ON vx_ai_tasks(model_id);