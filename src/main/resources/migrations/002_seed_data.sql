-- Seed data for development and testing
-- Time slots are generated at startup by SlotGenerator from each resource's settings.

-- Default admin user (password: admin123)
-- Password hash is salted PBKDF2-HMAC-SHA256 (see PasswordUtil)
INSERT INTO users (username, email, password_hash, role) VALUES
('admin', 'admin@reservo.edu', 'pbkdf2$210000$vRQX0E4iIOHg0KkHgoaiJg==$B2NNsgDyUwBoQW9GFvbnm8berO07mzlGdvOk1c/jSKU=', 'ADMIN');

-- Test users (password: test123 for all)
-- Password hashes are salted PBKDF2-HMAC-SHA256 (see PasswordUtil)
INSERT INTO users (username, email, password_hash, role) VALUES
('alice', 'alice@test.edu', 'pbkdf2$210000$fMJpL+0Gd5WYTafsk7fqYg==$8Q9goKjOZS+7qmrwdrJG82fqhgEhwPvu+HNjxsWsM8w=', 'USER'),
('bob', 'bob@test.edu', 'pbkdf2$210000$fMJpL+0Gd5WYTafsk7fqYg==$8Q9goKjOZS+7qmrwdrJG82fqhgEhwPvu+HNjxsWsM8w=', 'USER'),
('charlie', 'charlie@test.edu', 'pbkdf2$210000$fMJpL+0Gd5WYTafsk7fqYg==$8Q9goKjOZS+7qmrwdrJG82fqhgEhwPvu+HNjxsWsM8w=', 'USER');

-- Sample resources
INSERT INTO resources (name, description, capacity, slot_duration_minutes, booking_horizon_days, max_booking_hours_per_day, rules_json) VALUES
('Study Room A', 'Quiet study space with whiteboard and projector', 8, 60, 30, 4.0, '{"allow_group_bookings": true}'),
('Study Room B', 'Smaller study room for 2-4 people', 4, 60, 30, 4.0, '{"allow_group_bookings": true}'),
('Squash Court 1', 'Indoor squash court with equipment rental', 1, 60, 14, 2.0, '{"require_equipment": true}'),
('Squash Court 2', 'Indoor squash court with equipment rental', 1, 60, 14, 2.0, '{"require_equipment": true}');

-- Note: Time slots would be generated dynamically by the application
-- For testing, a few sample slots can be added manually if needed
