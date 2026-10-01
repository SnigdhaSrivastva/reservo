-- Reservo Database Schema
-- Migration 001: Initial schema creation

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Resources table
CREATE TABLE IF NOT EXISTS resources (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    capacity INTEGER NOT NULL CHECK (capacity > 0),
    slot_duration_minutes INTEGER NOT NULL CHECK (slot_duration_minutes > 0),
    booking_horizon_days INTEGER NOT NULL DEFAULT 30 CHECK (booking_horizon_days > 0),
    max_booking_hours_per_day DECIMAL(10,2),
    rules_json TEXT, -- JSON blob for resource-specific rules
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Time slots table
CREATE TABLE IF NOT EXISTS time_slot (
    id SERIAL PRIMARY KEY,
    resource_id INTEGER NOT NULL REFERENCES resources(id) ON DELETE CASCADE,
    start_ts TIMESTAMP NOT NULL,
    end_ts TIMESTAMP NOT NULL,
    capacity_remaining INTEGER NOT NULL CHECK (capacity_remaining >= 0),
    version INTEGER NOT NULL DEFAULT 0, -- For optimistic locking
    UNIQUE(resource_id, start_ts, end_ts),
    CHECK (start_ts < end_ts)
);

CREATE INDEX idx_time_slot_resource_dates ON time_slot(resource_id, start_ts, end_ts);
CREATE INDEX idx_time_slot_start_ts ON time_slot(start_ts);

-- Holds table (temporary claims)
CREATE TABLE IF NOT EXISTS hold (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    time_slot_id INTEGER NOT NULL REFERENCES time_slot(id) ON DELETE CASCADE,
    qty INTEGER NOT NULL CHECK (qty > 0),
    expires_at TIMESTAMP NOT NULL,
    request_id VARCHAR(255) UNIQUE NOT NULL, -- For idempotency
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, time_slot_id) -- One active hold per user per slot
);

CREATE INDEX idx_hold_expires_at ON hold(expires_at);
CREATE INDEX idx_hold_user_id ON hold(user_id);

-- Reservations table
CREATE TABLE IF NOT EXISTS reservation (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    time_slot_id INTEGER NOT NULL REFERENCES time_slot(id) ON DELETE CASCADE,
    qty INTEGER NOT NULL CHECK (qty > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED' CHECK (status IN ('CONFIRMED', 'CANCELLED')),
    request_id VARCHAR(255) UNIQUE NOT NULL, -- For idempotency
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancelled_at TIMESTAMP
    -- Note: Partial unique index for CONFIRMED status enforced at application level
    -- H2 doesn't support partial indexes, so we rely on application logic
);

CREATE INDEX idx_reservation_user_id ON reservation(user_id);
CREATE INDEX idx_reservation_time_slot_id ON reservation(time_slot_id);
CREATE INDEX idx_reservation_status ON reservation(status);

-- Waitlist table (FIFO queue)
CREATE TABLE IF NOT EXISTS waitlist (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    time_slot_id INTEGER NOT NULL REFERENCES time_slot(id) ON DELETE CASCADE,
    queued_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, time_slot_id) -- One entry per user per slot
);

CREATE INDEX idx_waitlist_time_slot_queued ON waitlist(time_slot_id, queued_at);

-- Notifications table (simulated inbox)
CREATE TABLE IF NOT EXISTS notification (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notification_user_read ON notification(user_id, read);

-- Audit event log (append-only)
CREATE TABLE IF NOT EXISTS audit_event (
    id SERIAL PRIMARY KEY,
    ts TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    type VARCHAR(50) NOT NULL,
    payload_json TEXT NOT NULL,
    user_id INTEGER REFERENCES users(id),
    resource_id INTEGER REFERENCES resources(id)
);

CREATE INDEX idx_audit_event_ts ON audit_event(ts);
CREATE INDEX idx_audit_event_type ON audit_event(type);
CREATE INDEX idx_audit_event_user_id ON audit_event(user_id);

