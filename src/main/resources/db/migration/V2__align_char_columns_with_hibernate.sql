ALTER TABLE users
    ALTER COLUMN currency TYPE VARCHAR(3) USING TRIM(currency);

ALTER TABLE accounts
    ALTER COLUMN currency TYPE VARCHAR(3) USING TRIM(currency);

ALTER TABLE transactions
    ALTER COLUMN currency TYPE VARCHAR(3) USING TRIM(currency);

ALTER TABLE categories
    ALTER COLUMN color TYPE VARCHAR(7) USING TRIM(color);
