-- Mock Data Setup for TheAllNewBinance
-- Target: MySQL 8+ / MariaDB
-- Safe to re-run: uses IF NOT EXISTS / INSERT IGNORE where applicable.
-- This script applies migrations and populates the database with default test users, items, auctions, and bids.

USE theallnewbinance;

-- 1. Apply Migration for Star Auction features if not already applied
ALTER TABLE auctions
    ADD COLUMN IF NOT EXISTS is_featured          BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS featured_until       DATETIME     NULL,
    ADD COLUMN IF NOT EXISTS promoted_description TEXT         NULL;

-- Index to optimize querying featured auctions
CREATE INDEX IF NOT EXISTS idx_auctions_is_featured ON auctions (is_featured, status, end_time);

-- 2. Clear existing records cleanly (disabling FK checks temporary to avoid constraint errors)
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE bids;
TRUNCATE TABLE auctions;
TRUNCATE TABLE items;
TRUNCATE TABLE users;
SET FOREIGN_KEY_CHECKS = 1;

-- 3. Populate Users
-- Passwords are BCrypt hashes of:
--   demo_user: 'demo1234' -> $2a$12$JtZ7XL/HImhTdlYqmmjYbuNpCCuDHaBqojJNwuWm1EjUNoc/EQlJq
--   seller_pro: 'seller1234' -> $2a$12$oNx5RMS0kMVi8i1IUcSMEesM/SH1u6/bApC6B4j3W5LPjCAy9vYDG
--   bidder_99: 'bidder1234' -> $2a$12$gjVOjc5dHsQjHKZqRov6.Ow8WIO44XwObAybOE3ITai3HzFvueSEC
INSERT INTO users (user_id, username, password, full_name, email, balance, locked_balance, role, is_active, created_at)
VALUES 
(1, 'demo_user', '$2a$12$JtZ7XL/HImhTdlYqmmjYbuNpCCuDHaBqojJNwuWm1EjUNoc/EQlJq', 'Nguyễn Văn Demo', 'demo@auction.vn', 25000.00, 4300.00, 'ADMIN', 1, '2025-01-15 10:00:00'),
(2, 'seller_pro', '$2a$12$oNx5RMS0kMVi8i1IUcSMEesM/SH1u6/bApC6B4j3W5LPjCAy9vYDG', 'Trần Thị Seller', 'seller@auction.vn', 150000.00, 0.00, 'STANDARD', 1, '2024-06-20 08:30:00'),
(3, 'bidder_99', '$2a$12$gjVOjc5dHsQjHKZqRov6.Ow8WIO44XwObAybOE3ITai3HzFvueSEC', 'Lê Minh Bidder', 'bidder@auction.vn', 8000.00, 2100.00, 'STANDARD', 1, '2025-03-10 14:00:00');

