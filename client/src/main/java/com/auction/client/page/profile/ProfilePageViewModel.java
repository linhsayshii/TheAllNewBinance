package com.auction.client.page.profile;

import com.auction.client.dto.ProfileAuctionCardUiModel;
import com.auction.client.service.NetworkService;
import com.auction.client.service.UserSessionService;
import com.auction.core.auction.Auction;
import com.auction.core.auction.Bid;
import com.auction.core.dto.auction.GetAuctionBySellerIdRequest;
import com.auction.core.dto.auction.GetAuctionDetailsRequest;
import com.auction.core.dto.bid.GetBidByBidderIdRequest;
import com.auction.core.protocol.EventType;
import com.auction.core.users.User;
import com.auction.core.utils.JsonMapper;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * ViewModel for the User Profile page.
 *
 * <p>Handles two modes: — Personal (isPublicView = false): full data from UserSessionService. —
 * Public Seller View (isPublicView = true): fetches seller's public listings.
 */
public class ProfilePageViewModel {

    // ------------------------------------------------------------------ //
    //  Constants                                                           //
    // ------------------------------------------------------------------ //
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM yyyy");
    private static final DateTimeFormatter TIME_LEFT_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final long NETWORK_TIMEOUT_MS = 5_000;

    // ------------------------------------------------------------------ //
    //  Observable Properties (bound in controller)                        //
    // ------------------------------------------------------------------ //
    private final StringProperty displayName = new SimpleStringProperty("Guest");
    private final StringProperty userIdTag = new SimpleStringProperty("");
    private final StringProperty emailProperty = new SimpleStringProperty("");
    private final StringProperty joinDate = new SimpleStringProperty("");
    private final StringProperty avatarInitial = new SimpleStringProperty("?");

    private final DoubleProperty totalBalance = new SimpleDoubleProperty(0.0);
    private final DoubleProperty lockedBalance = new SimpleDoubleProperty(0.0);
    private final DoubleProperty availableBalance = new SimpleDoubleProperty(0.0);

    private final IntegerProperty activeBidsCount = new SimpleIntegerProperty(0);
    private final IntegerProperty activeListingsCount = new SimpleIntegerProperty(0);

    private final BooleanProperty isPublicView = new SimpleBooleanProperty(false);

    // ------------------------------------------------------------------ //
    //  Internal state                                                      //
    // ------------------------------------------------------------------ //
    /** userId of the profile being viewed (own or seller). */
    private int targetUserId = -1;

    /** My bids section data */
    private final List<ProfileAuctionCardUiModel> activeBids =
            Collections.synchronizedList(new ArrayList<>());

    private final List<ProfileAuctionCardUiModel> wonAuctions =
            Collections.synchronizedList(new ArrayList<>());

    /** My listings section data */
    private final List<ProfileAuctionCardUiModel> liveListings =
            Collections.synchronizedList(new ArrayList<>());

    private final List<ProfileAuctionCardUiModel> pendingListings =
            Collections.synchronizedList(new ArrayList<>());
    private final List<ProfileAuctionCardUiModel> soldListings =
            Collections.synchronizedList(new ArrayList<>());
    private final List<ProfileAuctionCardUiModel> unsoldListings =
            Collections.synchronizedList(new ArrayList<>());

    /** Seller stats */
    private final IntegerProperty totalListingsCount = new SimpleIntegerProperty(0);

    private final IntegerProperty soldListingsCount = new SimpleIntegerProperty(0);

    // ------------------------------------------------------------------ //
    //  Initialisation                                                      //
    // ------------------------------------------------------------------ //

