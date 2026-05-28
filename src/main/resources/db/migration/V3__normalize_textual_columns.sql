DO $$
DECLARE
    rec RECORD;
    current_type TEXT;
    using_expr TEXT;
BEGIN
    FOR rec IN
        SELECT *
        FROM (VALUES
            ('users', 'email', 'VARCHAR(255)'),
            ('users', 'password_hash', 'VARCHAR(255)'),
            ('users', 'display_name', 'VARCHAR(100)'),
            ('users', 'currency', 'VARCHAR(3)'),
            ('users', 'role', 'VARCHAR(20)'),
            ('accounts', 'name', 'VARCHAR(100)'),
            ('accounts', 'currency', 'VARCHAR(3)'),
            ('accounts', 'note', 'TEXT'),
            ('categories', 'name', 'VARCHAR(100)'),
            ('categories', 'icon', 'VARCHAR(100)'),
            ('categories', 'color', 'VARCHAR(7)'),
            ('transactions', 'currency', 'VARCHAR(3)'),
            ('transactions', 'note', 'TEXT'),
            ('attachments', 'file_name', 'VARCHAR(255)'),
            ('attachments', 'file_path', 'VARCHAR(500)'),
            ('attachments', 'mime_type', 'VARCHAR(100)'),
            ('refresh_tokens', 'token_hash', 'VARCHAR(64)')
        ) AS cols(table_name, column_name, target_type)
    LOOP
        SELECT c.udt_name
        INTO current_type
        FROM information_schema.columns c
        WHERE c.table_schema = 'public'
          AND c.table_name = rec.table_name
          AND c.column_name = rec.column_name;

        IF current_type IS NULL THEN
            CONTINUE;
        END IF;

        IF current_type = 'bytea' THEN
            using_expr := format('convert_from(%I, ''UTF8'')', rec.column_name);
        ELSIF current_type = 'bpchar' THEN
            using_expr := format('TRIM(%I)', rec.column_name);
        ELSE
            using_expr := format('%I::text', rec.column_name);
        END IF;

        EXECUTE format(
                'ALTER TABLE %I ALTER COLUMN %I TYPE %s USING %s',
                rec.table_name,
                rec.column_name,
                rec.target_type,
                using_expr
        );
    END LOOP;
END $$;
