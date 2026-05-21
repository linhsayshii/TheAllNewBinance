package com.auction.client.mock;

import com.auction.core.auction.Auction;
import com.auction.core.auction.Bid;
import com.auction.core.products.Item;
import com.auction.core.protocol.EventType;
import com.auction.core.users.User;
import com.auction.core.utils.JsonMapper;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Provides mock response JSON for each supported {@link EventType}.
 *
 * <p>Data is loaded once from {@code resources/mockdata/} JSON files and cached in memory. Mutable
 * state (bids placed at runtime) is kept in-memory only.
 *
 * <p>Response format mirrors the real server exactly:
 *
 * <pre>
 * { "type": "...", "correlationId": "...", "success": true/false, "data": ... }
 * </pre>
 */
public class MockDataProvider {

    // ------------------------------------------------------------------ //
    //  In-memory stores                                                    //
    // ------------------------------------------------------------------ //
    private final List<User> users;
    private final List<Auction> auctions; // mutable — bids update currentPrice
    private final List<Bid> bids; // mutable — place bid appends here
    private final List<Item> items;

    private final AtomicInteger nextBidId = new AtomicInteger(9000);

    // ------------------------------------------------------------------ //
    //  Boot                                                                //
    // ------------------------------------------------------------------ //

    public MockDataProvider() {
        users = loadList("/mockdata/users.json", User[].class);
        items = loadList("/mockdata/items.json", Item[].class);
        auctions = loadList("/mockdata/auctions.json", Auction[].class);
        bids = new ArrayList<>(loadList("/mockdata/bids.json", Bid[].class));

        System.out.println(
                "[MockMode] Loaded: "
                        + users.size()
                        + " users, "
                        + auctions.size()
                        + " auctions, "
                        + bids.size()
                        + " bids, "
                        + items.size()
                        + " items");
    }

    // ------------------------------------------------------------------ //
    //  Main dispatcher                                                     //
    // ------------------------------------------------------------------ //

