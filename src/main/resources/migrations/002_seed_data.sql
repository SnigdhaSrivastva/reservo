-- Seed data for development and testing
-- Note: Simplified for H2 compatibility. For PostgreSQL, use generate_series for time slots.

-- Default admin user (password: admin123)
-- Password hash is SHA-256 of "admin123"
INSERT INTO users (username, email, password_hash, role) VALUES
('admin', 'admin@reservo.edu', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'ADMIN');

-- Test users (password: test123 for all)
-- Password hash is SHA-256 of "test123"
INSERT INTO users (username, email, password_hash, role) VALUES
('alice', 'alice@test.edu', 'ecd71870d1963316a97e3ac3408c9835ad8cf0f3c1bc703527c30265534f75ae', 'USER'),
('bob', 'bob@test.edu', 'ecd71870d1963316a97e3ac3408c9835ad8cf0f3c1bc703527c30265534f75ae', 'USER'),
('charlie', 'charlie@test.edu', 'ecd71870d1963316a97e3ac3408c9835ad8cf0f3c1bc703527c30265534f75ae', 'USER');

-- Sample resources
INSERT INTO resources (name, description, capacity, slot_duration_minutes, booking_horizon_days, max_booking_hours_per_day, rules_json) VALUES
('Study Room A', 'Quiet study space with whiteboard and projector', 8, 60, 30, 4.0, '{"allow_group_bookings": true}'),
('Study Room B', 'Smaller study room for 2-4 people', 4, 60, 30, 4.0, '{"allow_group_bookings": true}'),
('Squash Court 1', 'Indoor squash court with equipment rental', 1, 60, 14, 2.0, '{"require_equipment": true}'),
('Squash Court 2', 'Indoor squash court with equipment rental', 1, 60, 14, 2.0, '{"require_equipment": true}');

-- Note: Time slots would be generated dynamically by the application
-- For testing, a few sample slots can be added manually if needed
