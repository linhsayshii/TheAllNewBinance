package com.auction.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.auction.core.auction.Bid;
import com.auction.core.dao.IBidDao;

public class BidDao implements IBidDao {
    @Override
    public boolean saveBid(Bid bid) {
        String sql = "INSERT INTO bids (auction_id, bidder_id, amount, created_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, bid.getAuctionId());
            stmt.setInt(2, bid.getBidderId());
            stmt.setDouble(3, bid.getAmount());
            stmt.setTimestamp(4, Timestamp.valueOf(bid.getCreatedAt()));
            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        bid.setId(generatedKeys.getInt("bid_id"));
                    }
                }
            }
            return true;
        } catch (SQLException e) {
            System.err.println("Error: Cannot save Bid!" + e.getMessage());
        }
        return false;
    }

    @Override
    public List<Bid> findByBidderId(Integer bidderId) {
        String sql = "SELECT * FROM bids WHERE bidder_id = ? ORDER BY amount DESC";
        List<Bid> bids = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, bidderId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Bid bid = new Bid(
                        rs.getInt("bid_id"),
                        rs.getInt("auction_id"),
                        rs.getInt("bidder_id"),
                        rs.getDouble("amount")
                    );
                    bids.add(bid);  
                }
            }
        } catch (SQLException e) {
            System.err.println("Error: Cannot find Bids!" + e.getMessage());
        }
        return bids;
    }

    @Override
    public List<Bid> findByAuctionId(Integer auctionId) {
        String sql = "SELECT * FROM bids WHERE auction_id = ? ORDER BY amount DESC";
        List<Bid> bids = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, auctionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Bid bid = new Bid(
                        rs.getInt("bid_id"),
                        rs.getInt("auction_id"),
                        rs.getInt("bidder_id"),
                        rs.getDouble("amount")
                    );
                    bids.add(bid);  
                }
            }
        } catch (SQLException e) {
            System.err.println("Error: Cannot find Bids!" + e.getMessage());
        }
        return bids;
    }
}
