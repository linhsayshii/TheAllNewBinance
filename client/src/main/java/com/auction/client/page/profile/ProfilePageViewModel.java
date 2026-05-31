package com.auction.client.page.profile;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import com.auction.client.dto.ProfileAuctionCardUiModel;
import com.auction.client.service.NetworkService;
import com.auction.client.service.TimeSyncService;
import com.auction.client.service.UserSessionService;
import com.auction.core.auction.Auction;
import com.auction.core.auction.Bid;
import com.auction.core.dto.auction.GetAuctionBySellerIdRequest;
import com.auction.core.dto.auction.GetAuctionDetailsRequest;
import com.auction.core.dto.bid.GetBidByBidderIdRequest;
import com.auction.core.protocol.EventType;
import com.auction.core.users.User;
import com.auction.core.utils.JsonMapper;

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
    //  Wallet Transaction History                                          //
    // ------------------------------------------------------------------ //

    /**
     * Lightweight display model for a single wallet transaction row.
     *
     * @param isDeposit true for DEPOSIT, false for WITHDRAW
     * @param formattedAmount formatted dollar string, e.g. "$500.00"
     * @param date human-readable date string
     */
    public record TransactionRow(boolean isDeposit, String formattedAmount, String date) {}

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

        // Finance — balance in DB is already the Available Balance.
        // Total = balance (available) + lockedBalance (frozen in auctions).
        double available = user.getBalance() != null ? user.getBalance().doubleValue() : 0.0;
        double locked =
                user.getLockedBalance() != null ? user.getLockedBalance().doubleValue() : 0.0;
        availableBalance.set(available);
        lockedBalance.set(locked);
        totalBalance.set(available + locked);
    }

    /** Call this when navigating to a public seller profile. */
    public void loadPublicSellerView(int sellerId, String name, String email, String joinDateVal) {
        targetUserId = sellerId;
        isPublicView.set(true);
        String finalName = blankSafe(name, "Seller #" + sellerId);
        displayName.set(finalName);
        userIdTag.set("#" + sellerId);
        avatarInitial.set(
                finalName.isEmpty() ? "S" : String.valueOf(finalName.charAt(0)).toUpperCase());
        emailProperty.set(blankSafe(email, ""));
        joinDate.set(blankSafe(joinDateVal, ""));
    }

    /**
     * Queries the server for the wallet_transactions of the current user. Must be called from a
     * background thread. Returns an empty list if the server is unreachable, times out, or the
     * feature is not yet enabled on the server (graceful degradation).
     */
    public java.util.List<TransactionRow> fetchTransactionHistory() {
        if (targetUserId < 0) {
            return java.util.List.of();
        }
        try {
            NetworkService ns = NetworkService.getInstance();
            if (!waitForSocket(ns)) {
                return java.util.List.of();
            }

            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.atomic.AtomicReference<String> ref =
                    new java.util.concurrent.atomic.AtomicReference<>();

            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("userId", targetUserId);
            String corr =
                    ns.sendRequest(
                            com.auction.core.protocol.EventType.GET_WALLET_TRANSACTIONS, payload);
            try {
                ns.addCorrelationHandler(
                        corr,
                        raw -> {
                            ref.set(raw);
                            latch.countDown();
                        });

                latch.await(NETWORK_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS);
            } finally {
                ns.removeCorrelationHandler(corr);
            }

            String raw = ref.get();
            if (raw == null) {
                return java.util.List.of();
            }
            return parseTransactionRows(raw);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return java.util.List.of();
        } catch (Exception e) {
            return java.util.List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private java.util.List<TransactionRow> parseTransactionRows(String raw) {
        try {
            Map<?, ?> response = JsonMapper.fromJson(raw, Map.class);
            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                return java.util.List.of();
            }
            Object data = response.get("data");
            if (!(data instanceof java.util.List<?> rows)) {
                return java.util.List.of();
            }
            java.util.List<TransactionRow> result = new java.util.ArrayList<>();
            java.text.DecimalFormat fmt = new java.text.DecimalFormat("$#,##0.00");
            java.time.format.DateTimeFormatter dtf =
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            for (Object item : rows) {
                if (!(item instanceof Map<?, ?> m)) {
                    continue;
                }
                Object typeVal = m.get("transactionType");
                String typeStr = typeVal != null ? String.valueOf(typeVal) : "";
                boolean isDeposit = "DEPOSIT".equalsIgnoreCase(typeStr);
                if (!isDeposit && !"WITHDRAW".equalsIgnoreCase(typeStr)) {
                    continue;
                }
                double rawAmount = 0.0;
                Object amtObj = m.get("amount");
                if (amtObj instanceof Number n) {
                    rawAmount = n.doubleValue();
                } else if (amtObj != null) {
                    try {
                        rawAmount = Double.parseDouble(String.valueOf(amtObj));
                    } catch (NumberFormatException ignored) {
                    }
                }
                String amountStr = fmt.format(rawAmount);
                String dateStr = "";
                Object createdAt = m.get("createdAt");
                if (createdAt != null) {
                    try {
                        java.time.LocalDateTime ldt =
                                java.time.LocalDateTime.parse(
                                        String.valueOf(createdAt).replace(' ', 'T'));
                        dateStr = ldt.format(dtf);
                    } catch (Exception ignored) {
                        dateStr = String.valueOf(createdAt);
                    }
                }
                result.add(new TransactionRow(isDeposit, amountStr, dateStr));
            }
            return result;
        } catch (Exception e) {
            return java.util.List.of();
        }
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

        try {
            NetworkService ns = NetworkService.getInstance();
            if (!waitForSocket(ns)) {
                return;
            }

            GetBidByBidderIdRequest req = new GetBidByBidderIdRequest();
            req.setBidderId(targetUserId);
            String rawBidsJson = ns.sendRequestAsync(EventType.GET_BIDS_BY_BIDDER_ID, req).join();
            List<Bid> rawBids = parseBids(rawBidsJson);
            if (rawBids.isEmpty()) {
                synchronized (activeBids) {
                    activeBids.clear();
                }
                synchronized (wonAuctions) {
                    wonAuctions.clear();
                }
                javafx.application.Platform.runLater(() -> activeBidsCount.set(0));
                return;
            }

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

            if (highestBidByAuction.isEmpty()) {
                synchronized (activeBids) {
                    activeBids.clear();
                }
                synchronized (wonAuctions) {
                    wonAuctions.clear();
                }
                javafx.application.Platform.runLater(() -> activeBidsCount.set(0));
                return;
            }

            // Gom kết quả vào danh sách cục bộ để tránh race condition với các phiên fetch song song
            List<ProfileAuctionCardUiModel> localActiveBids =
                    Collections.synchronizedList(new ArrayList<>());
            List<ProfileAuctionCardUiModel> localWonAuctions =
                    Collections.synchronizedList(new ArrayList<>());
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            java.util.concurrent.atomic.AtomicInteger activeBidsCounter =
                    new java.util.concurrent.atomic.AtomicInteger(0);

            for (Map.Entry<Integer, Bid> entry : highestBidByAuction.entrySet()) {
                Bid myBid = entry.getValue();
                CompletableFuture<Void> future =
                        ns.sendRequestAsync(
                                        EventType.GET_AUCTION_DETAILS,
                                        new GetAuctionDetailsRequest(entry.getKey()))
                                .thenAccept(
                                        detailsRaw -> {
                                            com.auction.core.dto.auction.AuctionDetailsDto details =
                                                    parseAuctionDetails(detailsRaw);
                                            if (details != null && details.getAuction() != null) {
                                                Auction auction = details.getAuction();
                                                com.auction.core.products.Item item =
                                                        details.getItem();
                                                ProfileAuctionCardUiModel card =
                                                        buildBidCard(auction, item, myBid);
                                                if (card != null) {
                                                    if ("badge-won"
                                                            .equals(card.badgeStyleClass())) {
                                                        localWonAuctions.add(card);
                                                    } else {
                                                        localActiveBids.add(card);
                                                        activeBidsCounter.incrementAndGet();
                                                    }
                                                }
                                            }
                                        });
                futures.add(future);
            }

            // Đợi tất cả request hoàn thành rồi mới cập nhật nguyên tử danh sách chính
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            synchronized (activeBids) {
                activeBids.clear();
                activeBids.addAll(localActiveBids);
            }
            synchronized (wonAuctions) {
                wonAuctions.clear();
                wonAuctions.addAll(localWonAuctions);
            }
            int finalCount = activeBidsCounter.get();
            javafx.application.Platform.runLater(() -> activeBidsCount.set(finalCount));

        } catch (Exception e) {
            System.err.println("Error during fetchMyBids: " + e.getMessage());
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

        try {
            NetworkService ns = NetworkService.getInstance();
            if (!waitForSocket(ns)) {
                return;
            }

            GetAuctionBySellerIdRequest req = new GetAuctionBySellerIdRequest(sellerId);
            String raw = ns.sendRequestAsync(EventType.GET_AUCTIONS_BY_SELLER, req).join();
            List<Auction> auctions = parseAuctions(raw);
            if (auctions.isEmpty()) {
                synchronized (liveListings) {
                    liveListings.clear();
                }
                synchronized (pendingListings) {
                    pendingListings.clear();
                }
                synchronized (soldListings) {
                    soldListings.clear();
                }
                synchronized (unsoldListings) {
                    unsoldListings.clear();
                }
                javafx.application.Platform.runLater(() -> {
                    totalListingsCount.set(0);
                    soldListingsCount.set(0);
                    activeListingsCount.set(0);
                });
                return;
            }

            int size = auctions.size();
            // Gom kết quả vào danh sách cục bộ để tránh race condition
            List<ProfileAuctionCardUiModel> localLiveListings =
                    Collections.synchronizedList(new ArrayList<>());
            List<ProfileAuctionCardUiModel> localPendingListings =
                    Collections.synchronizedList(new ArrayList<>());
            List<ProfileAuctionCardUiModel> localSoldListings =
                    Collections.synchronizedList(new ArrayList<>());
            List<ProfileAuctionCardUiModel> localUnsoldListings =
                    Collections.synchronizedList(new ArrayList<>());
            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (Auction auction : auctions) {
                CompletableFuture<Void> future =
                        ns.sendRequestAsync(
                                        EventType.GET_AUCTION_DETAILS,
                                        new GetAuctionDetailsRequest(auction.getId()))
                                .thenAccept(
                                        detailsRaw -> {
                                            com.auction.core.dto.auction.AuctionDetailsDto details =
                                                    parseAuctionDetails(detailsRaw);
                                            if (details != null && details.getAuction() != null) {
                                                Auction fullAuction = details.getAuction();
                                                com.auction.core.products.Item item =
                                                        details.getItem();
                                                ProfileAuctionCardUiModel card =
                                                        buildListingCard(fullAuction, item);
                                                if (card != null) {
                                                    switch (card.badgeStyleClass()) {
                                                        case "badge-live" ->
                                                            localLiveListings.add(card);
                                                        case "badge-pending" ->
                                                            localPendingListings.add(card);
                                                        case "badge-sold" ->
                                                            localSoldListings.add(card);
                                                        default -> localUnsoldListings.add(card);
                                                    }
                                                }
                                            }
                                        });
                futures.add(future);
            }

            // Đợi tất cả request hoàn thành rồi mới cập nhật nguyên tử danh sách chính
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            synchronized (liveListings) {
                liveListings.clear();
                liveListings.addAll(localLiveListings);
            }
            synchronized (pendingListings) {
                pendingListings.clear();
                pendingListings.addAll(localPendingListings);
            }
            synchronized (soldListings) {
                soldListings.clear();
                soldListings.addAll(localSoldListings);
            }
            synchronized (unsoldListings) {
                unsoldListings.clear();
                unsoldListings.addAll(localUnsoldListings);
            }
            int sold = localSoldListings.size();
            int live = localLiveListings.size();
            int pending = localPendingListings.size();
            int total = size;
            javafx.application.Platform.runLater(() -> {
                totalListingsCount.set(total);
                soldListingsCount.set(sold);
                activeListingsCount.set(live + pending);
            });

        } catch (Exception e) {
            System.err.println("Error during fetchMyListings: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ //
    //  Card builders                                                       //
    // ------------------------------------------------------------------ //

    private ProfileAuctionCardUiModel buildBidCard(
            Auction auction, com.auction.core.products.Item item, Bid myBid) {
        if (auction == null || myBid == null) {
            return null;
        }

        Integer auctionId = auction.getId();
        String title =
                item != null && item.getName() != null ? item.getName() : "Auction #" + auctionId;
        String imageUrl = item != null ? item.getImageUrl() : null;
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
                    auctionId, title, finalPrice, formatEnded(auction.getEndTime()), imageUrl);
        }

        // Ended but not won — omit from active bids
        if (status == Auction.Status.ENDED || status == Auction.Status.CANCELLED) {
            return null;
        }

        // Active — check if winning or outbid
        double currentPrice = auction.getCurrentPrice() != null ? auction.getCurrentPrice() : 0.0;
        double myAmount = myBid.getAmount() != null ? myBid.getAmount() : 0.0;
        String timeInfo = formatTimeLeft(auction.getEndTime());

        if (myAmount >= currentPrice) {
            return ProfileAuctionCardUiModel.winning(
                    auctionId, title, myBidFormatted, timeInfo, imageUrl);
        } else {
            return ProfileAuctionCardUiModel.outbid(
                    auctionId, title, myBidFormatted, timeInfo, imageUrl);
        }
    }

    private ProfileAuctionCardUiModel buildListingCard(
            Auction auction, com.auction.core.products.Item item) {
        if (auction == null) {
            return null;
        }

        Integer auctionId = auction.getId();
        String title =
                item != null && item.getName() != null ? item.getName() : "Auction #" + auctionId;
        String imageUrl = item != null ? item.getImageUrl() : null;
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
                        auctionId,
                        title,
                        price,
                        formatTimeLeft(auction.getEndTime()),
                        featured,
                        imageUrl);
            }
            case PENDING -> {
                String price =
                        "$"
                                + MONEY_FORMAT.format(
                                        auction.getStartingPrice() != null
                                                ? auction.getStartingPrice()
                                                : 0.0);
                yield ProfileAuctionCardUiModel.pending(
                        auctionId,
                        title,
                        price,
                        formatUpcomingStart(auction.getStartTime()),
                        imageUrl);
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
                            auctionId,
                            title,
                            finalPrice,
                            formatEnded(auction.getEndTime()),
                            imageUrl);
                } else {
                    String price =
                            "$"
                                    + MONEY_FORMAT.format(
                                            auction.getStartingPrice() != null
                                                    ? auction.getStartingPrice()
                                                    : 0.0);
                    yield ProfileAuctionCardUiModel.unsold(
                            auctionId, title, price, formatEnded(auction.getEndTime()), imageUrl);
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
                        imageUrl);
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

    private com.auction.core.dto.auction.AuctionDetailsDto parseAuctionDetails(String raw) {
        try {
            Map<?, ?> response = JsonMapper.fromJson(raw, Map.class);
            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                return null;
            }
            Object data = response.get("data");
            if (data == null) {
                return null;
            }
            return JsonMapper.fromJson(
                    JsonMapper.toJson(data), com.auction.core.dto.auction.AuctionDetailsDto.class);
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
        java.time.Duration remaining = java.time.Duration.between(TimeSyncService.getNow(), endTime);
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
        if (startTime.isBefore(TimeSyncService.getNow())) {
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

    /**
     * Lists are returned as unmodifiable snapshots for thread safety with external synchronization.
     */
    public List<ProfileAuctionCardUiModel> getActiveBids() {
        synchronized (activeBids) {
            return List.copyOf(activeBids);
        }
    }

    public List<ProfileAuctionCardUiModel> getWonAuctions() {
        synchronized (wonAuctions) {
            return List.copyOf(wonAuctions);
        }
    }

    public List<ProfileAuctionCardUiModel> getLiveListings() {
        synchronized (liveListings) {
            return List.copyOf(liveListings);
        }
    }

    public List<ProfileAuctionCardUiModel> getPendingListings() {
        synchronized (pendingListings) {
            return List.copyOf(pendingListings);
        }
    }

    public List<ProfileAuctionCardUiModel> getSoldListings() {
        synchronized (soldListings) {
            return List.copyOf(soldListings);
        }
    }

    public List<ProfileAuctionCardUiModel> getUnsoldListings() {
        synchronized (unsoldListings) {
            return List.copyOf(unsoldListings);
        }
    }
}