    /** Call this to populate the ViewModel from the currently logged-in user. */
    public void loadFromSession() {
        User user = UserSessionService.getInstance().getCurrentUser();
        if (user == null) {
            return;
        }
        targetUserId = user.getId() != null ? user.getId() : -1;
        isPublicView.set(false);

        // Basic info
        String name = blankSafe(user.getFullName(), blankSafe(user.getUsername(), "User"));
        displayName.set(name);
        avatarInitial.set(name.isEmpty() ? "?" : String.valueOf(name.charAt(0)).toUpperCase());
        userIdTag.set(user.getId() != null ? "#" + user.getId() : "");
        emailProperty.set(blankSafe(user.getEmail(), ""));
        joinDate.set(
                user.getCreatedAt() != null
                        ? "Member since " + user.getCreatedAt().format(DATE_FORMAT)
                        : "");

        // Finance
        double balance = user.getBalance() != null ? user.getBalance() : 0.0;
        double locked = user.getLockedBalance() != null ? user.getLockedBalance() : 0.0;
        totalBalance.set(balance);
        lockedBalance.set(locked);
        availableBalance.set(balance - locked);
    }

    /** Call this when navigating to a public seller profile. */
    public void loadPublicSellerView(int sellerId) {
        targetUserId = sellerId;
        isPublicView.set(true);
        // Display name will be updated when listing data arrives
        displayName.set("Seller #" + sellerId);
        userIdTag.set("#" + sellerId);
        avatarInitial.set("S");
        emailProperty.set("");
        joinDate.set("");
    }

    // ------------------------------------------------------------------ //
    //  Network: My Bids                                                    //
    // ------------------------------------------------------------------ //

