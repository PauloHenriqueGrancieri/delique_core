-- ============================================================
-- V1 — Complete initial schema for delique_core
-- Consolidated from all previous incremental migrations.
-- ============================================================

-- ────────────────────────────────────────────────────────────
-- CATEGORIES
-- ────────────────────────────────────────────────────────────
CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    has_validity BOOLEAN NOT NULL DEFAULT false,
    display_id  INTEGER
);

INSERT INTO categories (name, has_validity, display_id) VALUES ('Brinde', false, 1);
INSERT INTO categories (name, has_validity, display_id) VALUES ('Outros', false, 2);
INSERT INTO categories (name, has_validity, display_id) VALUES ('Combos', false, 3);

-- ────────────────────────────────────────────────────────────
-- BRANDS
-- ────────────────────────────────────────────────────────────
CREATE TABLE brands (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL UNIQUE,
    display_id INTEGER
);

CREATE INDEX idx_brands_display_id ON brands(display_id);

INSERT INTO brands (name, display_id) VALUES ('Sem Marca', 1);
INSERT INTO brands (name, display_id) VALUES ('Combos',    2);

-- ────────────────────────────────────────────────────────────
-- SUPPLIERS
-- ────────────────────────────────────────────────────────────
CREATE TABLE suppliers (
    id                       BIGSERIAL PRIMARY KEY,
    name                     VARCHAR(255) NOT NULL,
    website                  VARCHAR(255),
    freight                  NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    min_free_freight         NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    min_order_value          NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    scraper_excluded         BOOLEAN NOT NULL DEFAULT false,
    scraper_success_selectors TEXT
);

CREATE TABLE supplier_emails (
    supplier_id BIGINT NOT NULL REFERENCES suppliers(id) ON DELETE CASCADE,
    email       VARCHAR(255) NOT NULL,
    PRIMARY KEY (supplier_id, email)
);

CREATE TABLE supplier_phones (
    supplier_id BIGINT NOT NULL REFERENCES suppliers(id) ON DELETE CASCADE,
    phone       VARCHAR(255) NOT NULL,
    PRIMARY KEY (supplier_id, phone)
);

-- ────────────────────────────────────────────────────────────
-- PRODUCTS
-- ────────────────────────────────────────────────────────────
CREATE TABLE products (
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(255) NOT NULL,
    sku               VARCHAR(100),
    description       TEXT,
    category_id       BIGINT NOT NULL REFERENCES categories(id) ON DELETE RESTRICT,
    brand_id          BIGINT NOT NULL REFERENCES brands(id) ON DELETE RESTRICT,
    image_url         TEXT,
    image_data        BYTEA,
    image_media_type  VARCHAR(100),
    variation_type    VARCHAR(100),
    display_id        INTEGER,
    minimum_stock     INTEGER
);

CREATE INDEX idx_products_category  ON products(category_id);
CREATE INDEX idx_products_brand     ON products(brand_id);
CREATE INDEX idx_products_display_id ON products(display_id);

-- ────────────────────────────────────────────────────────────
-- PRODUCT SUPPLIERS
-- ────────────────────────────────────────────────────────────
CREATE TABLE product_suppliers (
    id                       BIGSERIAL PRIMARY KEY,
    product_id               BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    supplier_id              BIGINT NOT NULL REFERENCES suppliers(id) ON DELETE CASCADE,
    url                      TEXT,
    price                    NUMERIC(19, 2),
    out_of_stock_at_supplier BOOLEAN NOT NULL DEFAULT false,
    UNIQUE(product_id, supplier_id)
);

CREATE INDEX idx_product_suppliers_product  ON product_suppliers(product_id);
CREATE INDEX idx_product_suppliers_supplier ON product_suppliers(supplier_id);

-- ────────────────────────────────────────────────────────────
-- PRODUCT VARIATION OPTIONS
-- ────────────────────────────────────────────────────────────
CREATE TABLE product_variation_options (
    id               BIGSERIAL PRIMARY KEY,
    product_id       BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    name             VARCHAR(255) NOT NULL,
    sku              VARCHAR(100),
    image_url        TEXT,
    image_data       BYTEA,
    image_media_type VARCHAR(100)
);

CREATE INDEX idx_product_variation_options_product ON product_variation_options(product_id);

