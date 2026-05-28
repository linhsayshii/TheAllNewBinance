package com.auction.server.dao.impl;

import com.auction.core.auction.Bid;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public interface IBidDao {
    boolean saveBid(Bid bid);

    /**
     * Lưu Bid dùng chung Connection của Transaction đang hoạt động. Tránh tạo kết nối mới gây
     * Deadlock với khóa bi quan FOR UPDATE của luồng chính.
     */
    boolean saveBid(Connection conn, Bid bid) throws SQLException;

    List<Bid> findByAuctionId(Integer auctionId);

    /**
     * Lấy danh sách Bid dùng chung Connection của Transaction đang hoạt động. Đảm bảo Repeatable
     * Read nhất quán, tránh Phantom Read với Late Bids giây cuối.
     */
    List<Bid> findByAuctionId(Connection conn, Integer auctionId) throws SQLException;

    List<Bid> findByBidderId(Integer bidderId);

    boolean hasBid(Integer auctionId, Integer bidderId);

    /**
     * Kiểm tra Bid dùng chung Connection của Transaction đang hoạt động. Tránh race condition đặt
     * cọc khi nhiều thầu đến cùng lúc.
     */
    boolean hasBid(Connection conn, Integer auctionId, Integer bidderId) throws SQLException;
}
