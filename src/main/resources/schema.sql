-- Order Service Database Schema
-- PostgreSQL 15+

-- Create database (run as superuser)
-- CREATE DATABASE orderdb;

-- Connect to orderdb
\c orderdb;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Drop tables if exist (for clean setup)
DROP TABLE IF EXISTS order_status_history CASCADE;
DROP TABLE IF EXISTS order_items CASCADE;
DROP TABLE IF EXISTS orders CASCADE;

-- Create orders table
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_number VARCHAR(50) UNIQUE NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    payment_status VARCHAR(20) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    payment_id VARCHAR(50),
    payment_url VARCHAR(1000),
    shipment_id VARCHAR(50),
    tracking_number VARCHAR(100),
    subtotal DECIMAL(19,2) NOT NULL,
    discount DECIMAL(19,2) DEFAULT 0.00,
    shipping DECIMAL(19,2) NOT NULL,
    tax DECIMAL(19,2) NOT NULL,
    total DECIMAL(19,2) NOT NULL,
    shipping_address JSONB,
    billing_address JSONB,
    customer_name VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255),
    customer_phone VARCHAR(50),
    notes TEXT,
    ordered_at TIMESTAMP,
    confirmed_at TIMESTAMP,
    shipped_at TIMESTAMP,
    delivered_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create order_items table
CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id VARCHAR(255) NOT NULL,
    product_variant_id VARCHAR(255),
    product_name VARCHAR(500) NOT NULL,
    product_image_url VARCHAR(1000),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    price DECIMAL(19,2) NOT NULL CHECK (price >= 0),
    subtotal DECIMAL(19,2) NOT NULL CHECK (subtotal >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create order_status_history table
CREATE TABLE order_status_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    order_id UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    notes TEXT,
    changed_by VARCHAR(255) NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_order_number ON orders(order_number);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_ordered_at ON orders(ordered_at DESC);
CREATE INDEX idx_orders_user_status ON orders(user_id, status);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
CREATE INDEX idx_order_items_product_id ON order_items(product_id);

CREATE INDEX idx_order_status_history_order_id ON order_status_history(order_id);
CREATE INDEX idx_order_status_history_changed_at ON order_status_history(changed_at DESC);

-- Create function to auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger for orders table
CREATE TRIGGER update_orders_updated_at
    BEFORE UPDATE ON orders
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Insert sample data (optional, for testing)
/*
INSERT INTO orders (id, order_number, user_id, status, payment_status, payment_method,
                    subtotal, discount, shipping, tax, total, customer_name, ordered_at)
VALUES
    (uuid_generate_v4(), 'ORD-20260228-0001', 'user-001', 'PENDING', 'PENDING', 'CREDIT_CARD',
     200.00, 0.00, 10.00, 20.00, 230.00, 'Nguyen Van A', CURRENT_TIMESTAMP),
    (uuid_generate_v4(), 'ORD-20260228-0002', 'user-001', 'CONFIRMED', 'PAID', 'PAYPAL',
     350.00, 50.00, 10.00, 30.00, 340.00, 'Nguyen Van A', CURRENT_TIMESTAMP - INTERVAL '1 day');
*/

-- Grant permissions (adjust as needed)
-- GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO order_service_user;
-- GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO order_service_user;

-- Verify tables created
SELECT table_name FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;

-- Show table structures
\d orders
\d order_items
\d order_status_history

