-- Очистка таблиц
TRUNCATE TABLE tickets, seats, halls RESTART IDENTITY CASCADE;

-- Создание зала
INSERT INTO halls (name, rows, seats_per_row)
VALUES ('Главный зал', 10, 20);

-- Создание 200 мест (10 рядов × 20 мест)
DO $$
DECLARE
hall_id BIGINT;
    row_num INT;
    seat_num INT;
BEGIN
SELECT id INTO hall_id FROM halls WHERE name = 'Главный зал' LIMIT 1;

FOR row_num IN 1..10 LOOP
        FOR seat_num IN 1..20 LOOP
            INSERT INTO seats (row_num, seat_num, status, hall_id)
            VALUES (row_num, seat_num, 'FREE', hall_id);
END LOOP;
END LOOP;
END $$;