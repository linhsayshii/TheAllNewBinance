-- Schema for TheAllNewBinance server module
-- Target: MySQL 8+

CREATE DATABASE IF NOT EXISTS theallnewbinance
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'binance'@'localhost' IDENTIFIED BY 'PasswordCucManh!';
GRANT ALL PRIVILEGES ON theallnewbinance.* TO 'binance'@'localhost';
FLUSH PRIVILEGES;

USE theallnewbinance;

-- =========================
-- Table: users
-- =========================
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    locked_balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    role VARCHAR(20) NOT NULL DEFAULT 'STANDARD',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('STANDARD', 'ADMIN')),
    CONSTRAINT ck_users_balance_non_negative CHECK (balance >= 0),
    CONSTRAINT ck_users_locked_balance_non_negative CHECK (locked_balance >= 0)
);

-- =========================
-- Table: items
-- =========================
CREATE TABLE IF NOT EXISTS items (
    item_id INT AUTO_INCREMENT PRIMARY KEY,
    seller_id INT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    image_url VARCHAR(500),
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Polymorphic product attributes
    brand VARCHAR(255) DEFAULT NULL,
    item_condition VARCHAR(255) DEFAULT NULL,
    warranty_months INT DEFAULT NULL,
    custom_attributes TEXT DEFAULT NULL,
    has_certificate BOOLEAN DEFAULT NULL,
    artist VARCHAR(255) DEFAULT NULL,
    year_created INT DEFAULT NULL,
    model VARCHAR(255) DEFAULT NULL,

    CONSTRAINT fk_items_seller
        FOREIGN KEY (seller_id) REFERENCES users(user_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

CREATE INDEX idx_items_seller_id ON items (seller_id);
CREATE INDEX idx_items_category ON items (category);

-- =========================
-- Table: auctions
-- Notes:
-- 1) Both end_time and extended_end_time are present because DAO currently uses both.
-- 2) seller_id is optional but included because getAuctionsBySellerId() queries it.
-- =========================
CREATE TABLE IF NOT EXISTS auctions (
    auction_id INT AUTO_INCREMENT PRIMARY KEY,
    item_id INT NOT NULL,
    seller_id INT NULL,
    starting_price DECIMAL(15,2) NOT NULL,
    current_price DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    bid_increment DECIMAL(15,2) NOT NULL DEFAULT 1.00,
    start_time TIMESTAMP NOT NULL,
    original_end_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NULL,
    extended_end_time TIMESTAMP NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    winner_id INT NULL,
    final_price DECIMAL(15,2) NULL,
    snipe_threshold INT NOT NULL DEFAULT 120,
    snipe_extension INT NOT NULL DEFAULT 120,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_auctions_item
        FOREIGN KEY (item_id) REFERENCES items(item_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT fk_auctions_seller
        FOREIGN KEY (seller_id) REFERENCES users(user_id)
        ON UPDATE CASCADE
        ON DELETE SET NULL,

    CONSTRAINT fk_auctions_winner
        FOREIGN KEY (winner_id) REFERENCES users(user_id)
        ON UPDATE CASCADE
        ON DELETE SET NULL,

    CONSTRAINT ck_auctions_status CHECK (status IN ('PENDING', 'ACTIVE', 'ENDED', 'CANCELLED')),
    CONSTRAINT ck_auctions_starting_price_non_negative CHECK (starting_price >= 0),
    CONSTRAINT ck_auctions_current_price_non_negative CHECK (current_price >= 0),
    CONSTRAINT ck_auctions_bid_increment_positive CHECK (bid_increment > 0)
);

CREATE INDEX idx_auctions_item_id ON auctions (item_id);
CREATE INDEX idx_auctions_seller_id ON auctions (seller_id);
CREATE INDEX idx_auctions_status ON auctions (status);
CREATE INDEX idx_auctions_end_time ON auctions (end_time);

-- Keep data compatible with current DAO behavior.
DROP TRIGGER IF EXISTS trg_auctions_before_insert;
DROP TRIGGER IF EXISTS trg_auctions_before_update;

DELIMITER $$
CREATE TRIGGER trg_auctions_before_insert
BEFORE INSERT ON auctions
FOR EACH ROW
BEGIN
    IF NEW.end_time IS NULL THEN
        SET NEW.end_time = NEW.original_end_time;
    END IF;

    IF NEW.current_price IS NULL OR NEW.current_price = 0 THEN
        SET NEW.current_price = NEW.starting_price;
    END IF;

    IF NEW.seller_id IS NULL THEN
        SET NEW.seller_id = (
            SELECT i.seller_id
            FROM items i
            WHERE i.item_id = NEW.item_id
            LIMIT 1
        );
    END IF;
END$$

CREATE TRIGGER trg_auctions_before_update
BEFORE UPDATE ON auctions
FOR EACH ROW
BEGIN
    IF NEW.end_time IS NULL THEN
        SET NEW.end_time = NEW.original_end_time;
    END IF;

    IF NEW.seller_id IS NULL THEN
        SET NEW.seller_id = (
            SELECT i.seller_id
            FROM items i
            WHERE i.item_id = NEW.item_id
            LIMIT 1
        );
    END IF;
END$$
DELIMITER ;

-- Backfill old rows if the schema is applied on an existing database.
UPDATE auctions
SET end_time = original_end_time
WHERE end_time IS NULL;

UPDATE auctions a
SET a.seller_id = (
    SELECT i.seller_id
    FROM items i
    WHERE i.item_id = a.item_id
    LIMIT 1
)
WHERE a.seller_id IS NULL;

-- =========================
-- Table: bids
-- =========================
CREATE TABLE IF NOT EXISTS bids (
    bid_id INT AUTO_INCREMENT PRIMARY KEY,
    auction_id INT NOT NULL,
    bidder_id INT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_bids_auction
        FOREIGN KEY (auction_id) REFERENCES auctions(auction_id)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT fk_bids_bidder
        FOREIGN KEY (bidder_id) REFERENCES users(user_id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT ck_bids_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_bids_auction_id ON bids (auction_id);
CREATE INDEX idx_bids_bidder_id ON bids (bidder_id);
CREATE INDEX idx_bids_amount ON bids (amount);
CREATE INDEX idx_bids_created_at ON bids (created_at);
