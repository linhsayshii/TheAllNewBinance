-- ============================================================
-- SCHEMA: Hệ thống Đấu giá Trực tuyến - TheAllNewBinance
-- Database: MySQL 8.0+
-- ============================================================

-- Xóa bảng theo thứ tự phụ thuộc (con trước, cha sau)
DROP TABLE IF EXISTS auto_bids;
DROP TABLE IF EXISTS bids;
DROP TABLE IF EXISTS auctions;
DROP TABLE IF EXISTS items;
DROP TABLE IF EXISTS users;

-- ============================================================
-- 1. BẢNG USERS - Quản lý người dùng
-- ============================================================
CREATE TABLE users (
    user_id      INT AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(50)  NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,              -- Lưu hash (BCrypt)
    full_name    VARCHAR(100) NOT NULL,
    email        VARCHAR(100) NOT NULL UNIQUE,
    balance      DECIMAL(15, 2) NOT NULL DEFAULT 0,  -- Số dư tài khoản
    role         ENUM('BUYER', 'SELLER', 'ADMIN') NOT NULL DEFAULT 'BUYER',
    is_active    BOOLEAN NOT NULL DEFAULT TRUE,      -- Chỉ Active = TRUE mới được tham gia đấu giá
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 2. BẢNG ITEMS - Sản phẩm đấu giá
-- ============================================================
CREATE TABLE items (
    item_id      INT AUTO_INCREMENT PRIMARY KEY,
    seller_id    INT          NOT NULL,               -- Người bán (FK -> users)
    name         VARCHAR(200) NOT NULL,
    description  TEXT,
    image_url    VARCHAR(500),
    category     VARCHAR(50),
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_items_seller
        FOREIGN KEY (seller_id) REFERENCES users(user_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 3. BẢNG AUCTIONS - Phiên đấu giá
-- ============================================================
CREATE TABLE auctions (
    auction_id       INT AUTO_INCREMENT PRIMARY KEY,
    item_id          INT            NOT NULL,          -- Sản phẩm (FK -> items)
    starting_price   DECIMAL(15, 2) NOT NULL,          -- Giá khởi điểm
    current_price    DECIMAL(15, 2) NOT NULL,          -- Giá hiện tại (cập nhật mỗi lần bid)
    bid_increment    DECIMAL(15, 2) NOT NULL DEFAULT 1.00, -- Bước giá tối thiểu
    start_time       TIMESTAMP      NOT NULL,
    end_time         TIMESTAMP      NOT NULL,          -- Có thể bị kéo dài bởi Anti-sniping
    original_end_time TIMESTAMP     NOT NULL,          -- Thời gian kết thúc gốc (không đổi)
    status           ENUM('PENDING', 'ACTIVE', 'ENDED', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    winner_id        INT            DEFAULT NULL,      -- Người thắng (FK -> users, set khi ENDED)
    final_price      DECIMAL(15, 2) DEFAULT NULL,      -- Giá chốt cuối cùng
    snipe_threshold  INT            NOT NULL DEFAULT 3, -- Anti-sniping: số phút cuối kích hoạt
    snipe_extension  INT            NOT NULL DEFAULT 2, -- Anti-sniping: số phút gia hạn thêm
    version          INT            NOT NULL DEFAULT 0, -- Optimistic Locking cho Concurrency
    created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_auctions_item
        FOREIGN KEY (item_id) REFERENCES items(item_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_auctions_winner
        FOREIGN KEY (winner_id) REFERENCES users(user_id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT chk_price
        CHECK (starting_price > 0 AND bid_increment > 0),
    CONSTRAINT chk_time
        CHECK (end_time > start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 4. BẢNG BIDS - Lịch sử trả giá
-- ============================================================
CREATE TABLE bids (
    bid_id       INT AUTO_INCREMENT PRIMARY KEY,
    auction_id   INT            NOT NULL,              -- Phiên đấu giá (FK -> auctions)
    bidder_id    INT            NOT NULL,              -- Người trả giá (FK -> users)
    amount       DECIMAL(15, 2) NOT NULL,              -- Số tiền trả
    is_auto      BOOLEAN        NOT NULL DEFAULT FALSE,-- Có phải Auto-bid không
    bid_time     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_bids_auction
        FOREIGN KEY (auction_id) REFERENCES auctions(auction_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_bids_bidder
        FOREIGN KEY (bidder_id) REFERENCES users(user_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_bid_amount
        CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- 5. BẢNG AUTO_BIDS - Cấu hình tự động trả giá
-- ============================================================
CREATE TABLE auto_bids (
    auto_bid_id  INT AUTO_INCREMENT PRIMARY KEY,
    auction_id   INT            NOT NULL,              -- Phiên đấu giá (FK -> auctions)
    bidder_id    INT            NOT NULL,              -- Người đặt auto-bid (FK -> users)
    max_amount   DECIMAL(15, 2) NOT NULL,              -- Giới hạn trả giá tối đa
    is_active    BOOLEAN        NOT NULL DEFAULT TRUE,  -- Còn hiệu lực không
    created_at   TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_autobids_auction
        FOREIGN KEY (auction_id) REFERENCES auctions(auction_id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_autobids_bidder
        FOREIGN KEY (bidder_id) REFERENCES users(user_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_max_amount
        CHECK (max_amount > 0),
    -- Mỗi người chỉ có 1 auto-bid cho 1 phiên đấu giá
    CONSTRAINT uq_autobid_per_user
        UNIQUE (auction_id, bidder_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
-- INDEXES - Tối ưu truy vấn
-- ============================================================
CREATE INDEX idx_auctions_status     ON auctions(status);
CREATE INDEX idx_auctions_end_time   ON auctions(end_time);
CREATE INDEX idx_bids_auction        ON bids(auction_id, bid_time DESC);
CREATE INDEX idx_bids_bidder         ON bids(bidder_id);
CREATE INDEX idx_items_seller        ON items(seller_id);
CREATE INDEX idx_items_category      ON items(category);
CREATE INDEX idx_autobids_auction    ON auto_bids(auction_id, is_active);

-- ============================================================
-- DỮ LIỆU MẪU (Tùy chọn - dùng cho dev/test)
-- ============================================================
/*
INSERT INTO users (username, password, full_name, email, balance, role) VALUES
    ('admin',   '$2a$10$placeholder_hash_admin',  'Quản trị viên', 'admin@auction.vn',   999999.00, 'ADMIN'),
    ('seller1', '$2a$10$placeholder_hash_seller', 'Nguyễn Văn A',  'seller1@auction.vn', 50000.00,  'SELLER'),
    ('buyer1',  '$2a$10$placeholder_hash_buyer1', 'Trần Thị B',    'buyer1@auction.vn',  100000.00, 'BUYER'),
    ('buyer2',  '$2a$10$placeholder_hash_buyer2', 'Lê Văn C',      'buyer2@auction.vn',  80000.00,  'BUYER');

INSERT INTO items (seller_id, name, description, category) VALUES
    (2, 'iPhone 15 Pro Max', 'Mới 99%, fullbox, bảo hành 10 tháng', 'Điện tử'),
    (2, 'Tranh sơn dầu phong cảnh', 'Tranh gốc, kích thước 60x80cm', 'Nghệ thuật');

INSERT INTO auctions (item_id, starting_price, current_price, bid_increment, start_time, end_time, original_end_time, status) VALUES
    (1, 5000.00, 5000.00, 500.00, NOW(), DATE_ADD(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 3 DAY), 'ACTIVE'),
    (2, 2000.00, 2000.00, 200.00, NOW(), DATE_ADD(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY), 'ACTIVE');
*/