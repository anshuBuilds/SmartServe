-- Menus belong to a restaurant and are shared by all of its branches. Items belong to a
-- restaurant through their category.
ALTER TABLE menu_categories ADD COLUMN restaurant_id BIGINT;

-- A fresh install has the seeded menu but no restaurant yet; create one to own it.
INSERT INTO restaurants (name, owner_name, active, created_at, updated_at)
SELECT 'SmartServe Restaurant', 'Owner', TRUE, NOW(), NOW()
WHERE EXISTS (SELECT 1 FROM menu_categories)
  AND NOT EXISTS (SELECT 1 FROM restaurants);

-- The menu predates per-restaurant ownership: give it to the first restaurant created.
UPDATE menu_categories
SET restaurant_id = (SELECT MIN(id) FROM restaurants)
WHERE restaurant_id IS NULL;

ALTER TABLE menu_categories
    ALTER COLUMN restaurant_id SET NOT NULL,
    ADD CONSTRAINT fk_menu_categories_restaurant
        FOREIGN KEY (restaurant_id) REFERENCES restaurants(id);

-- Category names were unique across the whole system; now they are unique per restaurant,
-- ignoring case and surrounding spaces. The index also serves restaurant_id lookups.
ALTER TABLE menu_categories DROP CONSTRAINT IF EXISTS menu_categories_name_key;
CREATE UNIQUE INDEX uk_menu_categories_restaurant_name
    ON menu_categories (restaurant_id, LOWER(BTRIM(name)));
