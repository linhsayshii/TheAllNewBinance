package com.auction.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.auction.core.auction.Auction;
import com.auction.core.auction.Bid;

public class AuctionDao implements IAuctionDao {
    @Override
    public boolean createAuction(Auction auction) {
        String sql = "INSERT INTO auctions (item_id, starting_price, bid_increment, start_time, original_end_time, extended_end_time, status, is_deleted, created_at, snipe_threshold, snipe_extension) "
                +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, auction.getItemId());
            stmt.setDouble(2, auction.getStartingPrice());
            stmt.setDouble(3, auction.getBidIncrement());
            stmt.setTimestamp(4, Timestamp.valueOf(auction.getStartTime()));
            stmt.setTimestamp(5, Timestamp.valueOf(auction.getEndTime()));
            stmt.setString(6, null); // extended_end_time sẽ được cập nhật khi có gia hạn
            stmt.setString(7, auction.getStatus().name());
            stmt.setBoolean(8, false);
            stmt.setTimestamp(9, Timestamp.valueOf(auction.getCreatedAt()));
            stmt.setInt(10, auction.getSnipeSettings()[0]);
            stmt.setInt(11, auction.getSnipeSettings()[1]);
            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        auction.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error: Cannot create auction! " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean updateAuctionInformation(Auction auction) {
        String sql = "UPDATE auctions SET item_id = ?, starting_price = ?, current_price = ?, bid_increment = ?, start_time = ?, original_end_time = ?, end_time = ?, status = ?, winner_id = ?, updated_at = ?, snipe_threshold = ?, snipe_extension = ? WHERE auction_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, auction.getItemId());
            stmt.setDouble(2, auction.getStartingPrice());
            stmt.setDouble(3, auction.getCurrentPrice());
            stmt.setDouble(4, auction.getBidIncrement());
            stmt.setTimestamp(5, Timestamp.valueOf(auction.getStartTime()));
            stmt.setTimestamp(6, Timestamp.valueOf(auction.getOriginalEndTime()));
            stmt.setTimestamp(7, Timestamp.valueOf(auction.getEndTime()));
            stmt.setString(8, auction.getStatus().name());
            if (auction.getWinnerId() != null) {
                stmt.setInt(9, auction.getWinnerId());
            } else {
                stmt.setNull(9, java.sql.Types.INTEGER);
            }
            stmt.setTimestamp(10, Timestamp.valueOf(auction.getUpdatedAt()));
            stmt.setInt(11, auction.getSnipeSettings()[0]);
            stmt.setInt(12, auction.getSnipeSettings()[1]);
            stmt.setInt(13, auction.getId());
            int rowsUpdated = stmt.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            System.err.println("Error: Cannot update auction! " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean deleteAuction(Auction auction) {
        String sql = "UPDATE auctions SET is_deleted = ?, updated_at = ? WHERE auction_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, true);
            stmt.setTimestamp(2, Timestamp.valueOf(auction.getUpdatedAt()));
            stmt.setInt(3, auction.getId());
            int rowsUpdated = stmt.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            System.err.println("Error: Cannot delete auction! " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean extendAuction(Auction auction) {
        String sql = "UPDATE auctions SET end_time = ? WHERE auction_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(auction.getEndTime()));
            stmt.setInt(2, auction.getId());
            int rowsUpdated = stmt.executeUpdate();
            return rowsUpdated > 0;
        } catch (SQLException e) {
            System.err.println("Error: Cannot extend auction! " + e.getMessage());
        }
        return false;
    }

    @Override
    public Auction getAuctionDetails(Integer auctionId) {
        String sql = "SELECT * FROM auctions WHERE auction_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, auctionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Auction auction = new Auction();
                    auction.setId(rs.getInt("auction_id"));
                    auction.setItemId(rs.getInt("item_id"));
                    auction.setStartingPrice(rs.getDouble("starting_price"));
                    auction.setCurrentPrice(rs.getDouble("current_price"));
                    auction.setBidIncrement(rs.getDouble("bid_increment"));
                    auction.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
                    auction.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
                    auction.setOriginalEndTime(rs.getTimestamp("original_end_time").toLocalDateTime());
                    auction.setStatus(
                            rs.getString("status").equals("ACTIVE") ? Auction.Status.ACTIVE : Auction.Status.PENDING);
                    auction.setWinnerId(rs.getInt("winner_id"));
                    auction.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    return auction;
                }
            } catch (SQLException e) {
                System.err.println("Error: Cannot parse auction details! " + e.getMessage());
            }
        } catch (SQLException e) {
            System.err.println("Error: Cannot get auction details! " + e.getMessage());
        }
        return null;
    }

    @Override
    public double getCurrentPrice(Integer auctionId) {
        String sql = "SELECT current_price FROM auctions WHERE auction_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, auctionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("current_price");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error: Cannot get current price! " + e.getMessage());
        }
        return -1;
    }

    @Override
    public void updateCurrentPrice(Bid bid) {
        // Tái sử dụng connection nhưng với Transaction
        String selectForUpdate = "SELECT current_price FROM auctions WHERE auction_id = ? FOR UPDATE";
        String updateSql = "UPDATE auctions SET current_price = ?, updated_at = ? WHERE auction_id = ?";

        Connection conn = DBConnection.getConnection();
        try {
            // Tắt auto commit để bắt đầu Transaction
            conn.setAutoCommit(false);

            // 1. Khóa row auction này lại bằng FOR UPDATE
            try (PreparedStatement selectStmt = conn.prepareStatement(selectForUpdate)) {
                selectStmt.setInt(1, bid.getAuctionId());
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        double dbCurrentPrice = rs.getDouble("current_price");
                        if (dbCurrentPrice >= bid.getAmount()) {
                            System.err.println("Concurrency alert: Another bid was placed higher just now!");
                            conn.rollback();
                            throw new IllegalStateException("Phiên đấu giá đã có người đặt giá cao hơn!");
                        }
                    }
                }
            }

            // 2. An toàn cập nhật giá mới
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setDouble(1, bid.getAmount());
                updateStmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                updateStmt.setInt(3, bid.getAuctionId());
                updateStmt.executeUpdate();
            }

            // Commit transaction, chính thức lưu và giải phóng khóa FOR UPDATE
            conn.commit();
        } catch (SQLException e) {
            System.err.println("Error: Locked update failed! " + e.getMessage());
            try {
                if (conn != null)
                    conn.rollback();
            } catch (SQLException ex) {
                System.err.println("Error: Cannot rollback! " + ex.getMessage());
            }
        } finally {
            try {
                if (conn != null)
                    conn.setAutoCommit(true);
            } catch (SQLException ex) {
                System.err.println("Error: Cannot set auto commit! " + ex.getMessage());
            }
        }
    }

    @Override
    public boolean updateAuctionForBid(Bid bid, Auction auction) {
        // Only lock, updates, NO commit and NO setAutoCommit here. The Service layer
        // handles it.
        String selectForUpdate = "SELECT current_price, end_time FROM auctions WHERE auction_id = ? FOR UPDATE";
        String updateSql = "UPDATE auctions SET current_price = ?, end_time = ?, updated_at = ? WHERE auction_id = ?";

        Connection conn = DBConnection.getConnection();
        try {
            // 1. Khóa row auction và lấy thông tin mới nhất
            try (PreparedStatement selectStmt = conn.prepareStatement(selectForUpdate)) {
                selectStmt.setInt(1, bid.getAuctionId());
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        double dbCurrentPrice = rs.getDouble("current_price");
                        LocalDateTime dbEndTime = rs.getTimestamp("end_time").toLocalDateTime();

                        // Check price
                        if (dbCurrentPrice + auction.getBidIncrement() > bid.getAmount()) {
                            throw new IllegalStateException(
                                    "Phiên đấu giá đã có người đặt giá cao hơn hoặc không đủ bước giá!");
                        }

                        // Check time
                        LocalDateTime bidTime = bid.getCreatedAt() != null ? bid.getCreatedAt() : LocalDateTime.now();
                        if (!bidTime.isBefore(dbEndTime)) {
                            throw new IllegalStateException("Phiên đấu giá đã kết thúc!");
                        }

                        // Apply Snipe Extension safely using DB value
                        auction.setEndTime(dbEndTime);
                        auction.applySnipeExtension(bidTime);

                    } else {
                        throw new IllegalStateException("Không tìm thấy phiên đấu giá!");
                    }
                }
            }

            // 2. An toàn cập nhật giá mới và thời gian mới
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setDouble(1, bid.getAmount());
                updateStmt.setTimestamp(2, Timestamp.valueOf(auction.getEndTime()));
                updateStmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                updateStmt.setInt(4, bid.getAuctionId());
                int rows = updateStmt.executeUpdate();
                return rows > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error: Locked update failed! " + e.getMessage());
            throw new RuntimeException("Database error during bid update", e);
        }
    }

    @Override
    public Integer getSellerId(Integer auctionId) {
        String sql = "SELECT i.seller_id FROM auctions a JOIN items i ON a.item_id = i.id WHERE a.auction_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, auctionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("seller_id");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error: Cannot get seller ID! " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Auction> getAuctionsBySellerId(Integer sellerId) {
        String sql = "SELECT * FROM auctions WHERE seller_id = ?";
        List<Auction> auctions = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, sellerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Auction auction = new Auction();
                    auction.setId(rs.getInt("auction_id"));
                    auction.setItemId(rs.getInt("item_id"));
                    auction.setStartingPrice(rs.getDouble("starting_price"));
                    auction.setBidIncrement(rs.getDouble("bid_increment"));
                    auction.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
                    auction.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
                    auctions.add(auction);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error: Cannot get auctions by seller ID! " + e.getMessage());
        }
        return auctions;
    }

    @Override
    public List<com.auction.core.dto.auction.PublicAuctionDto> getPublicAuctions(int offset, int limit, boolean includeEndingSoon, boolean includeTrending) {
        StringBuilder sql = new StringBuilder(
            "SELECT a.auction_id, a.item_id, i.name as item_name, i.image_url as thumbnail_url, " +
            "a.current_price, a.end_time, u.full_name as seller_display_name ");
        
        if (includeTrending) {
            sql.append(", COUNT(b.id) as bid_count ");
        }
        
        sql.append("FROM auctions a ")
           .append("JOIN items i ON a.item_id = i.item_id ")
           .append("JOIN users u ON i.seller_id = u.user_id ");
           
        if (includeTrending) {
            sql.append("LEFT JOIN bids b ON a.auction_id = b.auction_id ");
        }
        
        sql.append("WHERE a.status = 'ACTIVE' AND a.is_deleted = false AND a.end_time > NOW() ");
        
        if (includeTrending) {
            sql.append("GROUP BY a.auction_id, a.item_id, i.name, i.image_url, a.current_price, a.end_time, u.full_name ");
            sql.append("ORDER BY bid_count DESC, a.end_time ASC ");
        } else if (includeEndingSoon) {
            sql.append("ORDER BY a.end_time ASC ");
        } else {
            sql.append("ORDER BY a.auction_id DESC "); // default fallback
        }
        
        sql.append("LIMIT ? OFFSET ?");
        
        List<com.auction.core.dto.auction.PublicAuctionDto> result = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
             
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    com.auction.core.dto.auction.PublicAuctionDto dto = new com.auction.core.dto.auction.PublicAuctionDto();
                    dto.setAuctionId(rs.getInt("auction_id"));
                    dto.setItemId(rs.getInt("item_id"));
                    dto.setItemName(rs.getString("item_name"));
                    dto.setThumbnailUrl(rs.getString("thumbnail_url"));
                    dto.setCurrentPrice(rs.getDouble("current_price"));
                    dto.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
                    dto.setSellerDisplayName(rs.getString("seller_display_name"));
                    result.add(dto);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error: Cannot get public auctions! " + e.getMessage());
        }
        return result;
    }
}