    /**
     * Returns a JSON response string for the given event type and payload. Called by {@link
     * MockAuctionClient} after a short async delay.
     */
    public String buildResponse(EventType type, Object payload) {
        // Automatically update the status of all auctions in memory based on current time before
        // responding
        updateAuctionsStatus();

        try {
            return switch (type) {
                case LOGIN -> handleLogin(payload);
                case REGISTER -> handleRegister(payload);
                case LOGOUT -> successJson(null);
                case GET_PUBLIC_AUCTIONS -> handleGetPublicAuctions(payload);
                case GET_AUCTION_DETAILS -> handleGetAuctionDetails(payload);
                case GET_BIDS_BY_AUCTION_ID -> handleGetBidsByAuctionId(payload);
                case PLACE_BID -> handlePlaceBid(payload);
                case GET_BIDS_BY_BIDDER_ID -> handleGetBidsByBidderId(payload);
                case GET_AUCTIONS_BY_SELLER -> handleGetAuctionsBySeller(payload);
                case CREATE_AUCTION -> handleCreateAuction(payload);
                case GET_FEATURED_AUCTIONS -> handleGetFeaturedAuctions(payload);
                case PROMOTE_AUCTION -> handlePromoteAuction(payload);
                case GET_ALL_AUCTIONS_ADMIN -> handleGetAllAuctionsAdmin(payload);
                case GET_ALL_USERS_ADMIN -> handleGetAllUsersAdmin(payload);
                case GET_UPLOAD_SIGNATURE -> handleGetUploadSignature(payload);
                default -> errorJson("Event type not supported in mock mode: " + type);
            };
        } catch (Exception e) {
            System.err.println(
                    "[MockDataProvider] Error building response for "
                            + type
                            + ": "
                            + e.getMessage());
            return errorJson("Internal mock error: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ //
    //  Handlers                                                            //
    // ------------------------------------------------------------------ //

    private String handleLogin(Object payload) {
        Map<?, ?> map = toMap(payload);
        if (map == null) return errorJson("Invalid login payload");

        String username = str(map.get("username"));
        String password = str(map.get("password"));

        User found =
                users.stream()
                        .filter(u -> username != null && username.equals(u.getUsername()))
                        .findFirst()
                        .orElse(null);

        if (found == null) {
            return errorJson("Người dùng không tồn tại.");
        }
        // Plain-text password comparison (mock only — never do this in production)
        if (!password.equals(found.getPassword())) {
            return errorJson("Sai mật khẩu.");
        }
        return successJson(found);
    }

    private String handleRegister(Object payload) {
        Map<?, ?> map = toMap(payload);
        if (map == null) return errorJson("Invalid register payload");

        String username = str(map.get("username"));
        boolean taken =
                users.stream().anyMatch(u -> username != null && username.equals(u.getUsername()));
        if (taken) {
            return errorJson("Tên đăng nhập đã tồn tại.");
        }

        int newId =
                users.stream().mapToInt(u -> u.getId() != null ? u.getId() : 0).max().orElse(0) + 1;
        User newUser =
                new User(
                        newId,
                        username,
                        str(map.get("password")),
                        str(map.get("fullName")),
                        str(map.get("email")),
                        0.0,
                        User.Role.STANDARD,
                        true);
        users.add(newUser);
        return successJson(newUser);
    }

    private String handleGetPublicAuctions(Object payload) {
        Map<?, ?> map = toMap(payload);
        String statusFilter = map != null ? str(map.get("status")) : "ACTIVE";
        if (statusFilter == null) statusFilter = "ACTIVE";
        final String filter = statusFilter.toUpperCase();

        List<Map<String, Object>> cards =
                auctions.stream()
                        .filter(a -> a.getStatus() != null && a.getStatus().name().equals(filter))
                        .map(
                                a -> {
                                    Item item = findItem(a.getItemId());
                                    User seller =
                                            findUser(a.getItemId()); // We use sellerId from item
                                    if (item != null) {
                                        seller = findUser(item.getSellerId());
                                    }
                                    return buildPublicAuctionDto(a, item, seller);
                                })
                        .collect(Collectors.toList());

        return successJson(cards);
    }

    private String handleGetAuctionDetails(Object payload) {
        Integer auctionId = extractId(payload, "auctionId");
        if (auctionId == null) return errorJson("Missing auctionId");

        Auction auction = findAuction(auctionId);
        if (auction == null) return errorJson("Auction not found: " + auctionId);

        Item item = null;
        if (auction.getItemId() != null) {
            item = findItem(auction.getItemId());
        }

        User seller = null;
        if (item != null && item.getSellerId() != null) {
            seller = findUser(item.getSellerId());
        }

        com.auction.core.dto.auction.AuctionDetailsDto dto =
                new com.auction.core.dto.auction.AuctionDetailsDto(auction, item, seller);
        return successJson(dto);
    }

    private String handleGetBidsByAuctionId(Object payload) {
        Map<?, ?> map = toMap(payload);
        Integer auctionId = map != null ? toInt(map.get("auctionId")) : null;
        if (auctionId == null) return errorJson("Missing auctionId");

        List<Bid> result =
                bids.stream()
                        .filter(b -> auctionId.equals(b.getAuctionId()))
                        .collect(Collectors.toList());
        return successJson(result);
    }

    private String handlePlaceBid(Object payload) {
        Map<?, ?> map = toMap(payload);
        if (map == null) return errorJson("Invalid bid payload");

        Integer auctionId = toInt(map.get("auctionId"));
        Integer bidderId = toInt(map.get("bidderId"));
        Double amount = toDouble(map.get("amount"));

        if (auctionId == null || bidderId == null || amount == null) {
            return errorJson("Missing required bid fields");
        }

        Auction auction = findAuction(auctionId);
        if (auction == null) return errorJson("Auction not found");
        if (auction.getStatus() != Auction.Status.ACTIVE) {
            return errorJson("Auction is not active");
        }
        if (amount <= auction.getCurrentPrice()) {
            return errorJson(
                    "Bid must be higher than current price of " + auction.getCurrentPrice());
        }

        // Update auction price and winner ID
        auction.setCurrentPrice(amount);
        auction.setWinnerId(bidderId);

        // Create and store bid
        Bid newBid = new Bid(nextBidId.getAndIncrement(), auctionId, bidderId, amount);
        bids.add(newBid);

        return successJson(newBid);
    }

    private String handleGetBidsByBidderId(Object payload) {
        Map<?, ?> map = toMap(payload);
        Integer bidderId = map != null ? toInt(map.get("bidderId")) : null;
        if (bidderId == null) return errorJson("Missing bidderId");

        List<Bid> result =
                bids.stream()
                        .filter(b -> bidderId.equals(b.getBidderId()))
                        .collect(Collectors.toList());
        return successJson(result);
    }

    private String handleGetAuctionsBySeller(Object payload) {
        Map<?, ?> map = toMap(payload);
        Integer sellerId = map != null ? toInt(map.get("sellerId")) : null;
        if (sellerId == null) return errorJson("Missing sellerId");

        // Match auctions whose item's sellerId matches
        final Integer sid = sellerId;
        List<Auction> result =
                auctions.stream()
                        .filter(
                                a -> {
                                    Item item = findItem(a.getItemId());
                                    return item != null && sid.equals(item.getSellerId());
                                })
                        .collect(Collectors.toList());
        return successJson(result);
    }

    private String handleCreateAuction(Object payload) {
        try {
            String json = JsonMapper.toJson(payload);
            com.auction.core.dto.auction.CreateAuctionRequest req =
                    JsonMapper.fromJson(
                            json, com.auction.core.dto.auction.CreateAuctionRequest.class);

            if (req.getSellerId() == null) return errorJson("Missing sellerId");

            int newItemId = items.stream().mapToInt(Item::getId).max().orElse(0) + 1;
            Item newItem =
                    new Item(
                            newItemId,
                            req.getSellerId(),
                            req.getItemTitle(),
                            req.getItemDescription(),
                            req.getItemCategory(),
                            req.getItemImageUrl(),
                            false);
            items.add(newItem);

            int newAuctionId = auctions.stream().mapToInt(Auction::getId).max().orElse(0) + 1;
            Auction newAuction =
                    new Auction(
                            newAuctionId,
                            newItemId,
                            req.getStartingPrice(),
                            req.getBidIncrement(),
                            req.getStartTime(),
                            req.getEndTime());
            auctions.add(newAuction);

            return successJson(newAuction);
        } catch (Exception e) {
            e.printStackTrace();
            return errorJson("Invalid payload for CREATE_AUCTION");
        }
    }

    private String handleGetFeaturedAuctions(Object payload) {
        List<Map<String, Object>> featured =
                auctions.stream()
                        .filter(
                                a ->
                                        a.getIsFeatured()
                                                && (a.getStatus() == Auction.Status.ACTIVE
                                                        || a.getStatus() == Auction.Status.PENDING))
                        .limit(5)
                        .map(
                                a -> {
                                    Item item = findItem(a.getItemId());
                                    User seller =
                                            item != null ? findUser(item.getSellerId()) : null;
                                    return buildPublicAuctionDto(a, item, seller);
                                })
                        .collect(Collectors.toList());
        return successJson(featured);
    }

    private String handlePromoteAuction(Object payload) {
        try {
            String json = JsonMapper.toJson(payload);
            com.auction.core.dto.auction.PromoteAuctionRequest req =
                    JsonMapper.fromJson(
                            json, com.auction.core.dto.auction.PromoteAuctionRequest.class);

            Auction auction = findAuction(req.getAuctionId());
            if (auction == null) return errorJson("Không tìm thấy phiên đấu giá.");

            // Mock logic: set fixed 00:00 end time
            int days = req.getPackageDays() != null ? req.getPackageDays() : 1;
            java.time.LocalDateTime expiry =
                    java.time.LocalDate.now()
                            .plusDays(days + 1)
                            .atTime(java.time.LocalTime.MIDNIGHT);

            auction.setIsFeatured(true);
            auction.setFeaturedUntil(expiry);

            String promoted = req.getShortDescription();
            if (promoted == null || promoted.isBlank()) {
                Item item = findItem(auction.getItemId());
                if (item != null && item.getDescription() != null) {
                    promoted =
                            item.getDescription().length() > 100
                                    ? item.getDescription().substring(0, 100) + "..."
                                    : item.getDescription();
                }
            }
            auction.setPromotedDescription(promoted);

            return successJson(null, "Promote thành công!");
        } catch (Exception e) {
            return errorJson("Lỗi xử lý promote: " + e.getMessage());
        }
    }

    private String handleGetAllUsersAdmin(Object payload) {
        // Return all users (sensitive in production; mocked here for dev)
        return successJson(users);
    }

    private String handleGetAllAuctionsAdmin(Object payload) {
        // Admin view: all auctions
        List<Map<String, Object>> all =
                auctions.stream()
                        .map(
                                a -> {
                                    Item item = findItem(a.getItemId());
                                    User seller =
                                            item != null ? findUser(item.getSellerId()) : null;
                                    return buildPublicAuctionDto(a, item, seller);
                                })
                        .collect(Collectors.toList());
        return successJson(all);
    }

    // ------------------------------------------------------------------ //
    //  Public accessor                                                     //
    // ------------------------------------------------------------------ //

    /** Returns user id=1 (demo_user) for auto-login on mock startup. */
    public User getDefaultUser() {
        return users.stream()
                .filter(u -> u.getId() != null && u.getId() == 1)
                .findFirst()
                .orElse(null);
    }

    // ------------------------------------------------------------------ //
    //  JSON builder helpers                                                //
    // ------------------------------------------------------------------ //

    private Map<String, Object> buildPublicAuctionDto(Auction a, Item item, User seller) {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("auctionId", a.getId() != null ? a.getId() : 0);
        map.put("itemId", a.getItemId() != null ? a.getItemId() : 0);
        map.put(
                "itemName",
                item != null && item.getName() != null ? item.getName() : "Auction #" + a.getId());
        map.put(
                "thumbnailUrl",
                item != null && item.getImageUrl() != null ? item.getImageUrl() : "");
        map.put("currentPrice", a.getCurrentPrice() != null ? a.getCurrentPrice() : 0.0);
        map.put("startTime", a.getStartTime());
        map.put("endTime", a.getEndTime());
        map.put("status", a.getStatus() != null ? a.getStatus().name() : "ACTIVE");
        map.put(
                "sellerDisplayName",
                seller != null && seller.getFullName() != null
                        ? seller.getFullName()
                        : (seller != null ? seller.getUsername() : "Seller"));
        map.put("isFeatured", a.getIsFeatured() != null ? a.getIsFeatured() : false);
        map.put("featuredUntil", a.getFeaturedUntil());
        map.put(
                "promotedDescription",
                a.getPromotedDescription() != null ? a.getPromotedDescription() : "");
        return map;
    }

    private String successJson(Object data) {
        return successJson(data, "OK");
    }

    private String successJson(Object data, String message) {
        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);
        return JsonMapper.toJson(response);
    }

    private String errorJson(String message) {
        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("data", null);
        return JsonMapper.toJson(response);
    }

    // ------------------------------------------------------------------ //
    //  Lookup helpers                                                      //
    // ------------------------------------------------------------------ //

    private Auction findAuction(Integer id) {
        if (id == null) return null;
        return auctions.stream().filter(a -> id.equals(a.getId())).findFirst().orElse(null);
    }

    private Item findItem(Integer itemId) {
        if (itemId == null) return null;
        return items.stream().filter(i -> itemId.equals(i.getId())).findFirst().orElse(null);
    }

    private User findUser(Integer userId) {
        if (userId == null) return null;
        return users.stream().filter(u -> userId.equals(u.getId())).findFirst().orElse(null);
    }

    // ------------------------------------------------------------------ //
    //  Type coercion                                                       //
    // ------------------------------------------------------------------ //

    @SuppressWarnings("unchecked")
    private Map<?, ?> toMap(Object payload) {
        if (payload instanceof Map) return (Map<?, ?>) payload;
        if (payload == null) return null;
        try {
            return JsonMapper.fromJson(JsonMapper.toJson(payload), Map.class);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer extractId(Object payload, String key) {
        Map<?, ?> map = toMap(payload);
        if (map != null) return toInt(map.get(key));
        return null;
    }

    private Integer toInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double toDouble(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private String handleGetUploadSignature(Object payload) {
        try {
            Map<?, ?> map = toMap(payload);
            String folder =
                    map != null && map.get("folder") != null
                            ? str(map.get("folder"))
                            : "auction_items";
            long timestamp = System.currentTimeMillis() / 1000L;

            // Cloudinary credentials (matching ItemController.java)
            String apiKey = "453152866822858";
            String apiSecret = "6u0v6ubiLwNtEHIkACjr55HW5a4";

            // Generate SHA-1 signature
            String toSign = "folder=" + folder + "&timestamp=" + timestamp + apiSecret;
            String signature = sha1(toSign);

            Map<String, Object> data = new java.util.HashMap<>();
            data.put("signature", signature);
            data.put("timestamp", timestamp);
            data.put("apiKey", apiKey);

            Map<String, Object> response = new java.util.HashMap<>();
            response.put("status", "success");
            response.put("data", data);

            return JsonMapper.toJson(response);
        } catch (Exception e) {
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("status", "error");
            response.put("message", e.getMessage());
            return JsonMapper.toJson(response);
        }
    }

    private String sha1(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
            byte[] messageDigest =
                    md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ------------------------------------------------------------------ //
    //  JSON file loader                                                    //
    // ------------------------------------------------------------------ //

    private <T> List<T> loadList(String resourcePath, Class<T[]> arrayClass) {
        try (InputStream is = MockDataProvider.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("[MockDataProvider] Resource not found: " + resourcePath);
                return new ArrayList<>();
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            T[] arr = JsonMapper.fromJson(json, arrayClass);
            return arr != null ? new ArrayList<>(Arrays.asList(arr)) : new ArrayList<>();
        } catch (Exception e) {
            System.err.println(
                    "[MockDataProvider] Failed to load " + resourcePath + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public synchronized Bid generateSimulatedBidFromOtherUser() {
        // Tìm một phiên đấu giá đang ACTIVE và chưa kết thúc thời gian
        Auction activeAuction =
                auctions.stream()
                        .filter(a -> a.getStatus() == Auction.Status.ACTIVE)
                        .filter(
                                a ->
                                        a.getEndTime() == null
                                                || a.getEndTime()
                                                        .isAfter(java.time.LocalDateTime.now()))
                        .findAny()
                        .orElse(null);

        if (activeAuction == null) {
            return null;
        }

        // Chọn ngẫu nhiên một bidder ID từ 2 đến 5
        int bidderId = java.util.concurrent.ThreadLocalRandom.current().nextInt(2, 6);

        // Tăng giá hiện tại lên một khoảng ngẫu nhiên bằng bidIncrement * (1 đến 2)
        double currentPrice =
                activeAuction.getCurrentPrice() != null
                        ? activeAuction.getCurrentPrice()
                        : activeAuction.getStartingPrice();
        double increment =
                activeAuction.getBidIncrement() != null ? activeAuction.getBidIncrement() : 1.0;
        double amount =
                currentPrice
                        + increment
                                * java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 3);

        // Cập nhật giá phiên đấu giá và winnerId
        activeAuction.setCurrentPrice(amount);
        activeAuction.setWinnerId(bidderId);

        // Tạo bid mới
        Bid newBid = new Bid(nextBidId.getAndIncrement(), activeAuction.getId(), bidderId, amount);
        newBid.setCreatedAt(java.time.LocalDateTime.now());
        bids.add(newBid);

        System.out.println(
                "[MockMode] Simulated Bidder #"
                        + bidderId
                        + " placed bid of $"
                        + amount
                        + " on Auction #"
                        + activeAuction.getId());
        return newBid;
    }

    private synchronized void updateAuctionsStatus() {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        for (Auction a : auctions) {
            if (a.getStatus() == Auction.Status.PENDING) {
                if (a.getStartTime() != null && !now.isBefore(a.getStartTime())) {
                    a.setStatus(Auction.Status.ACTIVE);
                    System.out.println(
                            "[MockMode] Auction #"
                                    + a.getId()
                                    + " status transitioned: PENDING -> ACTIVE");
                }
            }
            if (a.getStatus() == Auction.Status.ACTIVE) {
                if (a.getEndTime() != null && !now.isBefore(a.getEndTime())) {
                    a.setStatus(Auction.Status.ENDED);

                    // Assign winner automatically based on highest bid in history if not already
                    // assigned
                    if (a.getWinnerId() == null || a.getWinnerId() == 0) {
                        Bid highestBid =
                                bids.stream()
                                        .filter(b -> b.getAuctionId().equals(a.getId()))
                                        .max(java.util.Comparator.comparingDouble(Bid::getAmount))
                                        .orElse(null);
                        if (highestBid != null) {
                            a.setWinnerId(highestBid.getBidderId());
                            a.setFinalPrice(highestBid.getAmount());
                        }
                    }
                    System.out.println(
                            "[MockMode] Auction #"
                                    + a.getId()
                                    + " status transitioned: ACTIVE -> ENDED, winner bidder: #"
                                    + a.getWinnerId());
                }
            }
        }
    }
}
