package com.auction.core.dto.auction;

/**
 * Request DTO để lấy danh sách Auctions đang được Featured (Star Auction Carousel). Trả về tối đa 5
 * kết quả ACTIVE + isFeatured = true, xáo trộn ngẫu nhiên.
 */
public class GetFeaturedAuctionsRequest {
    private int limit = 5; // Hard-coded pool limit

    public GetFeaturedAuctionsRequest() {}

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
