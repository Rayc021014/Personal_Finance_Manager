DO $$
DECLARE
    rec RECORD;
    current_type TEXT;
    using_expr TEXT;
BEGIN
    FOR rec IN
        SELECT *
        FROM (VALUES
            ('accounts', 'note'),
            ('transactions', 'note')
        ) AS cols(table_name, column_name)
    LOOP
        SELECT c.udt_name
        INTO current_type
        FROM information_schema.columns c
        WHERE c.table_schema = 'public'
          AND c.table_name = rec.table_name
          AND c.column_name = rec.column_name;

        IF current_type IS NULL OR current_type = 'text' THEN
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
            'ALTER TABLE %I ALTER COLUMN %I TYPE TEXT USING %s',
            rec.table_name,
            rec.column_name,
            using_expr
        );
    END LOOP;
END $$;
