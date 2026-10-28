-- 1. ENUM TYPES
CREATE TYPE product_type AS ENUM ('BOOK', 'NEWSPAPER', 'DVD', 'CD');
CREATE TYPE account_status AS ENUM ('ACTIVE', 'DEACTIVATED', 'BLOCKED');
CREATE TYPE order_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED');
CREATE TYPE payment_method AS ENUM ('VIETQR', 'PAYPAL');
CREATE TYPE payment_status AS ENUM ('PENDING', 'COMPLETED', 'FAILED', 'REFUNDED');
CREATE TYPE action_type AS ENUM ('CREATE', 'UPDATE', 'DELETE', 'DEACTIVATE', 'STOCK_CHANGE');
CREATE TYPE publication_frequency AS ENUM
  ('DAILY', 'WEEKLY', 'BIWEEKLY', 'MONTHLY', 'QUARTERLY', 'YEARLY');
 
-- 2. USERS & ROLES  (customers do not log in, so they have no account)
CREATE TABLE users (
  user_id     uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_name   varchar(100) NOT NULL,
  email       varchar(255) NOT NULL UNIQUE,
  password    varchar(255) NOT NULL,   
  avatar_url  varchar(500),
  status      account_status NOT NULL DEFAULT 'ACTIVE',
  created_at  timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
 
CREATE TABLE roles (
  role_id      varchar(50) PRIMARY KEY,
  description  varchar(255)
);
 
CREATE TABLE user_roles (
  user_id  uuid        NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
  role_id  varchar(50) NOT NULL REFERENCES roles(role_id) ON DELETE CASCADE,
  PRIMARY KEY (user_id, role_id)
);
 
-- 3. PRODUCTS (joined-table inheritance, discriminator = product_type)
CREATE TABLE products (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  title           varchar(255) NOT NULL,
  product_type    product_type NOT NULL,
  category        varchar(100) NOT NULL,
  original_value  bigint NOT NULL CHECK (original_value > 0), 
  current_price   bigint NOT NULL CHECK (current_price > 0),  
  weight          double precision NOT NULL DEFAULT 0 CHECK (weight >= 0),
  height          double precision NOT NULL DEFAULT 0 CHECK (height >= 0),
  length          double precision NOT NULL DEFAULT 0 CHECK (length >= 0),
  width           double precision NOT NULL DEFAULT 0 CHECK (width  >= 0),
  barcode         varchar(50) UNIQUE,
  is_active       boolean NOT NULL DEFAULT true,      
  deleted_at      timestamp,                          
  description     text,
  img_url         varchar(500),
  qty_stock       integer NOT NULL DEFAULT 0 CHECK (qty_stock >= 0),
  created_at      timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- Business rule: price must stay within 60%-120% of the original value
  CONSTRAINT ck_products_price_range
    CHECK (current_price * 10 BETWEEN original_value * 6 AND original_value * 12),
  -- Needed so subtype tables can reference (id, product_type) together.
  CONSTRAINT uq_products_id_type UNIQUE (id, product_type)
);
 
CREATE TABLE books (
  product_id        uuid PRIMARY KEY,
  product_type      product_type NOT NULL DEFAULT 'BOOK' CHECK (product_type = 'BOOK'),
  publisher         varchar(255) NOT NULL,
  publication_date  date NOT NULL,
  language          varchar(50),
  cover_type        varchar(50) NOT NULL CHECK (cover_type IN ('PAPERBACK', 'HARDCOVER')),
  genre             varchar(255),
  nb_pages          integer CHECK (nb_pages > 0),
  FOREIGN KEY (product_id, product_type) REFERENCES products(id, product_type) ON DELETE CASCADE
);
 
CREATE TABLE newspapers (
  product_id             uuid PRIMARY KEY,
  product_type           product_type NOT NULL DEFAULT 'NEWSPAPER' CHECK (product_type = 'NEWSPAPER'),
  publisher              varchar(255) NOT NULL,
  publication_date       date NOT NULL,
  editor_in_chief        varchar(255) NOT NULL,
  language               varchar(50),
  issue_number           varchar(50),
  publication_frequency  publication_frequency,
  issn                   varchar(20),
  FOREIGN KEY (product_id, product_type) REFERENCES products(id, product_type) ON DELETE CASCADE
);
 
CREATE TABLE cds (
  product_id    uuid PRIMARY KEY,
  product_type  product_type NOT NULL DEFAULT 'CD' CHECK (product_type = 'CD'),
  record_label  varchar(255) NOT NULL,
  release_date  date,
  total_length  integer NOT NULL CHECK (total_length >= 0), 
  genre         varchar(255),
  FOREIGN KEY (product_id, product_type) REFERENCES products(id, product_type) ON DELETE CASCADE
);
 
CREATE TABLE dvds (
  product_id    uuid PRIMARY KEY,
  product_type  product_type NOT NULL DEFAULT 'DVD' CHECK (product_type = 'DVD'),
  disc_type     varchar(50) NOT NULL CHECK (disc_type IN ('BLU-RAY', 'HD-DVD')),
  director      varchar(255) NOT NULL,
  genre         varchar(255),
  studio        varchar(255) NOT NULL,
  release_date  date,
  language      varchar(50) NOT NULL,
  total_length  integer NOT NULL CHECK (total_length > 0),   
  FOREIGN KEY (product_id, product_type) REFERENCES products(id, product_type) ON DELETE CASCADE
);
 
-- 4. MULTI-VALUED ATTRIBUTES & N-N RELATIONSHIPS
CREATE TABLE authors (
  author_id  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  name       varchar(255) NOT NULL
);
 
CREATE TABLE book_author_mapping (
  book_id    uuid NOT NULL REFERENCES books(product_id) ON DELETE CASCADE,
  author_id  uuid NOT NULL REFERENCES authors(author_id) ON DELETE CASCADE,
  PRIMARY KEY (book_id, author_id)
);
 
CREATE TABLE artists (
  artist_id    uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  artist_name  varchar(255) NOT NULL
);
 
CREATE TABLE cd_artist_mapping (
  cd_id      uuid NOT NULL REFERENCES cds(product_id) ON DELETE CASCADE,
  artist_id  uuid NOT NULL REFERENCES artists(artist_id) ON DELETE CASCADE,
  PRIMARY KEY (cd_id, artist_id)
);
 
CREATE TABLE sections (
  section_id  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  title       varchar(255) NOT NULL UNIQUE
);
 
CREATE TABLE newspaper_section_mapping (
  newspaper_id  uuid NOT NULL REFERENCES newspapers(product_id) ON DELETE CASCADE,
  section_id    uuid NOT NULL REFERENCES sections(section_id) ON DELETE CASCADE,
  PRIMARY KEY (newspaper_id, section_id)
);
 
CREATE TABLE tracks (
  track_id  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  cd_id     uuid NOT NULL REFERENCES cds(product_id) ON DELETE CASCADE,
  title     varchar(255) NOT NULL,
  length    integer NOT NULL DEFAULT 0 CHECK (length >= 0)    -- seconds
);
 
CREATE TABLE dvd_subtitles (
  subtitle_id  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  dvd_id       uuid NOT NULL REFERENCES dvds(product_id) ON DELETE CASCADE,
  language     varchar(50) NOT NULL,
  UNIQUE (dvd_id, language)
);
 
-- 5. ORDERS, DELIVERY, INVOICE, PAYMENT
CREATE TABLE orders (
  order_id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  status                  order_status NOT NULL DEFAULT 'PENDING',
  sub_total               decimal(12,2) NOT NULL CHECK (sub_total >= 0),    
  vat                     decimal(12,2) NOT NULL CHECK (vat >= 0),          
  shipping_fee            decimal(12,2) NOT NULL CHECK (shipping_fee >= 0),  
  total                   decimal(12,2) NOT NULL CHECK (total >= 0),
  shipping_rule_snapshot  jsonb NOT NULL,
  reviewed_by             uuid REFERENCES users(user_id),
  reviewed_at             timestamp,
  created_at              timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT ck_orders_total CHECK (total = sub_total + vat + shipping_fee),
  CONSTRAINT ck_orders_review
    CHECK (status = 'PENDING' OR status = 'CANCELLED'
           OR (reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL))
);
 
CREATE TABLE order_items (
  order_id        uuid NOT NULL REFERENCES orders(order_id) ON DELETE CASCADE,
  product_id      uuid NOT NULL REFERENCES products(id),  
  product_title   varchar(255) NOT NULL,                    
  purchase_price  bigint NOT NULL CHECK (purchase_price >= 0),  
  img_url         varchar(500),                            
  quantity        integer NOT NULL CHECK (quantity > 0),
  PRIMARY KEY (order_id, product_id)
);
 
CREATE TABLE delivery_info (
  order_id        uuid PRIMARY KEY REFERENCES orders(order_id) ON DELETE CASCADE,
  receiver_name   varchar(200) NOT NULL,
  phone_number    varchar(20)  NOT NULL,
  email           varchar(255) NOT NULL,
  province        varchar(100) NOT NULL,
  detail_address  varchar(500) NOT NULL,
  note            text
);
 
CREATE TABLE invoices (
  invoice_id      uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  order_id        uuid NOT NULL UNIQUE REFERENCES orders(order_id),
  total_amount    decimal(12,2) NOT NULL CHECK (total_amount >= 0), 
  created_at      timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT uq_invoices_id_order UNIQUE (invoice_id, order_id)
);
 
CREATE TABLE payment_transactions (
  transaction_id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  invoice_id              uuid NOT NULL,
  order_id                uuid NOT NULL,        
  method                  payment_method NOT NULL,
  amount                  decimal(12,2) NOT NULL CHECK (amount > 0),
  gateway_transaction_id  varchar(100),         
  gateway_refund_id       varchar(100),         
  transaction_content     varchar(255),
  transaction_time        timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status                  payment_status NOT NULL DEFAULT 'PENDING',
  error_message           varchar(500),
  FOREIGN KEY (invoice_id, order_id) REFERENCES invoices(invoice_id, order_id),
  CONSTRAINT uq_payment_gateway UNIQUE (method, gateway_transaction_id),
  CONSTRAINT ck_payment_refund CHECK (gateway_refund_id IS NULL OR method = 'PAYPAL')
);

-- 6. LOGS
CREATE TABLE product_history_logs (
  id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  product_id     uuid NOT NULL REFERENCES products(id),
  action_type    action_type NOT NULL,
  old_value      jsonb,
  new_value      jsonb,
  justification  text,
  performed_by   uuid NOT NULL REFERENCES users(user_id),
  created_at     timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT ck_history_stock_reason
    CHECK (action_type <> 'STOCK_CHANGE' OR length(btrim(coalesce(justification, ''))) > 0)
);
 
CREATE TABLE system_audit_logs (
  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  action          varchar(100) NOT NULL,         
  target_user_id  uuid NOT NULL REFERENCES users(user_id),
  performed_by    uuid NOT NULL REFERENCES users(user_id),
  old_value       jsonb,
  new_value       jsonb,
  notified_at     timestamp,                    
  created_at      timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
 
-- 7. INDEXES
CREATE INDEX idx_user_roles_role_id          ON user_roles(role_id);
CREATE INDEX idx_book_author_author_id       ON book_author_mapping(author_id);
CREATE INDEX idx_cd_artist_artist_id         ON cd_artist_mapping(artist_id);
CREATE INDEX idx_newspaper_section_section   ON newspaper_section_mapping(section_id);
CREATE INDEX idx_tracks_cd_id                ON tracks(cd_id);
CREATE INDEX idx_dvd_subtitles_dvd_id        ON dvd_subtitles(dvd_id);
 
CREATE INDEX idx_products_title              ON products(title);
CREATE INDEX idx_products_category           ON products(category);
CREATE INDEX idx_products_type               ON products(product_type);
CREATE INDEX idx_products_active_price       ON products(is_active, current_price);
 
CREATE INDEX idx_orders_status_created       ON orders(status, created_at);  
CREATE INDEX idx_orders_reviewed_by          ON orders(reviewed_by);
CREATE INDEX idx_order_items_product_id      ON order_items(product_id);
CREATE INDEX idx_payment_invoice_id          ON payment_transactions(invoice_id);
CREATE INDEX idx_payment_order_id            ON payment_transactions(order_id);
 
CREATE INDEX idx_phl_product_created         ON product_history_logs(product_id, created_at);
CREATE INDEX idx_phl_user_action_created     ON product_history_logs(performed_by, action_type, created_at);
CREATE INDEX idx_audit_target_user           ON system_audit_logs(target_user_id, created_at);
 
-- 8. TRIGGERS / BUSINESS RULES ENFORCED IN THE DATABASE
-- 8.1 Keep updated_at fresh
CREATE FUNCTION fn_set_updated_at() RETURNS trigger AS $$
BEGIN
  NEW.updated_at := CURRENT_TIMESTAMP;
  RETURN NEW;
END $$ LANGUAGE plpgsql;
 
CREATE TRIGGER trg_users_updated    BEFORE UPDATE ON users
  FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
CREATE TRIGGER trg_products_updated BEFORE UPDATE ON products
  FOR EACH ROW EXECUTE FUNCTION fn_set_updated_at();
 
-- 8.2 A product may be physically deleted only if its stock is zero
CREATE FUNCTION fn_products_block_delete() RETURNS trigger AS $$
BEGIN
  IF OLD.qty_stock > 0 THEN
    RAISE EXCEPTION 'Cannot delete product %: stock is %, deactivate it instead',
      OLD.id, OLD.qty_stock;
  END IF;
  RETURN OLD;
END $$ LANGUAGE plpgsql;
 
CREATE TRIGGER trg_products_block_delete BEFORE DELETE ON products
  FOR EACH ROW EXECUTE FUNCTION fn_products_block_delete();
 
-- 8.3 products.product_type must have a matching row in its subtype table
CREATE FUNCTION fn_check_product_subtype() RETURNS trigger AS $$
DECLARE ok boolean;
BEGIN
  ok := CASE NEW.product_type
    WHEN 'BOOK'      THEN EXISTS (SELECT 1 FROM books      WHERE product_id = NEW.id)
    WHEN 'NEWSPAPER' THEN EXISTS (SELECT 1 FROM newspapers WHERE product_id = NEW.id)
    WHEN 'CD'        THEN EXISTS (SELECT 1 FROM cds        WHERE product_id = NEW.id)
    WHEN 'DVD'       THEN EXISTS (SELECT 1 FROM dvds       WHERE product_id = NEW.id)
  END;
  IF NOT ok THEN
    RAISE EXCEPTION 'Product % is of type % but has no row in the subtype table',
      NEW.id, NEW.product_type;
  END IF;
  RETURN NULL;
END $$ LANGUAGE plpgsql;
 
CREATE CONSTRAINT TRIGGER trg_products_subtype
  AFTER INSERT OR UPDATE OF product_type ON products
  DEFERRABLE INITIALLY DEFERRED
  FOR EACH ROW EXECUTE FUNCTION fn_check_product_subtype();
 
-- 8.4 A manager may delete at most 20 products per day
CREATE FUNCTION fn_limit_daily_deletes() RETURNS trigger AS $$
DECLARE n integer;
BEGIN
  IF NEW.action_type = 'DELETE' THEN
    SELECT count(*) INTO n
    FROM product_history_logs
    WHERE performed_by = NEW.performed_by
      AND action_type  = 'DELETE'
      AND created_at  >= CURRENT_DATE;
    IF n >= 20 THEN
      RAISE EXCEPTION 'Daily limit of 20 product deletions reached for user %', NEW.performed_by;
    END IF;
  END IF;
  RETURN NEW;
END $$ LANGUAGE plpgsql;
 
CREATE TRIGGER trg_phl_daily_delete_limit BEFORE INSERT ON product_history_logs
  FOR EACH ROW EXECUTE FUNCTION fn_limit_daily_deletes();