package com.auction.server.dao.impl;

import java.util.List;

import com.auction.core.auction.Auction;
import com.auction.core.auction.Bid;
import com.auction.core.dto.auction.PublicAuctionDto;

public interface IAuctionDao {
    boolean createAuction(Auction auction);
    boolean updateAuctionInformation(Auction auction);
    boolean deleteAuction(Auction auction);
    boolean extendAuction(Auction auction);
    Auction getAuctionDetails(Integer auctionId);
    double getCurrentPrice(Integer auctionId);
    void updateCurrentPrice(Bid bid);
    boolean updateAuctionForBid(Bid bid, Auction auction);
    Integer getSellerId(Integer auctionId);
    List<Auction> getAuctionsBySellerId(Integer sellerId);
    List<PublicAuctionDto> getPublicAuctions(
            int offset,
            int limit,
            List<String> statuses,
            boolean includeEndingSoon,
            boolean includeTrending);

    /** Cập nhật isFeatured, featuredUntil, promotedDescription cho một auction. */
    boolean promoteAuction(Integer auctionId, java.time.LocalDateTime featuredUntil, String promotedDescription);

    /** Lấy danh sách auctions đang isFeatured=true và status=ACTIVE (ngẫu nhiên, giới hạn). */
    List<PublicAuctionDto> getFeaturedAuctions(int limit);

    /** Reset isFeatured=false cho các auction hết hạn. Gọi bởi Batch Job lúc 00:00 hàng ngày. */
    int resetExpiredFeaturedAuctions();

    /** Admin: Lấy tất cả auctions theo status (không giới hạn seller). */
    List<PublicAuctionDto> getAllAuctionsForAdmin(List<String> statuses, int offset, int limit);
}