-- ────────────────────────────────────────────────────────────
-- PRICE CALCULATION CONFIG
-- ────────────────────────────────────────────────────────────
CREATE TABLE price_calculation_config (
    id                                BIGSERIAL PRIMARY KEY,
    default_cmv                       NUMERIC(19, 2) DEFAULT 0.00,
    default_loss_percentage           NUMERIC(5, 2)  DEFAULT 0.00,
    default_sales_commission_percentage NUMERIC(5, 2) DEFAULT 0.00,
    default_card_fee_percentage       NUMERIC(5, 2)  DEFAULT 0.00,
    default_tax_percentage            NUMERIC(5, 2)  DEFAULT 0.00,
    default_packaging_value           NUMERIC(19, 2) DEFAULT 0.00,
    default_delivery_value            NUMERIC(19, 2) DEFAULT 0.00,
    default_average_items_per_order   NUMERIC(10, 2) DEFAULT 1.00,
    default_fixed_expense_percentage  NUMERIC(5, 2)  DEFAULT 20.00,
    default_profit_margin_percentage  NUMERIC(5, 2)  DEFAULT 25.00
);

-- ────────────────────────────────────────────────────────────
-- PAYMENT METHOD CONFIG
-- ────────────────────────────────────────────────────────────
CREATE TABLE payment_method_config (
    id                  BIGSERIAL PRIMARY KEY,
    payment_method      VARCHAR(50) NOT NULL UNIQUE,
    discount_percentage NUMERIC(5, 2),
    fee_percentage      NUMERIC(5, 2)
);

INSERT INTO payment_method_config (payment_method, discount_percentage, fee_percentage) VALUES ('MONEY',       0, NULL);
INSERT INTO payment_method_config (payment_method, discount_percentage, fee_percentage) VALUES ('PIX',         0, NULL);
INSERT INTO payment_method_config (payment_method, discount_percentage, fee_percentage) VALUES ('CREDIT_CARD', 0, NULL);
INSERT INTO payment_method_config (payment_method, discount_percentage, fee_percentage) VALUES ('DEBIT_CARD',  0, NULL);

-- ────────────────────────────────────────────────────────────
-- CREDIT CARD INSTALLMENT FEES
-- ────────────────────────────────────────────────────────────
CREATE TABLE credit_card_installment_fee (
    id             BIGSERIAL PRIMARY KEY,
    installments   INT NOT NULL UNIQUE,
    fee_percentage NUMERIC(5, 2) NOT NULL DEFAULT 0
);

INSERT INTO credit_card_installment_fee (installments, fee_percentage)
SELECT i, 0 FROM generate_series(1, 12) AS i;