-- 4. Populate Items
INSERT INTO items (item_id, seller_id, name, description, category, image_url, is_deleted, created_at)
VALUES
(101, 1, 'Đồng hồ Vintage Omega Seamaster 1967', 'Đồng hồ cổ Omega Seamaster sản xuất năm 1967, mặt số màu xanh navy, dây da nguyên bản. Tình trạng rất tốt, có đầy đủ giấy tờ đi kèm.', 'WATCHES', 'https://res.cloudinary.com/dpclmah9p/image/upload/v1779285761/DongHo101_vsxcra.jpg', 0, '2026-05-19 09:00:00'),
(102, 2, 'Túi Hermès Birkin 30 Da Crocodile', 'Túi Hermès Birkin 30 da cá sấu màu nâu cognac năm 2019. Phụ kiện đầy đủ, hộp và giấy tờ gốc.', 'FASHION', 'https://res.cloudinary.com/dpclmah9p/image/upload/v1779285761/Tui102_afleiv.jpg', 0, '2026-05-19 10:30:00'),
(103, 2, 'Tranh Sơn Dầu "Hà Nội Mùa Đông" - Bùi Xuân Phái', 'Tác phẩm sơn dầu trên canvas, kích thước 60×80cm. Chứng nhận xác thực từ nhà đấu giá Christie\'s 2015.', 'ART', 'https://res.cloudinary.com/dpclmah9p/image/upload/v1779241324/tranh-son-dau-treo-tuong-ha-noi-mua-dong_advapd.jpg', 0, '2026-05-17 14:00:00'),
(104, 1, 'Guitar Acoustic Martin D-28 1975', 'Guitar acoustic huyền thoại Martin D-28 sản xuất 1975 tại Nazareth, PA. Âm thanh cực kỳ rich và warm. Đã được luthier kiểm tra.', 'MUSIC', 'https://res.cloudinary.com/dpclmah9p/image/upload/v1779285761/Guitar104_f0wzst.jpg', 0, '2026-05-18 08:00:00'),
(105, 2, 'Bộ Lego Star Wars Millennium Falcon 75192', 'Bộ Lego 75192 Ultimate Collector Series, 7541 mảnh. Còn nguyên hộp, chưa mở. Phiên bản giới hạn năm 2017.', 'COLLECTIBLES', 'https://res.cloudinary.com/dpclmah9p/image/upload/v1779285762/Lego105_m8af5e.jpg', 0, '2026-05-21 10:21:00'),
(106, 1, 'Xe Đạp Pinarello Dogma F12 2022', 'Khung carbon T1100 3K Torayca, size 53. Gruppo Shimano Dura-Ace Di2 R9270 12-speed. Bánh Fulcrum Racing Zero Carbon.', 'SPORTS', 'https://res.cloudinary.com/dpclmah9p/image/upload/v1779265652/xeDap106_acc2ro.jpg', 0, '2026-05-25 09:00:00'),
(107, 2, 'Máy Ảnh Leica M6 + Summicron 35mm f/2', 'Leica M6 TTL Chrome 0.72 như mới, shutter đã qua bảo dưỡng 2024. Kèm Summicron-M 35mm f/2 ASPH đời 7, CLA 2023.', 'CAMERAS', 'https://res.cloudinary.com/dpclmah9p/image/upload/v1779285761/MayAnh107_fs6u0b.jpg', 0, '2026-04-28 10:00:00'),
(108, 1, 'Rượu Vang Petrus 1982 Pomerol', 'Chai Petrus 1982 Pomerol, vintage huyền thoại. Được bảo quản trong hầm nhiệt độ 14°C liên tục. Robert Parker 100/100 điểm.', 'WINE', 'https://res.cloudinary.com/dpclmah9p/image/upload/v1779285762/RuouVang108_fleeuh.jpg', 0, '2026-04-25 10:00:00');

-- 5. Populate Auctions
INSERT INTO auctions (auction_id, item_id, seller_id, starting_price, current_price, bid_increment, start_time, original_end_time, end_time, status, winner_id, final_price, is_featured, featured_until, promoted_description)
VALUES
(1001, 101, 1, 4.50, 5.23, 0.50, '2026-05-19 09:00:00', '2026-05-22 21:00:00', '2026-05-23 01:19:00', 'ACTIVE', NULL, NULL, 1, '2026-06-15 00:00:00', 'Mô hình siêu xe tỉ lệ 1:18, độ chi tiết cực cao, sơn tĩnh điện bóng loáng.'),
(1002, 102, 2, 28.00, 29.50, 1.00, '2026-05-19 10:30:00', '2026-05-23 20:00:00', '2026-05-23 20:00:00', 'ACTIVE', NULL, NULL, 1, '2026-06-15 00:00:00', 'Đồng hồ cơ lộ máy phong cách cổ điển, dây da bò thật, bảo hành 12 tháng.'),
(1003, 103, 2, 12.00, 13.50, 0.50, '2026-05-17 14:00:00', '2026-05-24 18:00:00', '2026-05-24 18:00:00', 'ACTIVE', NULL, NULL, 1, '2026-06-15 00:00:00', 'Tai nghe chống ồn chủ động, âm bass mạnh mẽ, pin trâu 40h liên tục.'),
(1004, 104, 1, 3.50, 4.30, 0.50, '2026-05-18 08:00:00', '2026-05-24 22:00:00', '2026-05-24 22:00:00', 'ACTIVE', NULL, NULL, 0, NULL, NULL),
(1005, 105, 2, 1.80, 1.80, 0.20, '2026-05-21 10:21:00', '2026-05-25 20:00:00', '2026-05-25 20:00:00', 'PENDING', NULL, NULL, 1, NULL, NULL),
(1006, 106, 1, 8.50, 8.50, 0.50, '2026-05-25 09:00:00', '2026-05-26 21:00:00', '2026-05-26 21:00:00', 'PENDING', NULL, NULL, 0, NULL, NULL),
(1007, 107, 2, 9.50, 11.20, 0.50, '2026-04-28 10:00:00', '2026-05-05 18:00:00', '2026-05-05 18:00:00', 'ENDED', 1, 11.20, 0, NULL, NULL),
(1008, 108, 1, 6.50, 6.50, 0.50, '2026-04-25 10:00:00', '2026-05-02 18:00:00', '2026-05-02 18:00:00', 'ENDED', NULL, NULL, 0, NULL, NULL);

