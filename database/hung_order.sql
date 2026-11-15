-- Chạy SAU aims.db và seed.sql:  mysql -u root -p --default-character-set=utf8mb4 < database/hung_order.sql
-- Bảng đặt tên tạm place_order / place_order_item để không đụng bảng của thiết kế chính thức.
USE aims;

CREATE TABLE IF NOT EXISTS place_order (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_name       VARCHAR(255) NOT NULL,
    phone               VARCHAR(20)  NOT NULL,
    email               VARCHAR(255) NOT NULL,
    address             VARCHAR(500) NOT NULL,
    province            VARCHAR(100) NOT NULL,
    total_excluding_vat BIGINT NOT NULL,
    vat_amount          BIGINT NOT NULL,
    total_including_vat BIGINT NOT NULL,
    shipping_fee        BIGINT NOT NULL,
    total_amount        BIGINT NOT NULL,
    status              VARCHAR(30) NOT NULL,
    created_at          DATETIME NOT NULL
) CHARACTER SET utf8mb4;

CREATE TABLE IF NOT EXISTS place_order_item (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT NOT NULL,
    product_id INT NOT NULL,
    title      VARCHAR(255) NOT NULL,
    unit_price BIGINT NOT NULL,
    quantity   INT NOT NULL,
    FOREIGN KEY (order_id) REFERENCES place_order(id)
) CHARACTER SET utf8mb4;

-- CHỈ DÙNG NẾU CHƯA CÓ bảng product (Thành chưa xong). Nếu đã có bảng product của nhóm thì bỏ qua đoạn dưới.
CREATE TABLE IF NOT EXISTS product (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    title         VARCHAR(255) NOT NULL,
    current_price BIGINT NOT NULL,
    weight        DOUBLE NOT NULL,
    quantity      INT NOT NULL,
    image_url     VARCHAR(500)
) CHARACTER SET utf8mb4;

INSERT INTO product (title, current_price, weight, quantity, image_url)
SELECT * FROM (
    SELECT 'Dế Mèn Phiêu Lưu Ký' AS t, 85000 AS p, 0.4 AS w, 50 AS q, NULL AS i UNION ALL
    SELECT 'Báo Tuổi Trẻ cuối tuần', 15000, 0.2, 100, NULL UNION ALL
    SELECT 'CD Trịnh Công Sơn - Tuyển tập', 120000, 0.15, 30, NULL UNION ALL
    SELECT 'DVD Phim Tuổi Thơ Dữ Dội', 150000, 0.2, 5, NULL UNION ALL
    SELECT 'Bộ sách giáo khoa lớp 12 (hộp)', 450000, 4.0, 3, NULL
) AS seed WHERE NOT EXISTS (SELECT 1 FROM product);
