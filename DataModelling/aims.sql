-- AIMS - An Internet Media Store
-- Script tạo CSDL (SQL Server 2022)

CREATE DATABASE aims;
GO

USE aims;
GO

-- ===== Sản phẩm =====

CREATE TABLE products (
    id                  INT IDENTITY(1,1) PRIMARY KEY,
    title               NVARCHAR(255) NOT NULL,
    category            NVARCHAR(100) NOT NULL,
    description         NVARCHAR(MAX) NOT NULL,
    barcode             NVARCHAR(50)  NOT NULL UNIQUE,
    original_value      INT NOT NULL,
    current_price       INT NOT NULL,
    quantity_in_stock   INT NOT NULL DEFAULT 0 CHECK (quantity_in_stock >= 0),
    weight              DECIMAL(10,2) NOT NULL,
    height              DECIMAL(10,2) NOT NULL,
    width               DECIMAL(10,2) NOT NULL,
    length              DECIMAL(10,2) NOT NULL,
    -- giá bán 60% - 120% giá gốc
    CHECK (current_price BETWEEN original_value * 0.6 AND original_value * 1.2)
);

CREATE TABLE books (
    product_id          INT PRIMARY KEY,
    author              NVARCHAR(255) NOT NULL,
    cover_type          NVARCHAR(20)  NOT NULL CHECK (cover_type IN ('PAPERBACK', 'HARDCOVER')),
    publisher           NVARCHAR(255) NOT NULL,
    publication_date    DATE NOT NULL,
    number_of_pages     INT,
    language            NVARCHAR(50),
    genre               NVARCHAR(50),
    CONSTRAINT fk_books_products FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE TABLE newspapers (
    product_id              INT PRIMARY KEY,
    editor_in_chief         NVARCHAR(100) NOT NULL,
    publisher               NVARCHAR(255) NOT NULL,
    publication_date        DATE NOT NULL,
    issue_number            NVARCHAR(20),
    publication_frequency   NVARCHAR(20),
    issn                    NVARCHAR(9),
    language                NVARCHAR(50),
    sections                NVARCHAR(255),
    CONSTRAINT fk_newspapers_products FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE TABLE cds (
    product_id          INT PRIMARY KEY,
    artist              NVARCHAR(255) NOT NULL,
    record_label        NVARCHAR(255) NOT NULL,
    music_type          NVARCHAR(50)  NOT NULL,
    release_date        DATE,
    CONSTRAINT fk_cds_products FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE TABLE tracks (
    id                  INT IDENTITY(1,1) PRIMARY KEY,
    cd_id               INT NOT NULL,
    track_number        INT NOT NULL,
    title               NVARCHAR(255) NOT NULL,
    length_seconds      INT NOT NULL,
    UNIQUE (cd_id, track_number),
    CONSTRAINT fk_tracks_cds FOREIGN KEY (cd_id) REFERENCES cds(product_id) ON DELETE CASCADE
);

CREATE TABLE dvds (
    product_id          INT PRIMARY KEY,
    disc_type           NVARCHAR(20)  NOT NULL CHECK (disc_type IN ('BLU_RAY', 'HD_DVD')),
    director            NVARCHAR(100) NOT NULL,
    runtime             INT NOT NULL,
    studio              NVARCHAR(255) NOT NULL,
    language            NVARCHAR(50)  NOT NULL,
    subtitles           NVARCHAR(255) NOT NULL,
    release_date        DATE,
    CONSTRAINT fk_dvds_products FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

-- ===== Đơn hàng & thanh toán =====

CREATE TABLE delivery_info (
    id                  INT IDENTITY(1,1) PRIMARY KEY,
    name                NVARCHAR(100) NOT NULL,
    phone               NVARCHAR(15)  NOT NULL,
    email               NVARCHAR(255) NOT NULL,
    province            NVARCHAR(100) NOT NULL,
    address             NVARCHAR(255) NOT NULL,
    instructions        NVARCHAR(500)
);

CREATE TABLE orders (
    id                  INT IDENTITY(1,1) PRIMARY KEY,
    delivery_info_id    INT NOT NULL UNIQUE,
    shipping_fees       INT NOT NULL,
    status              NVARCHAR(20) NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    CONSTRAINT fk_orders_delivery_info FOREIGN KEY (delivery_info_id) REFERENCES delivery_info(id)
);

-- quan hệ Order - Product, lưu giá lúc mua
CREATE TABLE order_items (
    order_id            INT NOT NULL,
    product_id          INT NOT NULL,
    quantity            INT NOT NULL CHECK (quantity > 0),
    unit_price          INT NOT NULL,
    PRIMARY KEY (order_id, product_id),
    CONSTRAINT fk_order_items_orders FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_products FOREIGN KEY (product_id) REFERENCES products(id)
);

CREATE TABLE invoices (
    id                  INT IDENTITY(1,1) PRIMARY KEY,
    order_id            INT NOT NULL UNIQUE,
    total_amount        INT NOT NULL,
    CONSTRAINT fk_invoices_orders FOREIGN KEY (order_id) REFERENCES orders(id)
);

CREATE TABLE cards (
    id                  INT IDENTITY(1,1) PRIMARY KEY,
    card_code           NVARCHAR(20)  NOT NULL,
    owner               NVARCHAR(100) NOT NULL,
    cvv_code            NVARCHAR(4)   NOT NULL,
    date_expired        NVARCHAR(4)   NOT NULL
);

-- thanh toán QR thì không có thẻ nên card_id để NULL
CREATE TABLE payment_transactions (
    id                  INT IDENTITY(1,1) PRIMARY KEY,
    invoice_id          INT NOT NULL,
    card_id             INT,
    method              NVARCHAR(20)  NOT NULL CHECK (method IN ('VIETQR', 'PAYPAL')),
    content             NVARCHAR(255) NOT NULL,
    created_at          DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT fk_payment_transactions_invoices FOREIGN KEY (invoice_id) REFERENCES invoices(id),
    CONSTRAINT fk_payment_transactions_cards FOREIGN KEY (card_id) REFERENCES cards(id)
);
GO
