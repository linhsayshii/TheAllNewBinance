package com.auction.core.protocol;

public enum EventType {
    // User
    LOGIN,
    REGISTER,
    UPDATE_PROFILE,
    CHANGE_PASSWORD,
    LOGOUT,

    // Auction
    GET_AUCTION_DETAILS,
    CREATE_AUCTION,
    GET_AUCTIONS_BY_SELLER,
    GET_PUBLIC_AUCTIONS,
    PROMOTE_AUCTION, // User/Admin: promote listing lên Star Auction
    GET_FEATURED_AUCTIONS, // Anonymous: lấy danh sách Star Auction cho Carousel
    GET_ALL_AUCTIONS_ADMIN, // Admin only: lấy tất cả auctions theo status
    GET_ALL_USERS_ADMIN, // Admin only: lấy tất cả users
    SUBSCRIBE_AUCTION, // Client: đăng ký nhận broadcast của phiên đấu giá
    UNSUBSCRIBE_AUCTION, // Client: hủy đăng ký nhận broadcast của phiên đấu giá

    // Item
    GET_UPLOAD_SIGNATURE,

    // Bid
    PLACE_BID,
    GET_BIDS_BY_AUCTION_ID,
    GET_BIDS_BY_BIDDER_ID;

    public String wireValue() {
        return name();
    }

    public static EventType fromWireValue(Object raw) {
        if (raw == null) {
            return null;
        }
        try {
            return EventType.valueOf(String.valueOf(raw).trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
