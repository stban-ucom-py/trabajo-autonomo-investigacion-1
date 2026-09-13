CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY,
    external_id VARCHAR(80) NOT NULL UNIQUE,
    subject VARCHAR(160) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS deliveries (
    id BIGSERIAL PRIMARY KEY,
    notification_id UUID NOT NULL REFERENCES notifications(id),
    channel VARCHAR(20) NOT NULL,
    destination VARCHAR(320) NOT NULL,
    status VARCHAR(30) NOT NULL,
    provider_message_id VARCHAR(100),
    error_message TEXT,
    attempts INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(notification_id, channel)
);

CREATE INDEX IF NOT EXISTS idx_deliveries_notification_id
    ON deliveries(notification_id);
