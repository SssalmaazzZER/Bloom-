-- Catalogue search filters on lower(name) / lower(description) (case-insensitive LIKE '%term%').
-- Trigram indexes must be built on the same expressions to be usable by the planner.
DROP INDEX IF EXISTS ix_products_name_trgm;
DROP INDEX IF EXISTS ix_products_description_trgm;
CREATE INDEX ix_products_lower_name_trgm ON products USING gin (lower(name) gin_trgm_ops);
CREATE INDEX ix_products_lower_description_trgm ON products USING gin (lower(description) gin_trgm_ops);

-- Foreign keys used in joins, ownership checks and ON DELETE actions.
CREATE INDEX IF NOT EXISTS ix_cart_items_product ON cart_items (product_id);
CREATE INDEX IF NOT EXISTS ix_cart_items_bouquet ON cart_items (bouquet_configuration_id);
CREATE INDEX IF NOT EXISTS ix_wishlist_product ON wishlist_items (product_id);
CREATE INDEX IF NOT EXISTS ix_reviews_user ON reviews (user_id);
CREATE INDEX IF NOT EXISTS ix_order_items_bouquet ON order_items (bouquet_configuration_id);
CREATE INDEX IF NOT EXISTS ix_bouquet_configurations_user ON bouquet_configurations (user_id);
CREATE INDEX IF NOT EXISTS ix_bouquet_configuration_stems_stem ON bouquet_configuration_stems (stem_id);
CREATE INDEX IF NOT EXISTS ix_order_history_changed_by ON order_status_history (changed_by);
