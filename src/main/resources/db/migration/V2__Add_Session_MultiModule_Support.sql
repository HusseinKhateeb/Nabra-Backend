-- V2__Add_Session_MultiModule_Support.sql
-- Migration to add multi-module support to session history
-- Adds: sessionType, content, contentRefId, deviceInfo, modelVersion, isOffline
-- and creates new indexes

ALTER TABLE sessions ADD COLUMN session_type VARCHAR(20) NOT NULL DEFAULT 'LIP_READING';
ALTER TABLE sessions ADD COLUMN content VARCHAR(5000);
ALTER TABLE sessions ADD COLUMN content_ref_id VARCHAR(100);
ALTER TABLE sessions ADD COLUMN device_info VARCHAR(500);
ALTER TABLE sessions ADD COLUMN model_version VARCHAR(50);
ALTER TABLE sessions ADD COLUMN is_offline BOOLEAN DEFAULT FALSE;

-- Create index on sessionType for faster filtering
CREATE INDEX idx_sessions_type ON sessions(session_type);

-- Create index on status for faster filtering
CREATE INDEX idx_sessions_status ON sessions(status);

-- Create comment for documentation
COMMENT ON COLUMN sessions.session_type IS 'Session type: LIP_READING, CHAT, VOICE_TO_TEXT, or LEARNING';
COMMENT ON COLUMN sessions.content IS 'Main content: transcript, chat summary, recognized text, learning progress summary';
COMMENT ON COLUMN sessions.content_ref_id IS 'Optional reference to related record (messageId, chatId, learningProgressId)';
COMMENT ON COLUMN sessions.device_info IS 'Device information (browser, OS, device type)';
COMMENT ON COLUMN sessions.model_version IS 'Model/algorithm version used in this session';
COMMENT ON COLUMN sessions.is_offline IS 'Whether session was processed offline';
