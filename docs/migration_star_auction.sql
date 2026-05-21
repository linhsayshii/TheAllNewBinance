-- Migration: Add Star Auction (Promote Listing) fields to auctions table
-- Run once on your MySQL database before starting the server.

ALTER TABLE auctions
    ADD COLUMN IF NOT EXISTS is_featured     BOOLEAN      NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS featured_until  DATETIME     NULL,
    ADD COLUMN IF NOT EXISTS promoted_description TEXT    NULL;

-- Index để query getFeaturedAuctions nhanh hơn
CREATE INDEX IF NOT EXISTS idx_auctions_is_featured ON auctions (is_featured, status, end_time);