    /**
     * Fetches all bids placed by the current user, then resolves auction details for each unique
     * auctionId. Returns when all calls have completed or timed out. Must be called from a
     * background thread.
     */
    public void fetchMyBids() {
        if (targetUserId < 0) {
            return;
        }

        activeBids.clear();
        wonAuctions.clear();

        try {
            NetworkService ns = NetworkService.getInstance();
            if (!waitForSocket(ns)) {
                return;
            }

            // Step 1: get all bids by this bidder
            CountDownLatch bidsLatch = new CountDownLatch(1);
            List<Bid> rawBids = new ArrayList<>();

            GetBidByBidderIdRequest req = new GetBidByBidderIdRequest();
            req.setBidderId(targetUserId);
            String bidsCorr = ns.sendRequest(EventType.GET_BIDS_BY_BIDDER_ID, req);

            ns.addCorrelationHandler(
                    bidsCorr,
                    raw -> {
                        rawBids.addAll(parseBids(raw));
                        bidsLatch.countDown();
                    });

            bidsLatch.await(NETWORK_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            if (rawBids.isEmpty()) {
                return;
            }

            // Step 2: find unique auctionIds and fetch details in parallel
            Map<Integer, Bid> highestBidByAuction = new ConcurrentHashMap<>();
            for (Bid bid : rawBids) {
                if (bid.getAuctionId() == null) {
                    continue;
                }
                highestBidByAuction.merge(
                        bid.getAuctionId(),
                        bid,
                        (existing, incoming) ->
                                (incoming.getAmount() != null
                                                && existing.getAmount() != null
                                                && incoming.getAmount() > existing.getAmount())
                                        ? incoming
                                        : existing);
            }

            int uniqueCount = highestBidByAuction.size();
            if (uniqueCount == 0) {
                return;
            }

            CountDownLatch detailsLatch = new CountDownLatch(uniqueCount);
            AtomicInteger activeBidsCounter = new AtomicInteger(0);

            for (Map.Entry<Integer, Bid> entry : highestBidByAuction.entrySet()) {
                int auctionId = entry.getKey();
                Bid myBid = entry.getValue();

                String detailCorr =
                        ns.sendRequest(
                                EventType.GET_AUCTION_DETAILS,
                                new GetAuctionDetailsRequest(auctionId));

                ns.addCorrelationHandler(
                        detailCorr,
                        raw -> {
                            try {
                                Auction auction = parseAuction(raw);
                                if (auction != null) {
                                    ProfileAuctionCardUiModel card = buildBidCard(auction, myBid);
                                    if (card != null) {
                                        if ("badge-won".equals(card.badgeStyleClass())) {
                                            wonAuctions.add(card);
                                        } else {
                                            activeBids.add(card);
                                            activeBidsCounter.incrementAndGet();
                                        }
                                    }
                                }
                            } finally {
                                detailsLatch.countDown();
                            }
                        });
            }

            detailsLatch.await(NETWORK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            activeBidsCount.set(activeBidsCounter.get());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ------------------------------------------------------------------ //
    //  Network: My Listings                                               //
    // ------------------------------------------------------------------ //

    /**
     * Fetches all auctions created by the current user (as seller). Must be called from a
     * background thread.
     */
    public void fetchMyListings() {
        int sellerId = targetUserId;
        if (sellerId < 0) {
            return;
        }

        liveListings.clear();
        pendingListings.clear();
        soldListings.clear();
        unsoldListings.clear();

        try {
            NetworkService ns = NetworkService.getInstance();
            if (!waitForSocket(ns)) {
                return;
            }

            CountDownLatch latch = new CountDownLatch(1);
            List<Auction> auctions = new ArrayList<>();

            GetAuctionBySellerIdRequest req = new GetAuctionBySellerIdRequest(sellerId);
            String corr = ns.sendRequest(EventType.GET_AUCTIONS_BY_SELLER, req);

            ns.addCorrelationHandler(
                    corr,
                    raw -> {
                        auctions.addAll(parseAuctions(raw));
                        latch.countDown();
                    });

            latch.await(NETWORK_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            int sold = 0;
            for (Auction auction : auctions) {
                ProfileAuctionCardUiModel card = buildListingCard(auction);
                if (card == null) {
                    continue;
                }
                switch (card.badgeStyleClass()) {
                    case "badge-live" -> liveListings.add(card);
                    case "badge-pending" -> pendingListings.add(card);
                    case "badge-sold" -> {
                        soldListings.add(card);
                        sold++;
                    }
                    default -> unsoldListings.add(card);
                }
            }

            totalListingsCount.set(auctions.size());
            soldListingsCount.set(sold);
            activeListingsCount.set(liveListings.size() + pendingListings.size());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ------------------------------------------------------------------ //
    //  Card builders                                                       //
    // ------------------------------------------------------------------ //

    private ProfileAuctionCardUiModel buildBidCard(Auction auction, Bid myBid) {
        if (auction == null || myBid == null) {
            return null;
        }

        Integer auctionId = auction.getId();
        String title = "Auction #" + auctionId;
        String myBidFormatted =
                "$" + MONEY_FORMAT.format(myBid.getAmount() != null ? myBid.getAmount() : 0.0);

        Auction.Status status = auction.getStatus();

        // Won
        if (status == Auction.Status.ENDED
                && auction.getWinnerId() != null
                && auction.getWinnerId().equals(targetUserId)) {
            String finalPrice =
                    "$"
                            + MONEY_FORMAT.format(
                                    auction.getFinalPrice() != null
                                            ? auction.getFinalPrice()
                                            : 0.0);
            return ProfileAuctionCardUiModel.won(
                    auctionId, title, finalPrice, formatEnded(auction.getEndTime()));
        }

        // Ended but not won → treat as outbid/lost (we still show it in active bids briefly)
        if (status == Auction.Status.ENDED || status == Auction.Status.CANCELLED) {
            return null; // Omit ended auctions where user didn't win
        }

        // Active — check if winning or outbid
        double currentPrice = auction.getCurrentPrice() != null ? auction.getCurrentPrice() : 0.0;
        double myAmount = myBid.getAmount() != null ? myBid.getAmount() : 0.0;
        String timeInfo = formatTimeLeft(auction.getEndTime());

        if (myAmount >= currentPrice) {
            return ProfileAuctionCardUiModel.winning(auctionId, title, myBidFormatted, timeInfo);
        } else {
            return ProfileAuctionCardUiModel.outbid(auctionId, title, myBidFormatted, timeInfo);
        }
    }

    private ProfileAuctionCardUiModel buildListingCard(Auction auction) {
        if (auction == null) {
            return null;
        }

        Integer auctionId = auction.getId();
        String title = "Auction #" + auctionId;
        Auction.Status status = auction.getStatus();

        return switch (status) {
            case ACTIVE -> {
                String price =
                        "$"
                                + MONEY_FORMAT.format(
                                        auction.getCurrentPrice() != null
                                                ? auction.getCurrentPrice()
                                                : 0.0);
                boolean featured = Boolean.TRUE.equals(auction.getIsFeatured());
                yield ProfileAuctionCardUiModel.live(
                        auctionId, title, price, formatTimeLeft(auction.getEndTime()), featured);
            }
            case PENDING -> {
                String price =
                        "$"
                                + MONEY_FORMAT.format(
                                        auction.getStartingPrice() != null
                                                ? auction.getStartingPrice()
                                                : 0.0);
                yield ProfileAuctionCardUiModel.pending(
                        auctionId, title, price, formatUpcomingStart(auction.getStartTime()));
            }
            case ENDED -> {
                boolean hasBuyer = auction.getWinnerId() != null && auction.getWinnerId() > 0;
                if (hasBuyer) {
                    String finalPrice =
                            "$"
                                    + MONEY_FORMAT.format(
                                            auction.getFinalPrice() != null
                                                    ? auction.getFinalPrice()
                                                    : 0.0);
                    yield ProfileAuctionCardUiModel.sold(
                            auctionId, title, finalPrice, formatEnded(auction.getEndTime()));
                } else {
                    String price =
                            "$"
                                    + MONEY_FORMAT.format(
                                            auction.getStartingPrice() != null
                                                    ? auction.getStartingPrice()
                                                    : 0.0);
                    yield ProfileAuctionCardUiModel.unsold(
                            auctionId, title, price, formatEnded(auction.getEndTime()));
                }
            }
            case CANCELLED -> {
                String price =
                        "$"
                                + MONEY_FORMAT.format(
                                        auction.getStartingPrice() != null
                                                ? auction.getStartingPrice()
                                                : 0.0);
                yield new ProfileAuctionCardUiModel(
                        auctionId,
                        title,
                        "Opening bid",
                        price,
                        formatEnded(auction.getEndTime()),
                        "CANCELLED",
                        "badge-cancelled",
                        false,
                        null);
            }
        };
    }

    // ------------------------------------------------------------------ //
    //  JSON Parsing helpers                                                //
    // ------------------------------------------------------------------ //

    @SuppressWarnings("unchecked")
    private List<Bid> parseBids(String raw) {
        try {
            Map<?, ?> response = JsonMapper.fromJson(raw, Map.class);
            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                return List.of();
            }
            Object data = response.get("data");
            if (!(data instanceof List)) {
                return List.of();
            }
            String dataJson = JsonMapper.toJson(data);
            Bid[] bids = JsonMapper.fromJson(dataJson, Bid[].class);
            return bids != null ? java.util.Arrays.asList(bids) : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    private Auction parseAuction(String raw) {
        try {
            Map<?, ?> response = JsonMapper.fromJson(raw, Map.class);
            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                return null;
            }
            Object data = response.get("data");
            if (data == null) {
                return null;
            }
            return JsonMapper.fromJson(JsonMapper.toJson(data), Auction.class);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Auction> parseAuctions(String raw) {
        try {
            Map<?, ?> response = JsonMapper.fromJson(raw, Map.class);
            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                return List.of();
            }
            Object data = response.get("data");
            if (!(data instanceof List)) {
                return List.of();
            }
            String dataJson = JsonMapper.toJson(data);
            Auction[] auctions = JsonMapper.fromJson(dataJson, Auction[].class);
            return auctions != null ? java.util.Arrays.asList(auctions) : List.of();
        } catch (Exception e) {
            return List.of();
        }
    }

    // ------------------------------------------------------------------ //
    //  Formatting helpers                                                  //
    // ------------------------------------------------------------------ //

    private String formatTimeLeft(LocalDateTime endTime) {
        if (endTime == null) {
            return "N/A";
        }
        java.time.Duration remaining = java.time.Duration.between(LocalDateTime.now(), endTime);
        if (remaining.isNegative() || remaining.isZero()) {
            return "Ended";
        }
        long totalMinutes = remaining.toMinutes();
        long days = totalMinutes / (24 * 60);
        long hours = (totalMinutes % (24 * 60)) / 60;
        long minutes = totalMinutes % 60;
        if (days > 0) {
            return days + "d " + hours + "h left";
        }
        return String.format("%02dh %02dm left", hours, minutes);
    }

    private String formatUpcomingStart(LocalDateTime startTime) {
        if (startTime == null) {
            return "N/A";
        }
        if (startTime.isBefore(LocalDateTime.now())) {
            return "Starting soon";
        }
        return "Starts " + startTime.format(TIME_LEFT_FORMAT);
    }

    private String formatEnded(LocalDateTime endTime) {
        if (endTime == null) {
            return "Ended";
        }
        return "Ended " + endTime.format(DateTimeFormatter.ofPattern("dd MMM"));
    }

    private boolean waitForSocket(NetworkService ns) {
        long waited = 0;
        while (!ns.getClient().isOpen() && waited < NETWORK_TIMEOUT_MS) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            waited += 100;
        }
        return ns.getClient().isOpen();
    }

    private static String blankSafe(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    // ------------------------------------------------------------------ //
    //  Property accessors                                                  //
    // ------------------------------------------------------------------ //

    public StringProperty displayNameProperty() {
        return displayName;
    }

    public StringProperty userIdTagProperty() {
        return userIdTag;
    }

    public StringProperty emailProperty() {
        return emailProperty;
    }

    public StringProperty joinDateProperty() {
        return joinDate;
    }

    public StringProperty avatarInitialProperty() {
        return avatarInitial;
    }

    public DoubleProperty totalBalanceProperty() {
        return totalBalance;
    }

    public DoubleProperty lockedBalanceProperty() {
        return lockedBalance;
    }

    public DoubleProperty availableBalanceProperty() {
        return availableBalance;
    }

    public IntegerProperty activeBidsCountProperty() {
        return activeBidsCount;
    }

    public IntegerProperty activeListingsCountProperty() {
        return activeListingsCount;
    }

    public IntegerProperty totalListingsCountProperty() {
        return totalListingsCount;
    }

    public IntegerProperty soldListingsCountProperty() {
        return soldListingsCount;
    }

    public BooleanProperty isPublicViewProperty() {
        return isPublicView;
    }

    public boolean isPublicView() {
        return isPublicView.get();
    }

    public int getTargetUserId() {
        return targetUserId;
    }

    /** Formatted "$12,500.00" strings for UI labels. */
    public String getFormattedTotalBalance() {
        return "$" + MONEY_FORMAT.format(totalBalance.get());
    }

    public String getFormattedLockedBalance() {
        return "$" + MONEY_FORMAT.format(lockedBalance.get());
    }

    public String getFormattedAvailableBalance() {
        return "$" + MONEY_FORMAT.format(availableBalance.get());
    }

    /** Seller success rate as "75%" string. */
    public String getSellerSuccessRate() {
        int total = totalListingsCount.get();
        if (total == 0) {
            return "—";
        }
        return Math.round(100.0 * soldListingsCount.get() / total) + "%";
    }

    /** Lists are returned as unmodifiable snapshots for thread safety. */
    public List<ProfileAuctionCardUiModel> getActiveBids() {
        return List.copyOf(activeBids);
    }

    public List<ProfileAuctionCardUiModel> getWonAuctions() {
        return List.copyOf(wonAuctions);
    }

    public List<ProfileAuctionCardUiModel> getLiveListings() {
        return List.copyOf(liveListings);
    }

    public List<ProfileAuctionCardUiModel> getPendingListings() {
        return List.copyOf(pendingListings);
    }

    public List<ProfileAuctionCardUiModel> getSoldListings() {
        return List.copyOf(soldListings);
    }

    public List<ProfileAuctionCardUiModel> getUnsoldListings() {
        return List.copyOf(unsoldListings);
    }
}
