package com.auction.client.service;

import com.auction.client.dto.ProductCardUiModel;
import com.auction.core.dto.auction.GetPublicAuctionsRequest;
import com.auction.core.dto.auction.PublicAuctionDto;
import com.auction.core.protocol.EventType;
import com.auction.core.utils.JsonMapper;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class AuctionQueryService {

    private static final DecimalFormat PRICE_FORMAT = new DecimalFormat("#,##0.00");
    private static final int MAX_FEATURED_AUCTIONS = 12;
    private static final long RESPONSE_TIMEOUT_MS = 5000;
    private static final long OPEN_WAIT_TIMEOUT_MS = 5000;
    private static final long OPEN_WAIT_STEP_MS = 100;
    private static final long CLIENT_CACHE_TTL_MS = 0;
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PENDING = "PENDING";
    private static final DateTimeFormatter UPCOMING_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private static volatile AuctionFeed cachedFeed = new AuctionFeed(List.of(), List.of());
    private static volatile long cacheExpiresAtMillis = 0L;

    public static void clearCache() {
        cacheExpiresAtMillis = 0L;
        cachedFeed = new AuctionFeed(List.of(), List.of());
    }

    public record AuctionFeed(
            List<ProductCardUiModel> liveAuctions, List<ProductCardUiModel> upcomingAuctions) {}

    public AuctionFeed getFeaturedAuctionFeed() {
        return fetchPublicAuctions();
    }

    public List<ProductCardUiModel> getFeaturedAuctions() {
        return getFeaturedAuctionFeed().liveAuctions();
    }

    private AuctionFeed fetchPublicAuctions() {
        try {
            NetworkService networkService = NetworkService.getInstance();
            if (!waitUntilSocketOpen(networkService, OPEN_WAIT_TIMEOUT_MS)) {
                return new AuctionFeed(List.of(), List.of());
            }

            CountDownLatch liveLatch = new CountDownLatch(1);
            AtomicReference<List<ProductCardUiModel>> liveRef = new AtomicReference<>(List.of());
            String liveCorr = java.util.UUID.randomUUID().toString();

            networkService.addCorrelationHandler(
                    liveCorr,
                    raw -> {
                        liveRef.set(parsePublicAuctionCardList(raw).liveAuctions());
                        liveLatch.countDown();
                    });

            CountDownLatch pendingLatch = new CountDownLatch(1);
            AtomicReference<List<ProductCardUiModel>> pendingRef = new AtomicReference<>(List.of());
            String pendingCorr = java.util.UUID.randomUUID().toString();

            networkService.addCorrelationHandler(
                    pendingCorr,
                    raw -> {
                        pendingRef.set(parsePublicAuctionCardList(raw).upcomingAuctions());
                        pendingLatch.countDown();
                    });

            networkService
                    .getClient()
                    .sendRequest(
                            EventType.GET_PUBLIC_AUCTIONS,
                            liveCorr,
                            new GetPublicAuctionsRequest(
                                    1, MAX_FEATURED_AUCTIONS, "ACTIVE", true, false));

            networkService
                    .getClient()
                    .sendRequest(
                            EventType.GET_PUBLIC_AUCTIONS,
                            pendingCorr,
                            new GetPublicAuctionsRequest(
                                    1, MAX_FEATURED_AUCTIONS, "PENDING", true, false));

            liveLatch.await(RESPONSE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            pendingLatch.await(RESPONSE_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            return new AuctionFeed(liveRef.get(), pendingRef.get());
        } catch (Exception ex) {
            return new AuctionFeed(List.of(), List.of());
        }
    }

    private boolean waitUntilSocketOpen(NetworkService networkService, long timeoutMs) {
        long waited = 0;
        while (!networkService.getClient().isOpen() && waited < timeoutMs) {
            try {
                Thread.sleep(OPEN_WAIT_STEP_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            waited += OPEN_WAIT_STEP_MS;
        }
        return networkService.getClient().isOpen();
    }

    private AuctionFeed parsePublicAuctionCardList(String rawJson) {
        try {
            Map<?, ?> response = JsonMapper.fromJson(rawJson, Map.class);
            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                return new AuctionFeed(List.of(), List.of());
            }

            Object data = response.get("data");
            if (!(data instanceof List)) {
                return new AuctionFeed(List.of(), List.of());
            }

            List<ProductCardUiModel> cards = parseFromRawRows((List<?>) data);
            if (!cards.isEmpty()) {
                return splitAndSort(cards);
            }

            String dataJson = JsonMapper.toJson(data);
            PublicAuctionDto[] auctions = JsonMapper.fromJson(dataJson, PublicAuctionDto[].class);
            if (auctions == null || auctions.length == 0) {
                return new AuctionFeed(List.of(), List.of());
            }

            List<ProductCardUiModel> mapped =
                    java.util.Arrays.stream(auctions)
                            .filter(java.util.Objects::nonNull)
                            .map(this::toCard)
                            .toList();
            return splitAndSort(mapped);
        } catch (Exception ex) {
            return new AuctionFeed(List.of(), List.of());
        }
    }

    private List<ProductCardUiModel> parseFromRawRows(List<?> rows) {
        List<ProductCardUiModel> cards = new ArrayList<>();
        for (Object row : rows) {
            if (!(row instanceof Map<?, ?> item)) {
                continue;
            }

            Integer itemId = toInteger(item.get("itemId"));
            String title = toSafeString(item.get("itemName"));
            if (title == null || title.isBlank()) {
                title = itemId == null ? "Item" : "Item #" + itemId;
            }

            String seller = toSafeString(item.get("sellerDisplayName"));
            if (seller == null || seller.isBlank()) {
                seller = "Seller";
            }

            double displayPrice = toDouble(item.get("currentPrice"));
            String currentBid = "$" + PRICE_FORMAT.format(displayPrice);
            Integer auctionId = toInteger(item.get("auctionId"));
            String status = normalizeStatus(toSafeString(item.get("status")));
            LocalDateTime startTime = toLocalDateTime(item.get("startTime"));
            LocalDateTime endTime = toLocalDateTime(item.get("endTime"));
            String timeLeft =
                    STATUS_PENDING.equals(status)
                            ? formatUpcomingStart(startTime)
                            : formatTimeLeft(endTime);
            String imageUrl = toSafeString(item.get("thumbnailUrl"));

            LocalDateTime sortTime = STATUS_PENDING.equals(status) ? startTime : endTime;
            cards.add(
                    new ProductCardUiModel(
                            auctionId,
                            status,
                            title,
                            seller,
                            currentBid,
                            timeLeft,
                            sortTime,
                            imageUrl));
        }
        return cards;
    }

    private String toSafeString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private double toDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }

        String raw = String.valueOf(value).trim();
        if (raw.isEmpty()) {
            return null;
        }

        String normalized = raw.replace(' ', 'T');

        try {
            return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
        }

        try {
            return OffsetDateTime.parse(normalized).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDateTime.parse(
                    normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        } catch (DateTimeParseException ignored) {
        }

        try {
            return LocalDateTime.parse(
                    normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
        } catch (DateTimeParseException ignored) {
        }

        return null;
    }

    private ProductCardUiModel toCard(PublicAuctionDto auction) {
        String title =
                (auction.getItemName() == null || auction.getItemName().isBlank())
                        ? "Item #" + auction.getItemId()
                        : auction.getItemName();
        String seller =
                (auction.getSellerDisplayName() == null || auction.getSellerDisplayName().isBlank())
                        ? "Seller"
                        : auction.getSellerDisplayName();

        double displayPrice =
                auction.getCurrentPrice() != null && auction.getCurrentPrice() > 0
                        ? auction.getCurrentPrice()
                        : 0.0;

        String status = normalizeStatus(auction.getStatus());
        String currentBid = "$" + PRICE_FORMAT.format(displayPrice);
        String timeLeft =
                STATUS_PENDING.equals(status)
                        ? formatUpcomingStart(auction.getStartTime())
                        : formatTimeLeft(auction.getEndTime());
        LocalDateTime sortTime =
                STATUS_PENDING.equals(status) ? auction.getStartTime() : auction.getEndTime();
        return new ProductCardUiModel(
                auction.getAuctionId(),
                status,
                title,
                seller,
                currentBid,
                timeLeft,
                sortTime,
                auction.getThumbnailUrl());
    }

    private AuctionFeed splitAndSort(List<ProductCardUiModel> cards) {
        java.util.Comparator<ProductCardUiModel> byTime =
                java.util.Comparator.comparing(
                        ProductCardUiModel::sortTime,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder()));

        List<ProductCardUiModel> live =
                cards.stream().filter(ProductCardUiModel::isLive).sorted(byTime).toList();
        List<ProductCardUiModel> upcoming =
                cards.stream().filter(ProductCardUiModel::isUpcoming).sorted(byTime).toList();

        return new AuctionFeed(live, upcoming);
    }

    private String normalizeStatus(String status) {
        if (status == null) {
            return STATUS_ACTIVE;
        }
        String normalized = status.trim().toUpperCase();
        if (STATUS_PENDING.equals(normalized)) {
            return STATUS_PENDING;
        }
        return STATUS_ACTIVE;
    }

    private String formatUpcomingStart(LocalDateTime startTime) {
        if (startTime == null) {
            return "N/A";
        }
        if (startTime.isBefore(LocalDateTime.now())) {
            return "Soon";
        }
        return startTime.format(UPCOMING_TIME_FORMAT);
    }

    private String formatTimeLeft(LocalDateTime endTime) {
        if (endTime == null) {
            return "N/A";
        }

        Duration remaining = Duration.between(LocalDateTime.now(), endTime);
        if (remaining.isNegative() || remaining.isZero()) {
            return "Ended";
        }

        long totalMinutes = remaining.toMinutes();
        long days = totalMinutes / (24 * 60);
        long hours = (totalMinutes % (24 * 60)) / 60;
        long minutes = totalMinutes % 60;

        if (days > 0) {
            return days + "d " + hours + "h";
        }
        return String.format("%02dh %02dm", hours, minutes);
    }
}
