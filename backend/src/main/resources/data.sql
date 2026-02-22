INSERT INTO rooms (room_number, type, price, base_price, current_price, available, status, description) VALUES
('101', 'SINGLE', 6500, 6500, 6500, true, 'AVAILABLE', 'Cozy single room with a city view'),
('102', 'DOUBLE', 9500, 9500, 9500, true, 'AVAILABLE', 'Spacious double room with two queen beds'),
('201', 'SUITE', 15000, 15000, 15000, true, 'AVAILABLE', 'Luxury suite with separate living area and kitchenette'),
('202', 'SINGLE', 7000, 7000, 7000, true, 'AVAILABLE', 'Modern single room with garden view'),
('301', 'DOUBLE', 10500, 10500, 10500, true, 'AVAILABLE', 'Premium double room with mountain view and balcony')
ON DUPLICATE KEY UPDATE 
room_number = VALUES(room_number), price = VALUES(price), base_price = VALUES(base_price), current_price = VALUES(current_price), available = VALUES(available), status = VALUES(status), description = VALUES(description);