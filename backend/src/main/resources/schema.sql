UPDATE rooms SET 
price = CASE 
    WHEN type = 'SINGLE' THEN 6500 
    WHEN type = 'DOUBLE' THEN 9500
    WHEN type = 'SUITE' THEN 15000
    ELSE 7500
END
WHERE price < 5000 OR price IS NULL;
-- schema.sql intentionally left minimal.
-- Hibernate (spring.jpa.hibernate.ddl-auto=update) will manage schema alterations.
-- If you need to run manual SQL to adjust schema, run it separately.