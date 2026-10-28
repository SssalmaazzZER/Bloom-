-- =====================================================================
-- Bloomé — initial relational schema
-- Monetary values: NUMERIC(10,2) (mapped to BigDecimal)
-- Timestamps: TIMESTAMPTZ (stored in UTC, mapped to java.time.Instant)
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ---------------------------------------------------------------------
-- Users & addresses
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(100) NOT NULL,
    first_name      VARCHAR(80)  NOT NULL,
    last_name       VARCHAR(80)  NOT NULL,
    phone           VARCHAR(30),
    role            VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER',
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_users_role CHECK (role IN ('CUSTOMER', 'ADMIN')),
    CONSTRAINT ck_users_email_lower CHECK (email = lower(email))
);
CREATE UNIQUE INDEX ux_users_email ON users (email);

CREATE TABLE addresses (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    label           VARCHAR(60)  NOT NULL,
    recipient_name  VARCHAR(120) NOT NULL,
    phone           VARCHAR(30)  NOT NULL,
    line1           VARCHAR(200) NOT NULL,
    line2           VARCHAR(200),
    city            VARCHAR(80)  NOT NULL,
    postal_code     VARCHAR(12),
    is_default      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_addresses_user ON addresses (user_id);

-- ---------------------------------------------------------------------
-- Catalog
-- ---------------------------------------------------------------------
CREATE TABLE categories (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slug            VARCHAR(80)  NOT NULL UNIQUE,
    name            VARCHAR(120) NOT NULL,
    description     TEXT,
    image_url       VARCHAR(500),
    sort_order      INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE occasions (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slug            VARCHAR(80)  NOT NULL UNIQUE,
    name            VARCHAR(120) NOT NULL,
    description     TEXT,
    image_url       VARCHAR(500),
    sort_order      INT          NOT NULL DEFAULT 0
);

CREATE TABLE products (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    slug                VARCHAR(120)  NOT NULL UNIQUE,
    name                VARCHAR(150)  NOT NULL,
    short_description   VARCHAR(300)  NOT NULL,
    description         TEXT          NOT NULL,
    composition         TEXT,
    care_instructions   TEXT,
    category_id         BIGINT        NOT NULL REFERENCES categories (id) ON DELETE RESTRICT,
    price               NUMERIC(10,2) NOT NULL,
    compare_at_price    NUMERIC(10,2),
    stock_quantity      INT           NOT NULL DEFAULT 0,
    low_stock_threshold INT           NOT NULL DEFAULT 5,
    max_per_order       INT           NOT NULL DEFAULT 10,
    active              BOOLEAN       NOT NULL DEFAULT TRUE,
    featured            BOOLEAN       NOT NULL DEFAULT FALSE,
    bestseller          BOOLEAN       NOT NULL DEFAULT FALSE,
    sales_count         INT           NOT NULL DEFAULT 0,
    version             BIGINT        NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_products_price CHECK (price >= 0),
    CONSTRAINT ck_products_compare_price CHECK (compare_at_price IS NULL OR compare_at_price >= 0),
    CONSTRAINT ck_products_stock CHECK (stock_quantity >= 0),
    CONSTRAINT ck_products_low_stock CHECK (low_stock_threshold >= 0),
    CONSTRAINT ck_products_max_per_order CHECK (max_per_order BETWEEN 1 AND 100),
    CONSTRAINT ck_products_sales CHECK (sales_count >= 0)
);
CREATE INDEX ix_products_category ON products (category_id);
CREATE INDEX ix_products_active_price ON products (active, price);
CREATE INDEX ix_products_created ON products (created_at DESC);
CREATE INDEX ix_products_name_trgm ON products USING gin (name gin_trgm_ops);
CREATE INDEX ix_products_description_trgm ON products USING gin (description gin_trgm_ops);

CREATE TABLE product_images (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id      BIGINT       NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    url             VARCHAR(500) NOT NULL,
    alt_text        VARCHAR(200),
    sort_order      INT          NOT NULL DEFAULT 0
);
CREATE INDEX ix_product_images_product ON product_images (product_id, sort_order);

CREATE TABLE product_colors (
    product_id      BIGINT      NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    color           VARCHAR(30) NOT NULL,
    PRIMARY KEY (product_id, color),
    CONSTRAINT ck_product_colors CHECK (color IN ('RED','PINK','WHITE','YELLOW','ORANGE','PURPLE','BLUE','GREEN','PASTEL','MIXED'))
);
CREATE INDEX ix_product_colors_color ON product_colors (color);

CREATE TABLE product_occasions (
    product_id      BIGINT NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    occasion_id     BIGINT NOT NULL REFERENCES occasions (id) ON DELETE CASCADE,
    PRIMARY KEY (product_id, occasion_id)
);
CREATE INDEX ix_product_occasions_occasion ON product_occasions (occasion_id);

-- ---------------------------------------------------------------------
-- Build-your-bouquet options
-- ---------------------------------------------------------------------
CREATE TABLE bouquet_stems (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name            VARCHAR(120)  NOT NULL,
    color           VARCHAR(30)   NOT NULL,
    description     VARCHAR(300),
    image_url       VARCHAR(500),
    unit_price      NUMERIC(10,2) NOT NULL,
    stock_quantity  INT           NOT NULL DEFAULT 0,
    max_per_bouquet INT           NOT NULL DEFAULT 15,
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    sort_order      INT           NOT NULL DEFAULT 0,
    CONSTRAINT ck_stems_price CHECK (unit_price >= 0),
    CONSTRAINT ck_stems_stock CHECK (stock_quantity >= 0),
    CONSTRAINT ck_stems_max CHECK (max_per_bouquet BETWEEN 1 AND 50)
);

CREATE TABLE bouquet_wrappings (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name            VARCHAR(120)  NOT NULL,
    description     VARCHAR(300),
    swatch_color    VARCHAR(7)    NOT NULL,
    price           NUMERIC(10,2) NOT NULL,
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    sort_order      INT           NOT NULL DEFAULT 0,
    CONSTRAINT ck_wrappings_price CHECK (price >= 0)
);

CREATE TABLE bouquet_ribbons (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name            VARCHAR(120)  NOT NULL,
    swatch_color    VARCHAR(7)    NOT NULL,
    price           NUMERIC(10,2) NOT NULL,
    active          BOOLEAN       NOT NULL DEFAULT TRUE,
    sort_order      INT           NOT NULL DEFAULT 0,
    CONSTRAINT ck_ribbons_price CHECK (price >= 0)
);

CREATE TABLE bouquet_configurations (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT       REFERENCES users (id) ON DELETE SET NULL,
    wrapping_id     BIGINT       NOT NULL REFERENCES bouquet_wrappings (id) ON DELETE RESTRICT,
    ribbon_id       BIGINT       NOT NULL REFERENCES bouquet_ribbons (id) ON DELETE RESTRICT,
    message         VARCHAR(300),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE bouquet_configuration_stems (
    configuration_id BIGINT NOT NULL REFERENCES bouquet_configurations (id) ON DELETE CASCADE,
    stem_id          BIGINT NOT NULL REFERENCES bouquet_stems (id) ON DELETE RESTRICT,
    quantity         INT    NOT NULL,
    PRIMARY KEY (configuration_id, stem_id),
    CONSTRAINT ck_config_stem_qty CHECK (quantity BETWEEN 1 AND 50)
);

-- ---------------------------------------------------------------------
-- Carts & wishlist
-- ---------------------------------------------------------------------
CREATE TABLE carts (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT      NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE cart_items (
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cart_id                  BIGINT      NOT NULL REFERENCES carts (id) ON DELETE CASCADE,
    product_id               BIGINT      REFERENCES products (id) ON DELETE CASCADE,
    bouquet_configuration_id BIGINT      REFERENCES bouquet_configurations (id) ON DELETE CASCADE,
    quantity                 INT         NOT NULL,
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_cart_items_qty CHECK (quantity BETWEEN 1 AND 100),
    CONSTRAINT ck_cart_items_kind CHECK ((product_id IS NULL) <> (bouquet_configuration_id IS NULL))
);
CREATE UNIQUE INDEX ux_cart_items_product ON cart_items (cart_id, product_id) WHERE product_id IS NOT NULL;
CREATE INDEX ix_cart_items_cart ON cart_items (cart_id);

CREATE TABLE wishlist_items (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id         BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    product_id      BIGINT      NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ux_wishlist UNIQUE (user_id, product_id)
);

-- ---------------------------------------------------------------------
-- Orders
-- ---------------------------------------------------------------------
CREATE TABLE customer_orders (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    reference               VARCHAR(20)   NOT NULL UNIQUE,
    user_id                 BIGINT        NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    idempotency_key         VARCHAR(64),
    status                  VARCHAR(20)   NOT NULL,
    payment_method          VARCHAR(30)   NOT NULL,
    payment_status          VARCHAR(20)   NOT NULL,
    customer_name           VARCHAR(160)  NOT NULL,
    customer_email          VARCHAR(255)  NOT NULL,
    customer_phone          VARCHAR(30)   NOT NULL,
    recipient_name          VARCHAR(160)  NOT NULL,
    recipient_phone         VARCHAR(30)   NOT NULL,
    address_line1           VARCHAR(200)  NOT NULL,
    address_line2           VARCHAR(200),
    city                    VARCHAR(80)   NOT NULL,
    postal_code             VARCHAR(12),
    delivery_instructions   VARCHAR(500),
    gift_message            VARCHAR(300),
    gift_wrapping           BOOLEAN       NOT NULL DEFAULT FALSE,
    delivery_date           DATE          NOT NULL,
    delivery_slot           VARCHAR(20)   NOT NULL,
    subtotal                NUMERIC(10,2) NOT NULL,
    delivery_fee            NUMERIC(10,2) NOT NULL,
    gift_wrapping_fee       NUMERIC(10,2) NOT NULL DEFAULT 0,
    discount                NUMERIC(10,2) NOT NULL DEFAULT 0,
    total                   NUMERIC(10,2) NOT NULL,
    version                 BIGINT        NOT NULL DEFAULT 0,
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    cancelled_at            TIMESTAMPTZ,
    CONSTRAINT ck_orders_status CHECK (status IN ('PENDING','CONFIRMED','PREPARING','OUT_FOR_DELIVERY','DELIVERED','CANCELLED')),
    CONSTRAINT ck_orders_payment_method CHECK (payment_method IN ('CASH_ON_DELIVERY')),
    CONSTRAINT ck_orders_payment_status CHECK (payment_status IN ('UNPAID','PAID','REFUNDED','VOID')),
    CONSTRAINT ck_orders_slot CHECK (delivery_slot IN ('MORNING','AFTERNOON','EVENING')),
    CONSTRAINT ck_orders_amounts CHECK (subtotal >= 0 AND delivery_fee >= 0 AND gift_wrapping_fee >= 0 AND discount >= 0 AND total >= 0)
);
CREATE INDEX ix_orders_user_created ON customer_orders (user_id, created_at DESC);
CREATE INDEX ix_orders_status_created ON customer_orders (status, created_at DESC);
CREATE INDEX ix_orders_created ON customer_orders (created_at DESC);
CREATE UNIQUE INDEX ux_orders_idempotency ON customer_orders (user_id, idempotency_key) WHERE idempotency_key IS NOT NULL;

CREATE TABLE order_items (
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id                 BIGINT        NOT NULL REFERENCES customer_orders (id) ON DELETE CASCADE,
    item_type                VARCHAR(20)   NOT NULL,
    product_id               BIGINT        REFERENCES products (id) ON DELETE SET NULL,
    bouquet_configuration_id BIGINT        REFERENCES bouquet_configurations (id) ON DELETE SET NULL,
    product_name             VARCHAR(150)  NOT NULL,
    image_url                VARCHAR(500),
    unit_price               NUMERIC(10,2) NOT NULL,
    quantity                 INT           NOT NULL,
    line_total               NUMERIC(10,2) NOT NULL,
    configuration_snapshot   JSONB,
    CONSTRAINT ck_order_items_type CHECK (item_type IN ('PRODUCT','CUSTOM_BOUQUET')),
    CONSTRAINT ck_order_items_qty CHECK (quantity > 0),
    CONSTRAINT ck_order_items_amounts CHECK (unit_price >= 0 AND line_total >= 0)
);
CREATE INDEX ix_order_items_order ON order_items (order_id);
CREATE INDEX ix_order_items_product ON order_items (product_id);

CREATE TABLE order_status_history (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id        BIGINT       NOT NULL REFERENCES customer_orders (id) ON DELETE CASCADE,
    from_status     VARCHAR(20),
    to_status       VARCHAR(20)  NOT NULL,
    changed_by      BIGINT       REFERENCES users (id) ON DELETE SET NULL,
    note            VARCHAR(300),
    changed_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_order_history_order ON order_status_history (order_id, changed_at);

-- ---------------------------------------------------------------------
-- Reviews & newsletter
-- ---------------------------------------------------------------------
CREATE TABLE reviews (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    product_id      BIGINT       NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    user_id         BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    rating          SMALLINT     NOT NULL,
    title           VARCHAR(120),
    comment         VARCHAR(2000) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_reviews_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT ux_reviews_user_product UNIQUE (product_id, user_id)
);
CREATE INDEX ix_reviews_product ON reviews (product_id, created_at DESC);

CREATE TABLE newsletter_subscriptions (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_newsletter_email_lower CHECK (email = lower(email))
);
