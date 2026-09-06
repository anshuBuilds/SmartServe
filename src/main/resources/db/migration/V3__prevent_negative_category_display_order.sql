-- Existing invalid values remain visible for deliberate cleanup, but all new writes
-- must use zero or a positive display order.
ALTER TABLE menu_categories
    ADD CONSTRAINT chk_menu_categories_display_order_non_negative
    CHECK (display_order >= 0)
    NOT VALID;
