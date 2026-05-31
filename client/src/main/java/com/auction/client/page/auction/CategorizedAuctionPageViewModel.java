package com.auction.client.page.auction;

import com.auction.client.dto.ProductCardUiModel;
import com.auction.client.service.NetworkService;
import com.auction.client.service.TimeSyncService;
import com.auction.core.dto.auction.GetPublicAuctionsRequest;
import com.auction.core.dto.auction.PublicAuctionDto;
import com.auction.core.protocol.EventType;
import com.auction.core.utils.JsonMapper;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class CategorizedAuctionPageViewModel {

    private static final long RESPONSE_TIMEOUT_MS = 5000;
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final DateTimeFormatter UPCOMING_FMT =
            DateTimeFormatter.ofPattern("dd/MM HH:mm");

    /** Loads all auctions (ACTIVE, PENDING, ENDED) filtered by item type, sorted by priority. */
    public List<ProductCardUiModel> loadAuctionsByItemType(String itemType) {
        List<ProductCardUiModel> result = new ArrayList<>();
        try {
            NetworkService networkService = NetworkService.getInstance();
            if (!networkService.getClient().isOpen()) {
                return result;
            }

            CountDownLatch latch = new CountDownLatch(1);
            AtomicReference<String> responseRef = new AtomicReference<>();
            String correlationId = java.util.UUID.randomUUID().toString();

            networkService.addCorrelationHandler(
                    correlationId,
                    raw -> {
                        responseRef.set(raw);
                        latch.countDown();
                    });

            GetPublicAuctionsRequest req = new GetPublicAuctionsRequest();
            req.setPage(1);
            req.setSize(100);
            req.setStatus("ACTIVE,PENDING,ENDED");
            req.setIncludeEndingSoon(false);
            req.setIncludeTrending(false);
            req.setItemType(itemType);
            req.setOrderByPrioritizedStatus(true);

            networkService.getClient().sendRequest(EventType.GET_PUBLIC_AUCTIONS, correlationId, req);

            boolean success = latch.await(RESPONSE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (success && responseRef.get() != null) {
                result = parseResponse(responseRef.get());
            }
        } catch (Exception e) {
            System.err.println("[CategorizedAuctionPageViewModel] Error: " + e.getMessage());
        }
        return result;
    }

    private List<ProductCardUiModel> parseResponse(String rawJson) {
        List<ProductCardUiModel> cards = new ArrayList<>();
        try {
            Map<?, ?> response = JsonMapper.fromJson(rawJson, Map.class);
            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                return cards;
            }

            Object data = response.get("data");
            if (!(data instanceof List)) {
                return cards;
            }

            String dataJson = JsonMapper.toJson(data);
            PublicAuctionDto[] auctions = JsonMapper.fromJson(dataJson, PublicAuctionDto[].class);
            if (auctions == null) {
                return cards;
            }

            DecimalFormat priceFormat = new DecimalFormat("#,##0.00");
            for (PublicAuctionDto dto : auctions) {
                if (dto == null) {
                    continue;
                }

                String status = dto.getStatus() != null ? dto.getStatus().toUpperCase() : STATUS_ACTIVE;
                String title = (dto.getItemName() == null || dto.getItemName().isBlank())
                        ? "Item #" + dto.getItemId() : dto.getItemName();
                String seller = (dto.getSellerDisplayName() == null
                        || dto.getSellerDisplayName().isBlank())
                        ? "Seller" : dto.getSellerDisplayName();
                double price = dto.getCurrentPrice() != null ? dto.getCurrentPrice() : 0.0;
                String currentBid = "$" + priceFormat.format(price);

                String timeLeft;
                if (STATUS_PENDING.equals(status)) {
                    timeLeft = formatUpcomingStart(dto.getStartTime());
                } else if (STATUS_ACTIVE.equals(status)) {
                    timeLeft = formatTimeLeft(dto.getEndTime());
                } else {
                    timeLeft = "Ended";
                }

                LocalDateTime sortTime = STATUS_PENDING.equals(status)
                        ? dto.getStartTime() : dto.getEndTime();

                cards.add(new ProductCardUiModel(
                        dto.getAuctionId(),
                        status,
                        title,
                        seller,
                        currentBid,
                        timeLeft,
                        sortTime,
                        dto.getThumbnailUrl(),
                        dto.getItemCategory()));
            }
        } catch (Exception e) {
            System.err.println("[CategorizedAuctionPageViewModel] Parse error: " + e.getMessage());
        }
        return cards;
    }

    private String formatUpcomingStart(LocalDateTime startTime) {
        if (startTime == null) {
            return "N/A";
        }
        if (startTime.isBefore(TimeSyncService.getNow())) {
            return "Soon";
        }
        return startTime.format(UPCOMING_FMT);
    }

    private String formatTimeLeft(LocalDateTime endTime) {
        if (endTime == null) {
            return "N/A";
        }
        Duration remaining = Duration.between(TimeSyncService.getNow(), endTime);
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
