-- Existing duplicates are intentionally left untouched. This trigger serializes creation
-- of the same normalized name and rejects future duplicates, including concurrent requests.
CREATE OR REPLACE FUNCTION prevent_duplicate_menu_item()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    normalized_name TEXT := LOWER(BTRIM(NEW.name));
BEGIN
    -- Permit ordinary edits to legacy duplicate rows while their identity stays unchanged.
    IF TG_OP = 'UPDATE'
       AND OLD.category_id = NEW.category_id
       AND LOWER(BTRIM(OLD.name)) = normalized_name THEN
        RETURN NEW;
    END IF;

    PERFORM pg_advisory_xact_lock(
        hashtextextended(NEW.category_id::TEXT || ':' || normalized_name, 0)
    );

    IF EXISTS (
        SELECT 1
        FROM menu_items existing
        WHERE existing.category_id = NEW.category_id
          AND LOWER(BTRIM(existing.name)) = normalized_name
          AND existing.id IS DISTINCT FROM NEW.id
    ) THEN
        RAISE EXCEPTION 'A menu item with this name already exists in the category'
            USING ERRCODE = '23505',
                  CONSTRAINT = 'uq_menu_items_category_normalized_name';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_prevent_duplicate_menu_item
BEFORE INSERT OR UPDATE OF name, category_id ON menu_items
FOR EACH ROW
EXECUTE FUNCTION prevent_duplicate_menu_item();
