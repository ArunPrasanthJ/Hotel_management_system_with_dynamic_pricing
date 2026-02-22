-- Update existing rooms with random prices between 5000 and 15000
-- If base_price/current_price columns exist, update current_price
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS base_price DOUBLE;
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS current_price DOUBLE;
ALTER TABLE rooms ADD COLUMN IF NOT EXISTS status VARCHAR(32) DEFAULT 'AVAILABLE';

UPDATE rooms 
SET current_price = FLOOR(5000 + (RAND() * (15000 - 5000))) / 100 * 100
WHERE (current_price IS NULL OR current_price < 5000);