package com.auction.server.dao;

import com.auction.core.auction.Auction;
import com.auction.core.auction.Bid;
import com.auction.server.dao.impl.IAuctionDao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AuctionDao implements IAuctionDao {
    @Override
    public boolean createAuction(Auction auction) {
        try (Connection conn = DBConnection.getConnection()) {
            return createAuctionWithConnection(conn, auction);
        } catch (SQLException e) {
            System.err.println("Error: Cannot open connection for createAuction! " + e.getMessage());
        }
        return false;
    }

    @Override
    public boolean createAuctionWithConnection(Connection conn, Auction auction)
            throws SQLException {
        String sql =
                "INSERT INTO auctions (item_id, starting_price, bid_increment, start_time,"
                    + " original_end_time, extended_end_time, status, is_deleted, created_at,"
                    + " snipe_threshold, snipe_extension) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt =
                conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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
        }
        return false;
    }


    @Override
    public boolean updateAuctionInformation(Auction auction) {
        String sql =
                "UPDATE auctions SET item_id = ?, starting_price = ?, current_price = ?,"
                        + " bid_increment = ?, start_time = ?, original_end_time = ?, end_time = ?,"
                        + " status = ?, winner_id = ?, updated_at = ?, snipe_threshold = ?,"
                        + " snipe_extension = ? WHERE auction_id = ?";
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
                    auction.setOriginalEndTime(
                            rs.getTimestamp("original_end_time").toLocalDateTime());

                    // Safe Enum Parsing: phòng ngự trường hợp DB chứa status viết thường
                    // hoặc dữ liệu không hợp lệ từ mock data
                    String statusStr = rs.getString("status");
                    Auction.Status status = Auction.Status.PENDING;
                    if (statusStr != null) {
                        try {
                            status = Auction.Status.valueOf(statusStr.toUpperCase().trim());
                        } catch (IllegalArgumentException e) {
                            System.err.println(
                                    "[AuctionDao] Warning: Invalid auction status in database: '"
                                            + statusStr
                                            + "'. Falling back to PENDING.");
                        }
                    }
                    auction.setStatus(status);

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
        // Queue đã serialize bid per auction nên không cần FOR UPDATE
        String selectSql = "SELECT current_price FROM auctions WHERE auction_id = ?";
        String updateSql =
                "UPDATE auctions SET current_price = ?, updated_at = ? WHERE auction_id = ?";

        Connection conn = DBConnection.getConnection();
        try {
            // 1. Đọc giá hiện tại (không cần lock vì queue đã serialize)
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setInt(1, bid.getAuctionId());
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        double dbCurrentPrice = rs.getDouble("current_price");
                        if (dbCurrentPrice >= bid.getAmount()) {
                            throw new IllegalStateException(
                                    "Phiên đấu giá đã có người đặt giá cao hơn!");
                        }
                    }
                }
            }

            // 2. Cập nhật giá mới
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setDouble(1, bid.getAmount());
                updateStmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                updateStmt.setInt(3, bid.getAuctionId());
                updateStmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error: Update price failed! " + e.getMessage());
            throw new RuntimeException("Database error during price update", e);
        }
    }

    @Override
    public boolean updateAuctionForBid(Bid bid, Auction auction) {
        // Queue đã serialize bid per auction — không cần FOR UPDATE
        String selectSql = "SELECT current_price, end_time FROM auctions WHERE auction_id = ?";
        String updateSql =
                "UPDATE auctions SET current_price = ?, end_time = ?, updated_at = ? WHERE"
                        + " auction_id = ?";

        Connection conn = DBConnection.getConnection();
        try {
            // 1. Đọc thông tin mới nhất từ DB (serialized bởi queue)
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setInt(1, bid.getAuctionId());
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        double dbCurrentPrice = rs.getDouble("current_price");
                        LocalDateTime dbEndTime = rs.getTimestamp("end_time").toLocalDateTime();

                        // Check price
                        if (dbCurrentPrice + auction.getBidIncrement() > bid.getAmount()) {
                            throw new IllegalStateException(
                                    "Phiên đấu giá đã có người đặt giá cao hơn hoặc không đủ bước"
                                            + " giá!");
                        }

                        // Check time
                        LocalDateTime bidTime =
                                bid.getCreatedAt() != null
                                        ? bid.getCreatedAt()
                                        : LocalDateTime.now();
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

            // 2. Cập nhật giá mới và thời gian mới
            try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                updateStmt.setDouble(1, bid.getAmount());
                updateStmt.setTimestamp(2, Timestamp.valueOf(auction.getEndTime()));
                updateStmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                updateStmt.setInt(4, bid.getAuctionId());
                int rows = updateStmt.executeUpdate();
                return rows > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error: Update bid failed! " + e.getMessage());
            throw new RuntimeException("Database error during bid update", e);
        }
    }

    @Override
    public Integer getSellerId(Integer auctionId) {
        String sql =
                "SELECT i.seller_id FROM auctions a JOIN items i ON a.item_id = i.item_id WHERE"
                        + " a.auction_id = ?";
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
    public Integer getSellerId(Connection conn, Integer auctionId) throws SQLException {
        String sql =
                "SELECT i.seller_id FROM auctions a JOIN items i ON a.item_id = i.item_id WHERE"
                        + " a.auction_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, auctionId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("seller_id");
                }
            }
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
    public List<com.auction.core.dto.auction.PublicAuctionDto> getPublicAuctions(
            int offset,
            int limit,
            List<String> statuses,
            boolean includeEndingSoon,
            boolean includeTrending) {
        List<String> safeStatuses =
                statuses == null
                        ? List.of()
                        : statuses.stream()
                                .filter(java.util.Objects::nonNull)
                                .map(String::trim)
                                .map(String::toUpperCase)
                                .filter(s -> !s.isEmpty())
                                .toList();

        if (safeStatuses.isEmpty()) {
            safeStatuses = List.of("ACTIVE", "PENDING");
        }

        String statusPlaceholders =
                safeStatuses.stream().map(s -> "?").collect(Collectors.joining(", "));

        StringBuilder sql =
                new StringBuilder(
                        "SELECT a.auction_id, a.item_id, i.name as item_name, i.image_url as"
                            + " thumbnail_url, a.current_price, a.start_time, a.end_time, a.status,"
                            + " u.full_name as seller_display_name ");

        if (includeTrending) {
            sql.append(", COUNT(b.bid_id) as bid_count ");
        }

        sql.append("FROM auctions a ")
                .append("JOIN items i ON a.item_id = i.item_id ")
                .append("JOIN users u ON i.seller_id = u.user_id ");

        if (includeTrending) {
            sql.append("LEFT JOIN bids b ON a.auction_id = b.auction_id ");
        }

        sql.append("WHERE a.status IN (")
                .append(statusPlaceholders)
                .append(") AND a.is_deleted = false ");

        if (safeStatuses.contains("ACTIVE")) {
            sql.append("AND ((a.status = 'ACTIVE' AND a.end_time > ?) OR a.status <> 'ACTIVE') ");
        }

        if (safeStatuses.contains("PENDING")) {
            sql.append(
                    "AND ((a.status = 'PENDING' AND a.start_time >= ?) OR a.status <> 'PENDING') ");
        }

        if (includeTrending) {
            sql.append(
                    "GROUP BY a.auction_id, a.item_id, i.name, i.image_url, a.current_price,"
                            + " a.start_time, a.end_time, a.status, u.full_name ");
            sql.append("ORDER BY bid_count DESC, a.end_time ASC, a.start_time ASC ");
        } else if (includeEndingSoon) {
            sql.append("ORDER BY (CASE WHEN a.status = 'ACTIVE' THEN a.end_time END) IS NULL ASC, ")
                    .append("CASE WHEN a.status = 'ACTIVE' THEN a.end_time END ASC, ")
                    .append("(CASE WHEN a.status = 'PENDING' THEN a.start_time END) IS NULL ASC, ")
                    .append("CASE WHEN a.status = 'PENDING' THEN a.start_time END ASC, ")
                    .append("a.auction_id DESC ");
        } else {
            sql.append("ORDER BY a.auction_id DESC "); // default fallback
        }

        sql.append("LIMIT ? OFFSET ?");

        List<com.auction.core.dto.auction.PublicAuctionDto> result = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            for (String status : safeStatuses) {
                stmt.setString(paramIndex++, status);
            }

            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            if (safeStatuses.contains("ACTIVE")) {
                stmt.setTimestamp(paramIndex++, now);
            }
            if (safeStatuses.contains("PENDING")) {
                stmt.setTimestamp(paramIndex++, now);
            }

            stmt.setInt(paramIndex++, limit);
            stmt.setInt(paramIndex, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    com.auction.core.dto.auction.PublicAuctionDto dto =
                            new com.auction.core.dto.auction.PublicAuctionDto();
                    dto.setAuctionId(rs.getInt("auction_id"));
                    dto.setItemId(rs.getInt("item_id"));
                    dto.setItemName(rs.getString("item_name"));
                    dto.setThumbnailUrl(rs.getString("thumbnail_url"));
                    dto.setCurrentPrice(rs.getDouble("current_price"));
                    dto.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
                    dto.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
                    dto.setStatus(rs.getString("status"));
                    dto.setSellerDisplayName(rs.getString("seller_display_name"));
                    result.add(dto);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error: Cannot get public auctions! " + e.getMessage());
        }
        return result;
    }

    @Override
    public boolean promoteAuction(
            Integer auctionId, java.time.LocalDateTime featuredUntil, String promotedDescription) {
        String sql =
                "UPDATE auctions SET is_featured = true, featured_until = ?, promoted_description ="
                        + " ?, updated_at = ? WHERE auction_id = ? AND is_deleted = false";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(featuredUntil));
            stmt.setString(2, promotedDescription);
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(4, auctionId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error: Cannot promote auction! " + e.getMessage());
        }
        return false;
    }

    @Override
    public List<com.auction.core.dto.auction.PublicAuctionDto> getFeaturedAuctions(int limit) {
        // Dùng ORDER BY RAND() để xáo trộn ngẫu nhiên, giới hạn pool
        String sql =
                "SELECT a.auction_id, a.item_id, i.name as item_name, i.image_url as thumbnail_url,"
                    + " a.current_price, a.start_time, a.end_time, a.status, u.full_name as"
                    + " seller_display_name, a.is_featured, a.featured_until,"
                    + " a.promoted_description, i.description as item_description FROM auctions a"
                    + " JOIN items i ON a.item_id = i.item_id JOIN users u ON i.seller_id ="
                    + " u.user_id WHERE a.is_featured = true AND a.status = 'ACTIVE' AND a.end_time"
                    + " > NOW() AND a.is_deleted = false ORDER BY a.end_time ASC LIMIT ?";
        List<com.auction.core.dto.auction.PublicAuctionDto> result = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    com.auction.core.dto.auction.PublicAuctionDto dto =
                            new com.auction.core.dto.auction.PublicAuctionDto();
                    dto.setAuctionId(rs.getInt("auction_id"));
                    dto.setItemId(rs.getInt("item_id"));
                    dto.setItemName(rs.getString("item_name"));
                    dto.setThumbnailUrl(rs.getString("thumbnail_url"));
                    dto.setCurrentPrice(rs.getDouble("current_price"));
                    dto.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
                    dto.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
                    dto.setStatus(rs.getString("status"));
                    dto.setSellerDisplayName(rs.getString("seller_display_name"));
                    dto.setIsFeatured(rs.getBoolean("is_featured"));
                    Timestamp featuredUntil = rs.getTimestamp("featured_until");
                    if (featuredUntil != null) {
                        dto.setFeaturedUntil(featuredUntil.toLocalDateTime());
                    }
                    // Fallback: if promotedDescription is empty, use first 100 chars of item
                    // description
                    String promoted = rs.getString("promoted_description");
                    if (promoted == null || promoted.isBlank()) {
                        String itemDesc = rs.getString("item_description");
                        if (itemDesc != null && !itemDesc.isBlank()) {
                            promoted =
                                    itemDesc.length() > 100
                                            ? itemDesc.substring(0, 100) + "..."
                                            : itemDesc;
                        }
                    }
                    dto.setPromotedDescription(promoted);
                    result.add(dto);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error: Cannot get featured auctions! " + e.getMessage());
        }
        return result;
    }

    @Override
    public int resetExpiredFeaturedAuctions() {
        String sql =
                "UPDATE auctions SET is_featured = false, featured_until = NULL WHERE is_featured ="
                        + " true AND featured_until IS NOT NULL AND featured_until <= NOW()";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            int affected = stmt.executeUpdate();
            if (affected > 0) {
                System.out.println("[BatchJob] Reset " + affected + " expired featured auctions.");
            }
            return affected;
        } catch (SQLException e) {
            System.err.println("Error: Cannot reset expired featured auctions! " + e.getMessage());
        }
        return 0;
    }

    @Override
    public List<com.auction.core.dto.auction.PublicAuctionDto> getAllAuctionsForAdmin(
            List<String> statuses, int offset, int limit) {
        List<String> safeStatuses =
                (statuses == null || statuses.isEmpty())
                        ? List.of("ACTIVE", "PENDING", "ENDED", "CANCELLED")
                        : statuses;
        String statusPlaceholders =
                safeStatuses.stream()
                        .map(s -> "?")
                        .collect(java.util.stream.Collectors.joining(", "));
        String sql =
                "SELECT a.auction_id, a.item_id, i.name as item_name, i.image_url as thumbnail_url,"
                        + " a.current_price, a.start_time, a.end_time, a.status, u.full_name as"
                        + " seller_display_name, a.is_featured, a.featured_until,"
                        + " a.promoted_description FROM auctions a JOIN items i ON a.item_id ="
                        + " i.item_id JOIN users u ON i.seller_id = u.user_id WHERE a.status IN ("
                        + statusPlaceholders
                        + ") AND a.is_deleted = false "
                        + "ORDER BY a.auction_id DESC LIMIT ? OFFSET ?";
        List<com.auction.core.dto.auction.PublicAuctionDto> result = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            int idx = 1;
            for (String s : safeStatuses) {
                stmt.setString(idx++, s);
            }
            stmt.setInt(idx++, limit);
            stmt.setInt(idx, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    com.auction.core.dto.auction.PublicAuctionDto dto =
                            new com.auction.core.dto.auction.PublicAuctionDto();
                    dto.setAuctionId(rs.getInt("auction_id"));
                    dto.setItemId(rs.getInt("item_id"));
                    dto.setItemName(rs.getString("item_name"));
                    dto.setThumbnailUrl(rs.getString("thumbnail_url"));
                    dto.setCurrentPrice(rs.getDouble("current_price"));
                    dto.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
                    dto.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
                    dto.setStatus(rs.getString("status"));
                    dto.setSellerDisplayName(rs.getString("seller_display_name"));
                    dto.setIsFeatured(rs.getBoolean("is_featured"));
                    Timestamp featuredUntil = rs.getTimestamp("featured_until");
                    if (featuredUntil != null) {
                        dto.setFeaturedUntil(featuredUntil.toLocalDateTime());
                    }
                    dto.setPromotedDescription(rs.getString("promoted_description"));
                    result.add(dto);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error: Cannot get all auctions for admin! " + e.getMessage());
        }
        return result;
    }

    @Override
    public Auction getAuctionDetailsForUpdate(Connection conn, Integer auctionId)
            throws SQLException {
        String sql =
                "SELECT auction_id, item_id, starting_price, current_price, bid_increment,"
                        + " start_time, end_time, original_end_time, status, winner_id,"
                        + " snipe_threshold, snipe_extension, is_featured, featured_until,"
                        + " promoted_description FROM auctions WHERE auction_id = ? FOR UPDATE";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
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
                    auction.setOriginalEndTime(
                            rs.getTimestamp("original_end_time").toLocalDateTime());
                    String statusStr = rs.getString("status");
                    Auction.Status status = Auction.Status.PENDING;
                    if (statusStr != null) {
                        try {
                            status = Auction.Status.valueOf(statusStr.toUpperCase().trim());
                        } catch (IllegalArgumentException e) {
                            System.err.println(
                                    "[AuctionDao] Warning: Invalid status in DB (FOR UPDATE): '"
                                            + statusStr + "'. Falling back to PENDING.");
                        }
                    }
                    auction.setStatus(status);
                    int winnerId = rs.getInt("winner_id");
                    auction.setWinnerId(rs.wasNull() ? null : winnerId);
                    return auction;
                }
            }
        }
        return null;
    }

    @Override
    public boolean updateAuctionInformation(Connection conn, Auction auction) throws SQLException {
        String sql =
                "UPDATE auctions SET item_id = ?, starting_price = ?, current_price = ?,"
                        + " bid_increment = ?, start_time = ?, original_end_time = ?, end_time = ?,"
                        + " status = ?, winner_id = ?, updated_at = ?, snipe_threshold = ?,"
                        + " snipe_extension = ? WHERE auction_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
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
            return stmt.executeUpdate() > 0;
        }
    }

    @Override
    public boolean updateAuctionForBidWithConnection(Connection conn, Bid bid, Auction auction)
            throws SQLException {
        String sql = "UPDATE auctions SET current_price = ? WHERE auction_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, bid.getAmount());
            stmt.setInt(2, auction.getId());
            return stmt.executeUpdate() > 0;
        }
    }
}
