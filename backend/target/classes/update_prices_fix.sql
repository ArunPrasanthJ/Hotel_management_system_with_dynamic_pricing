-- Fix script to set sensible base_price and current_price for existing rooms

UPDATE rooms
SET
  base_price = CASE
    WHEN type = 'SINGLE' THEN 6500
    WHEN type = 'DOUBLE' THEN 9500
    WHEN type = 'SUITE' THEN 15000
    ELSE 7500
  END,
  current_price = CASE
    WHEN type = 'SINGLE' THEN 6500
    WHEN type = 'DOUBLE' THEN 9500
    WHEN type = 'SUITE' THEN 15000
    ELSE 7500
  END
WHERE base_price IS NULL OR base_price = 0 OR current_price IS NULL OR current_price = 0;

-- Ensure legacy `price` column is populated when missing or very low
UPDATE rooms
SET price = COALESCE(price, base_price)
WHERE price IS NULL OR price < 5000;

-- Optional: show affected rows (works in MySQL client)
SELECT id, room_number, type, base_price, current_price, price FROM rooms ORDER BY id LIMIT 50;