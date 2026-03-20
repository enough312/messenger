CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    username VARCHAR(32) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    phone VARCHAR(20) UNIQUE,
    password_hash TEXT NOT NULL,
    display_name VARCHAR(64) NOT NULL,
    bio TEXT,
    avatar_url TEXT,
    status VARCHAR(16) DEFAULT 'offline',
    last_seen BIGINT,
    is_verified BOOLEAN DEFAULT false,
    is_blocked BOOLEAN DEFAULT false,
    two_fa_secret TEXT,
    two_fa_enabled BOOLEAN DEFAULT false,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS sessions (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    device_name VARCHAR(128),
    device_type VARCHAR(16),
    fcm_token TEXT,
    refresh_token TEXT UNIQUE NOT NULL,
    ip_address VARCHAR(64),
    user_agent TEXT,
    last_active BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    expires_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS contacts (
    owner_id UUID REFERENCES users(id) ON DELETE CASCADE,
    contact_id UUID REFERENCES users(id) ON DELETE CASCADE,
    created_at BIGINT NOT NULL,
    PRIMARY KEY (owner_id, contact_id)
);

CREATE TABLE IF NOT EXISTS user_blocks (
    owner_id UUID REFERENCES users(id) ON DELETE CASCADE,
    blocked_user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    created_at BIGINT NOT NULL,
    PRIMARY KEY (owner_id, blocked_user_id)
);

CREATE TABLE IF NOT EXISTS email_verifications (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    code VARCHAR(12) NOT NULL,
    expires_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS password_resets (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    code VARCHAR(12) NOT NULL,
    expires_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS chats (
    id UUID PRIMARY KEY,
    type VARCHAR(16) NOT NULL,
    name VARCHAR(128),
    description TEXT,
    avatar_url TEXT,
    invite_link VARCHAR(64) UNIQUE,
    owner_id UUID REFERENCES users(id),
    is_public BOOLEAN DEFAULT false,
    max_members INT DEFAULT 200000,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS chat_members (
    chat_id UUID REFERENCES chats(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(16) DEFAULT 'member',
    joined_at BIGINT NOT NULL,
    muted_until BIGINT,
    last_read_id UUID,
    PRIMARY KEY (chat_id, user_id)
);

CREATE TABLE IF NOT EXISTS messages (
    id UUID PRIMARY KEY,
    chat_id UUID REFERENCES chats(id) ON DELETE CASCADE,
    sender_id UUID REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(16) DEFAULT 'text',
    content TEXT,
    media_url TEXT,
    media_size BIGINT,
    media_mime VARCHAR(64),
    thumb_url TEXT,
    reply_to_id UUID,
    forward_from UUID,
    is_edited BOOLEAN DEFAULT false,
    is_deleted BOOLEAN DEFAULT false,
    is_pinned BOOLEAN DEFAULT false,
    metadata TEXT,
    created_at BIGINT NOT NULL,
    edited_at BIGINT
);

CREATE TABLE IF NOT EXISTS message_reads (
    message_id UUID REFERENCES messages(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    read_at BIGINT NOT NULL,
    PRIMARY KEY (message_id, user_id)
);

CREATE TABLE IF NOT EXISTS message_reactions (
    message_id UUID REFERENCES messages(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    emoji VARCHAR(32) NOT NULL,
    created_at BIGINT NOT NULL,
    PRIMARY KEY (message_id, user_id, emoji)
);

CREATE TABLE IF NOT EXISTS calls (
    id UUID PRIMARY KEY,
    chat_id UUID REFERENCES chats(id) ON DELETE CASCADE,
    caller_id UUID REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(16) NOT NULL,
    status VARCHAR(16) DEFAULT 'started',
    created_at BIGINT NOT NULL,
    ended_at BIGINT
);

CREATE TABLE IF NOT EXISTS signal_keys (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    identity_key TEXT NOT NULL,
    signed_pre_key TEXT NOT NULL,
    one_time_pre_keys TEXT NOT NULL,
    updated_at BIGINT NOT NULL
);