-- ────────────────────────────────────────────────────────────
-- MARKETING CAMPAIGNS  (before orders due to FK)
-- ────────────────────────────────────────────────────────────
CREATE TABLE marketing_campaign (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    channel     VARCHAR(100) NOT NULL,
    investment  NUMERIC(19, 2) NOT NULL,
    start_date  DATE NOT NULL,
    end_date    DATE NOT NULL,
    description TEXT,
    open_rate   NUMERIC(5, 2),
    click_rate  NUMERIC(5, 2),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ────────────────────────────────────────────────────────────
-- PURCHASE ORDERS
-- ────────────────────────────────────────────────────────────
CREATE TABLE purchase_orders (
    id            BIGSERIAL PRIMARY KEY,
    total_freight NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    supplier_id   BIGINT REFERENCES suppliers(id) ON DELETE SET NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    delivered_at  TIMESTAMP,
    cancelled_at  TIMESTAMP
);

CREATE INDEX idx_purchase_orders_supplier ON purchase_orders(supplier_id);
CREATE INDEX idx_purchase_orders_status   ON purchase_orders(status);

-- ────────────────────────────────────────────────────────────
-- STOCK MOVEMENTS
-- ────────────────────────────────────────────────────────────
CREATE TABLE stock_movements (
    id                 BIGSERIAL PRIMARY KEY,
    product_id         BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    variation_option_id BIGINT REFERENCES product_variation_options(id) ON DELETE SET NULL,
    quantity           INTEGER NOT NULL,
    type               VARCHAR(20) NOT NULL,
    details            TEXT,
    purchase_price     NUMERIC(19, 2),
    sale_id            BIGINT,
    edited_at          TIMESTAMP,
    edit_reason        TEXT,
    created_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at         DATE
);

CREATE INDEX idx_stock_movements_product    ON stock_movements(product_id);
CREATE INDEX idx_stock_movements_type       ON stock_movements(type);
CREATE INDEX idx_stock_movements_created_at ON stock_movements(created_at DESC);
CREATE INDEX idx_stock_movements_variation  ON stock_movements(variation_option_id);

-- ────────────────────────────────────────────────────────────
-- STOCK UNIT EXPIRY
-- ────────────────────────────────────────────────────────────
CREATE TABLE stock_unit_expiry (
    stock_movement_id BIGINT  NOT NULL REFERENCES stock_movements(id) ON DELETE CASCADE,
    unit_index        INTEGER NOT NULL,
    expires_at        DATE    NOT NULL,
    PRIMARY KEY (stock_movement_id, unit_index)
);

CREATE INDEX idx_stock_unit_expiry_movement ON stock_unit_expiry(stock_movement_id);

-- ────────────────────────────────────────────────────────────
-- STOCK UNIT  (UUID per physical unit)
-- ────────────────────────────────────────────────────────────
CREATE TABLE stock_unit (
    id                VARCHAR(36) NOT NULL PRIMARY KEY,
    stock_movement_id BIGINT NOT NULL REFERENCES stock_movements(id) ON DELETE CASCADE,
    unit_index        INTEGER NOT NULL,
    UNIQUE(stock_movement_id, unit_index)
);

CREATE INDEX idx_stock_unit_movement ON stock_unit(stock_movement_id);

-- ────────────────────────────────────────────────────────────
-- PURCHASE ORDER ITEMS
-- ────────────────────────────────────────────────────────────
CREATE TABLE purchase_order_items (
    id                  BIGSERIAL PRIMARY KEY,
    purchase_order_id   BIGINT NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    product_id          BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    variation_option_id BIGINT REFERENCES product_variation_options(id) ON DELETE SET NULL,
    quantity            INTEGER NOT NULL,
    received_quantity   INTEGER,
    unit_cost           NUMERIC(19, 2) NOT NULL,
    expires_at          DATE
);

CREATE INDEX idx_purchase_order_items_order      ON purchase_order_items(purchase_order_id);
CREATE INDEX idx_purchase_order_items_product    ON purchase_order_items(product_id);
CREATE INDEX idx_purchase_order_items_variation  ON purchase_order_items(variation_option_id);

-- ────────────────────────────────────────────────────────────
-- CLIENTS
-- ────────────────────────────────────────────────────────────
CREATE TABLE clients (
    id        BIGSERIAL PRIMARY KEY,
    name      VARCHAR(255) NOT NULL,
    phone     VARCHAR(255) NOT NULL,
    age       INTEGER,
    location  VARCHAR(255),
    canal     VARCHAR(100),
    interests TEXT
);

-- ────────────────────────────────────────────────────────────
-- ORDERS
-- ────────────────────────────────────────────────────────────
CREATE TABLE orders (
    id                   BIGSERIAL PRIMARY KEY,
    payment_method       VARCHAR(255) NOT NULL,
    client_id            BIGINT REFERENCES clients(id) ON DELETE SET NULL,
    campaign_id          BIGINT REFERENCES marketing_campaign(id) ON DELETE SET NULL,
    order_discount_value NUMERIC(19, 2),
    fee_percentage       NUMERIC(5, 2),
    fee_value            NUMERIC(19, 2),
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_orders_client     ON orders(client_id);
CREATE INDEX idx_orders_campaign   ON orders(campaign_id);
CREATE INDEX idx_orders_created_at ON orders(created_at DESC);

-- ────────────────────────────────────────────────────────────
-- SALES
-- ────────────────────────────────────────────────────────────
CREATE TABLE sales (
    id                  BIGSERIAL PRIMARY KEY,
    product_id          BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    variation_option_id BIGINT REFERENCES product_variation_options(id) ON DELETE SET NULL,
    order_id            BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    quantity            INTEGER NOT NULL,
    unit_price          NUMERIC(19, 2) NOT NULL,
    discount            NUMERIC(19, 2),
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sales_product    ON sales(product_id);
CREATE INDEX idx_sales_order      ON sales(order_id);
CREATE INDEX idx_sales_variation  ON sales(variation_option_id);
CREATE INDEX idx_sales_created_at ON sales(created_at DESC);

-- ────────────────────────────────────────────────────────────
-- SALE RETURNS
-- ────────────────────────────────────────────────────────────
CREATE TABLE sale_returns (
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT NOT NULL REFERENCES orders(id),
    returned_at  TIMESTAMP NOT NULL,
    reason       TEXT,
    created_at   TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE sale_return_items (
    id              BIGSERIAL PRIMARY KEY,
    sale_return_id  BIGINT NOT NULL REFERENCES sale_returns(id) ON DELETE CASCADE,
    product_id      BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    quantity        INTEGER NOT NULL,
    unit_price      NUMERIC(19, 2) NOT NULL,
    variation_option VARCHAR(255)
);

CREATE INDEX idx_sale_returns_order ON sale_returns(order_id);
CREATE INDEX idx_sale_return_items_return   ON sale_return_items(sale_return_id);
CREATE INDEX idx_sale_return_items_product  ON sale_return_items(product_id);

-- ────────────────────────────────────────────────────────────
-- CATALOG
-- ────────────────────────────────────────────────────────────
CREATE TABLE catalog (
    id                   BIGSERIAL PRIMARY KEY,
    product_id           BIGINT NOT NULL UNIQUE REFERENCES products(id) ON DELETE CASCADE,
    cost_price           NUMERIC(19, 2),
    sale_price           NUMERIC(19, 2) NOT NULL,
    discount_percentage  NUMERIC(5, 2),
    final_price          NUMERIC(19, 2) NOT NULL,
    in_catalog           BOOLEAN NOT NULL DEFAULT true,
    carousel_position    INTEGER,
    carousel_description TEXT,
    carousel_show_price  BOOLEAN DEFAULT true
);

CREATE INDEX idx_catalog_product ON catalog(product_id);

-- ────────────────────────────────────────────────────────────
-- CATALOG PRICES BY PAYMENT METHOD
-- ────────────────────────────────────────────────────────────
CREATE TABLE catalog_price_by_payment_method (
    id             BIGSERIAL PRIMARY KEY,
    product_id     BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    payment_method VARCHAR(50) NOT NULL,
    installments   INTEGER,
    base_price     NUMERIC(19, 2) NOT NULL,
    final_price    NUMERIC(19, 2) NOT NULL,
    CONSTRAINT uq_catalog_price_product_method_installments
        UNIQUE (product_id, payment_method, installments)
);

CREATE INDEX idx_catalog_price_by_payment_method_product ON catalog_price_by_payment_method(product_id);

-- ────────────────────────────────────────────────────────────
-- CATALOG SETTINGS
-- ────────────────────────────────────────────────────────────
CREATE TABLE catalog_settings (
    id               BIGSERIAL PRIMARY KEY,
    whatsapp_number  VARCHAR(30),
    instagram_handle VARCHAR(100),
    address          TEXT,
    logo_url         TEXT,
    catalog_title    VARCHAR(200),
    primary_color    VARCHAR(20),
    about_text       TEXT,
    show_prices      BOOLEAN NOT NULL DEFAULT true,
    show_cart        BOOLEAN NOT NULL DEFAULT true
);

-- ────────────────────────────────────────────────────────────
-- CASH
-- ────────────────────────────────────────────────────────────
CREATE TABLE cash (
    id                 BIGSERIAL PRIMARY KEY,
    period_type        VARCHAR(20) NOT NULL,
    start_date         DATE NOT NULL UNIQUE,
    end_date           DATE NOT NULL,
    initial_investment NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    closing_balance    NUMERIC(19, 2)
);

CREATE INDEX idx_cash_period_type ON cash(period_type);
CREATE INDEX idx_cash_start_date  ON cash(start_date);

-- ────────────────────────────────────────────────────────────
-- EXPENSES
-- ────────────────────────────────────────────────────────────
CREATE TABLE expenses (
    id           BIGSERIAL PRIMARY KEY,
    name         VARCHAR(255) NOT NULL,
    amount       NUMERIC(19, 2) NOT NULL,
    due_date     DATE NOT NULL,
    paid_at      TIMESTAMP,
    is_recurring BOOLEAN NOT NULL DEFAULT false,
    recurrence   VARCHAR(20)
);

CREATE INDEX idx_expenses_due_date ON expenses(due_date);
CREATE INDEX idx_expenses_paid_at  ON expenses(paid_at);

-- ────────────────────────────────────────────────────────────
-- CASH INFLOWS
-- ────────────────────────────────────────────────────────────
CREATE TABLE cash_inflows (
    id          BIGSERIAL PRIMARY KEY,
    amount      NUMERIC(19, 2) NOT NULL,
    date        DATE NOT NULL,
    description VARCHAR(500),
    type        VARCHAR(20) NOT NULL
);

CREATE INDEX idx_cash_inflows_date ON cash_inflows(date);

-- ────────────────────────────────────────────────────────────
-- PRODUCT METRICS
-- ────────────────────────────────────────────────────────────
CREATE TABLE product_metrics (
    id                    BIGSERIAL PRIMARY KEY,
    product_id            BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    period_start          DATE NOT NULL,
    period_end            DATE NOT NULL,
    period_type           VARCHAR(20) NOT NULL,
    preco_venda           NUMERIC(19, 2) DEFAULT 0.00,
    cmv_aj                NUMERIC(19, 2) DEFAULT 0.00,
    margem_lucro_percent  NUMERIC(5, 2)  DEFAULT 0.00,
    lucro_unitario        NUMERIC(19, 2) DEFAULT 0.00,
    quantidade_vendida    INTEGER NOT NULL DEFAULT 0,
    numero_pedidos        INTEGER NOT NULL DEFAULT 0,
    dias_com_estoque      INTEGER NOT NULL DEFAULT 0,
    estoque_medio         NUMERIC(10, 2) DEFAULT 0.00,
    faturamento_total     NUMERIC(19, 2) DEFAULT 0.00,
    margem_total          NUMERIC(19, 2) DEFAULT 0.00,
    created_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(product_id, period_type)
);

CREATE INDEX idx_product_metrics_product     ON product_metrics(product_id);
CREATE INDEX idx_product_metrics_period_type ON product_metrics(period_type);
CREATE INDEX idx_product_metrics_updated_at  ON product_metrics(updated_at DESC);

-- ────────────────────────────────────────────────────────────
-- PRODUCT CLASSIFICATIONS
-- ────────────────────────────────────────────────────────────
CREATE TABLE product_classifications (
    id               BIGSERIAL PRIMARY KEY,
    product_id       BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    period_start     DATE NOT NULL,
    period_end       DATE NOT NULL,
    period_type      VARCHAR(20) NOT NULL,
    abc_faturamento  VARCHAR(1),
    abc_margem       VARCHAR(1),
    xyz_giro         VARCHAR(1),
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(product_id, period_type)
);

CREATE INDEX idx_product_classifications_product     ON product_classifications(product_id);
CREATE INDEX idx_product_classifications_period_type ON product_classifications(period_type);
CREATE INDEX idx_product_classifications_updated_at  ON product_classifications(updated_at DESC);

-- ────────────────────────────────────────────────────────────
-- MARGIN STRATEGIES
-- ────────────────────────────────────────────────────────────
CREATE TABLE margin_strategies (
    id                          BIGSERIAL PRIMARY KEY,
    abc_faturamento             VARCHAR(1),
    abc_margem                  VARCHAR(1),
    xyz_giro                    VARCHAR(1),
    suggested_margin_percentage NUMERIC(5, 2) NOT NULL,
    description                 TEXT,
    is_active                   BOOLEAN NOT NULL DEFAULT true,
    sort_order                  INTEGER,
    created_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_margin_strategies_active         ON margin_strategies(is_active);
CREATE INDEX idx_margin_strategies_classification ON margin_strategies(abc_faturamento, abc_margem, xyz_giro);

INSERT INTO margin_strategies (abc_faturamento, abc_margem, xyz_giro, suggested_margin_percentage, description, is_active, sort_order, created_at, updated_at)
VALUES
  ('A','A','X', 30.00, 'Campeão absoluto. Alta receita, alta margem e giro semanal. Produto estrela.', true,  1, NOW(), NOW()),
  ('A','A','Y', 32.00, 'Forte, mas irregular. Alta rentabilidade com oscilação.',                       true,  2, NOW(), NOW()),
  ('A','A','Z', 35.00, 'Premium de giro baixo. Ticket alto e compra ocasional.',                        true,  3, NOW(), NOW()),
  ('A','B','X', 32.00, 'Volume competitivo. Vende muito, margem média. Sensível a preço.',              true,  4, NOW(), NOW()),
  ('A','B','Y', 35.00, 'Alto faturamento com oscilação de demanda.',                                    true,  5, NOW(), NOW()),
  ('A','B','Z', 38.00, 'Fatura bem em picos, mas giro baixo.',                                          true,  6, NOW(), NOW()),
  ('A','C','X', 38.00, 'Sustenta caixa, mas margem fraca. Precisa recuperar rentabilidade.',            true,  7, NOW(), NOW()),
  ('A','C','Y', 42.00, 'Alto faturamento pouco eficiente. Revisão recomendada.',                        true,  8, NOW(), NOW()),
  ('A','C','Z', 45.00, 'Faturou bem, mas hoje é ineficiente. Ajuste urgente.',                          true,  9, NOW(), NOW()),
  ('B','A','X', 35.00, 'Máquina de lucro recorrente. Ótima margem e giro constante.',                  true, 10, NOW(), NOW()),
  ('B','A','Y', 38.00, 'Boa rentabilidade, mas vendas irregulares.',                                    true, 11, NOW(), NOW()),
  ('B','A','Z', 42.00, 'Alta margem, porém capital parado.',                                            true, 12, NOW(), NOW()),
  ('B','B','X', 38.00, 'Produto equilibrado e saudável.',                                               true, 13, NOW(), NOW()),
  ('B','B','Y', 42.00, 'Intermediário instável.',                                                       true, 14, NOW(), NOW()),
  ('B','B','Z', 45.00, 'Médio sem destaque e baixo giro.',                                              true, 15, NOW(), NOW()),
  ('B','C','X', 42.00, 'Vende bem, mas lucro fraco. Pode servir como produto de entrada.',              true, 16, NOW(), NOW()),
  ('B','C','Y', 45.00, 'Margem ruim e venda irregular.',                                                true, 17, NOW(), NOW()),
  ('B','C','Z', 50.00, 'Baixa eficiência geral. Avaliar permanência.',                                  true, 18, NOW(), NOW()),
  ('C','A','X', 40.00, 'Joia escondida. Gira bem e tem ótima margem.',                                  true, 19, NOW(), NOW()),
  ('C','A','Y', 45.00, 'Alta margem, venda ocasional.',                                                 true, 20, NOW(), NOW()),
  ('C','A','Z', 50.00, 'Produto premium nichado.',                                                      true, 21, NOW(), NOW()),
  ('C','B','X', 45.00, 'Pequeno mas constante. Pode sustentar lucro complementar.',                     true, 22, NOW(), NOW()),
  ('C','B','Y', 50.00, 'Baixo impacto e venda irregular.',                                              true, 23, NOW(), NOW()),
  ('C','B','Z', 55.00, 'Produto secundário com pouca relevância.',                                      true, 24, NOW(), NOW()),
  ('C','C','X', 50.00, 'Vende, mas quase não lucra. Ajustar preço.',                                    true, 25, NOW(), NOW()),
  ('C','C','Y', 55.00, 'Fraco em todos os indicadores.',                                                true, 26, NOW(), NOW()),
  ('C','C','Z', 60.00, 'Produto problemático. Candidato a descontinuação.',                             true, 27, NOW(), NOW());

-- ────────────────────────────────────────────────────────────
-- PENDING PRICE CALCULATIONS
-- ────────────────────────────────────────────────────────────
CREATE TABLE pending_price_calculations (
    id                  BIGSERIAL PRIMARY KEY,
    product_id          BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    current_sale_price  NUMERIC(19, 2) NOT NULL,
    calculated_price    NUMERIC(19, 2) NOT NULL,
    final_price         NUMERIC(19, 2),
    cmv                 NUMERIC(19, 2) NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(product_id)
);

CREATE INDEX idx_pending_price_calculations_product    ON pending_price_calculations(product_id);
CREATE INDEX idx_pending_price_calculations_created_at ON pending_price_calculations(created_at);

-- ────────────────────────────────────────────────────────────
-- COMBOS
-- ────────────────────────────────────────────────────────────
CREATE TABLE combos (
    id                   BIGSERIAL PRIMARY KEY,
    product_id           BIGINT NOT NULL UNIQUE REFERENCES products(id) ON DELETE RESTRICT,
    name                 VARCHAR(255) NOT NULL,
    description          TEXT,
    image_url            TEXT,
    image_data           BYTEA,
    image_media_type     VARCHAR(100),
    sale_price           NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    active               BOOLEAN NOT NULL DEFAULT true,
    max_available_quantity INTEGER
);

CREATE INDEX idx_combos_product ON combos(product_id);
CREATE INDEX idx_combos_active  ON combos(active);

CREATE TABLE combo_items (
    id         BIGSERIAL PRIMARY KEY,
    combo_id   BIGINT NOT NULL REFERENCES combos(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    quantity   INTEGER NOT NULL CHECK (quantity > 0),
    UNIQUE(combo_id, product_id)
);

CREATE INDEX idx_combo_items_combo   ON combo_items(combo_id);
CREATE INDEX idx_combo_items_product ON combo_items(product_id);
