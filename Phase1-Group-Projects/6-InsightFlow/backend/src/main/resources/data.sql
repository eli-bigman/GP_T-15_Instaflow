-- Seed admin user (password: password123)
INSERT INTO users (id, email, name, password, role, created_at) VALUES
  (1, 'admin@amalitech.com', 'Admin User', '$2b$12$8.xl7j8FaEukj4Q3JU18heVk/3EpVlWRaW.LBak9sG4oCNuahovcG', 'ADMIN', NOW()),
  (2, 'user@amalitech.com', 'Test User', '$2b$12$8.xl7j8FaEukj4Q3JU18heVk/3EpVlWRaW.LBak9sG4oCNuahovcG', 'USER', NOW())
ON CONFLICT (id) DO NOTHING;

-- Seed sample data sources
INSERT INTO data_sources (id, name, description, type, created_by, created_at) VALUES
  (1, 'Sales CSV', 'Monthly sales data from CSV upload', 'CSV', 1, NOW()),
  (2, 'User Analytics API', 'User behavior data from analytics API', 'API', 1, NOW())
ON CONFLICT (id) DO NOTHING;
