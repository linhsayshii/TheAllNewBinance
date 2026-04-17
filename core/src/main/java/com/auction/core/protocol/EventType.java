package com.auction.core.protocol;

public enum EventType {
    // User
    LOGIN,
    REGISTER,
    UPDATE_PROFILE,
    CHANGE_PASSWORD,

    // Auction
    GET_AUCTION_DETAILS,
    CREATE_AUCTION,
    GET_AUCTIONS_BY_SELLER,

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
