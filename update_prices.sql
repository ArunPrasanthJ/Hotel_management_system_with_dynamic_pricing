-- One-time update to set all room prices above 5000
UPDATE rooms SET 
price = CASE 
    WHEN type = 'SINGLE' THEN 5000 + FLOOR(RAND() * 3000) -- Single rooms: 5000-8000
    WHEN type = 'DOUBLE' THEN 8000 + FLOOR(RAND() * 4000) -- Double rooms: 8000-12000
    WHEN type = 'SUITE' THEN 12000 + FLOOR(RAND() * 5000) -- Suites: 12000-17000
    ELSE 5000 + FLOOR(RAND() * 10000) -- Other types: 5000-15000
END;