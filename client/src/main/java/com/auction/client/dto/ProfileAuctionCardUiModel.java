package com.auction.client.dto;

/**
 * UI model for the compact ProfileAuctionCard component used in the User Profile page (My Bids, My
 * Listings, and Public Seller View sections).
 *
 * <p>Unlike {@link ProductCardUiModel} which is designed for the wide carousel cards on the General
 * page, this record is tuned for compact profile cards: — no progress bar — status badge with
 * human-readable label and CSS style class — priceCaption changes depending on context ("Current
 * bid" vs "Final price")
 */
public record ProfileAuctionCardUiModel(
        Integer auctionId,
        String title,
        String priceCaption, // "Current bid" | "Final price"
        String price, // "$12,600.00"
        String timeInfo, // "02h 31m left" | "Ended Apr 20" | "Starts 12/05 09:00"
        String badgeLabel, // "WINNING" | "OUTBID" | "WON" | "LIVE" | "PENDING" | "SOLD" | "UNSOLD"
        String badgeStyleClass, // "badge-winning" | "badge-outbid" | etc.
        boolean isFeatured, // true nếu đang được promote lên Star Auction
        String imageUrl) {

    /** Convenience factory for an active bid the user is currently winning. */
    public static ProfileAuctionCardUiModel winning(
            Integer auctionId, String title, String price, String timeInfo, String imageUrl) {
        return new ProfileAuctionCardUiModel(
                auctionId,
                title,
                "Current bid",
                price,
                timeInfo,
                "WINNING",
                "badge-winning",
                false,
                imageUrl);
    }

    /** Convenience factory for a bid where the user has been outbid. */
    public static ProfileAuctionCardUiModel outbid(
            Integer auctionId, String title, String price, String timeInfo, String imageUrl) {
        return new ProfileAuctionCardUiModel(
                auctionId,
                title,
                "Current bid",
                price,
                timeInfo,
                "OUTBID",
                "badge-outbid",
                false,
                imageUrl);
    }

    /** Convenience factory for an auction the user has won. */
    public static ProfileAuctionCardUiModel won(
            Integer auctionId, String title, String finalPrice, String endedInfo, String imageUrl) {
        return new ProfileAuctionCardUiModel(
                auctionId,
                title,
                "Final price",
                finalPrice,
                endedInfo,
                "WON",
                "badge-won",
                false,
                imageUrl);
    }

    /** Convenience factory for a seller's active/live listing. */
    public static ProfileAuctionCardUiModel live(
            Integer auctionId,
            String title,
            String price,
            String timeInfo,
            boolean isFeatured,
            String imageUrl) {
        return new ProfileAuctionCardUiModel(
                auctionId,
                title,
                "Current bid",
                price,
                timeInfo,
                "LIVE",
                "badge-live",
                isFeatured,
                imageUrl);
    }

    /** Convenience factory for a seller's pending listing. */
    public static ProfileAuctionCardUiModel pending(
            Integer auctionId, String title, String price, String startInfo, String imageUrl) {
        return new ProfileAuctionCardUiModel(
                auctionId,
                title,
                "Opening bid",
                price,
                startInfo,
                "PENDING",
                "badge-pending",
                false,
                imageUrl);
    }

    /** Convenience factory for a seller's sold listing. */
    public static ProfileAuctionCardUiModel sold(
            Integer auctionId, String title, String finalPrice, String endedInfo, String imageUrl) {
        return new ProfileAuctionCardUiModel(
                auctionId,
                title,
                "Final price",
                finalPrice,
                endedInfo,
                "SOLD",
                "badge-sold",
                false,
                imageUrl);
    }

    /** Convenience factory for a seller's unsold/cancelled listing. */
    public static ProfileAuctionCardUiModel unsold(
            Integer auctionId, String title, String price, String endedInfo, String imageUrl) {
        return new ProfileAuctionCardUiModel(
                auctionId,
                title,
                "Opening bid",
                price,
                endedInfo,
                "UNSOLD",
                "badge-unsold",
                false,
                imageUrl);
    }
}
