-- Initialize Employee Portal Database
-- This script runs when the PostgreSQL container starts for the first time

-- Create database if it doesn't exist (handled by POSTGRES_DB environment variable)
-- CREATE DATABASE IF NOT EXISTS employee_portal;

-- Connect to the database
\c employee_portal;

-- Create extensions if needed
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Set timezone
SET timezone = 'UTC';

-- Create initial admin user (password: admin123)
-- This will be handled by the application's data initialization
-- but we can prepare the database structure here if needed

-- Grant necessary permissions
GRANT ALL PRIVILEGES ON DATABASE employee_portal TO postgres;

-- Log initialization
SELECT 'Employee Portal database initialized successfully' as message;