-- 6. Populate Bids
INSERT INTO bids (bid_id, auction_id, bidder_id, amount, created_at)
VALUES
(2001, 1001, 1, 4.60, '2026-05-08 10:15:00'),
(2002, 1001, 3, 4.70, '2026-05-08 11:30:00'),
(2003, 1001, 1, 4.85, '2026-05-08 13:00:00'),
(2004, 1001, 3, 5.00, '2026-05-09 09:20:00'),
(2005, 1001, 1, 5.10, '2026-05-09 14:45:00'),
(2006, 1001, 3, 5.23, '2026-05-10 08:00:00'),
(2007, 1002, 1, 28.50, '2026-05-09 11:00:00'),
(2008, 1002, 3, 29.00, '2026-05-09 15:30:00'),
(2009, 1002, 1, 29.50, '2026-05-10 07:30:00'),
(2010, 1003, 3, 12.20, '2026-05-07 16:00:00'),
(2011, 1003, 1, 12.50, '2026-05-08 09:00:00'),
(2012, 1003, 3, 13.00, '2026-05-09 10:00:00'),
(2013, 1003, 1, 13.30, '2026-05-09 12:00:00'),
(2014, 1003, 3, 13.50, '2026-05-10 06:00:00'),
(2015, 1004, 3, 3.70, '2026-05-10 09:00:00'),
(2016, 1004, 1, 3.90, '2026-05-10 10:30:00'),
(2017, 1004, 3, 4.10, '2026-05-10 11:00:00'),
(2018, 1004, 1, 4.30, '2026-05-10 12:00:00'),
(2019, 1007, 1, 9.70, '2026-04-28 11:00:00'),
(2020, 1007, 3, 10.00, '2026-04-29 14:00:00'),
(2021, 1007, 1, 10.50, '2026-04-30 09:00:00'),
(2022, 1007, 3, 10.80, '2026-05-01 10:00:00'),
(2023, 1007, 1, 11.20, '2026-05-02 15:00:00'),
(2024, 1001, 2, 4.60, '2026-05-08 10:20:00'),
(2025, 1002, 2, 28.50, '2026-05-09 11:15:00'),
(2026, 1003, 2, 12.20, '2026-05-07 16:10:00'),
(2027, 1004, 2, 3.60, '2026-05-10 09:10:00'),
(2028, 1007, 2, 9.80, '2026-04-28 11:10:00'),
(2029, 1001, 2, 4.90, '2026-05-08 13:10:00'),
(2030, 1002, 2, 29.10, '2026-05-09 15:40:00'),
(2031, 1003, 2, 13.10, '2026-05-09 10:10:00'),
(2032, 1004, 2, 4.00, '2026-05-10 10:45:00'),
(2033, 1007, 2, 10.60, '2026-04-30 09:10:00');
