package com.auction.client.service;

import com.auction.core.utils.JsonMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Map;

/**
 * Đồng bộ đồng hồ Client với giờ chuẩn UTC+7 (Asia/Ho_Chi_Minh).
 *
 * <p>Gọi {@link #syncTime()} một lần khi khởi động ứng dụng. Sau đó thay thế mọi
 * {@code LocalDateTime.now()} liên quan đến đếm ngược đấu giá bằng {@link #getNow()} để đảm bảo
 * hiển thị luôn khớp với đồng hồ Server, dù máy người dùng bị sai giờ.
 */
public final class TimeSyncService {

    /** Độ lệch mili-giây giữa giờ chuẩn UTC+7 và giờ hệ thống Client. Mặc định 0 (không lệch). */
    private static volatile long timeOffsetMillis = 0;

    private static final String TIME_API_URL =
            "http://worldtimeapi.org/api/timezone/Asia/Ho_Chi_Minh";

    private TimeSyncService() {}

    /**
     * Khởi chạy một Virtual Thread bất đồng bộ để lấy giờ chuẩn và tính sai số. Timeout 3 giây.
     * Nếu thất bại, giữ nguyên offset = 0 (dùng giờ cục bộ như cũ).
     */
    public static void syncTime() {
        Thread.ofVirtual()
                .name("time-sync-worker")
                .start(
                        () -> {
                            try {
                                HttpClient httpClient =
                                        HttpClient.newBuilder()
                                                .connectTimeout(Duration.ofSeconds(3))
                                                .build();
                                HttpRequest request =
                                        HttpRequest.newBuilder()
                                                .uri(URI.create(TIME_API_URL))
                                                .timeout(Duration.ofSeconds(3))
                                                .GET()
                                                .build();

                                HttpResponse<String> response =
                                        httpClient.send(
                                                request, HttpResponse.BodyHandlers.ofString());

                                if (response.statusCode() == 200) {
                                    Map<?, ?> data =
                                            JsonMapper.fromJson(response.body(), Map.class);
                                    // worldtimeapi trả về chuỗi ISO-8601 có offset, ví dụ:
                                    // "2026-05-31T12:00:00.123456+07:00"
                                    String datetimeStr = String.valueOf(data.get("datetime"));
                                    ZonedDateTime realZonedTime = ZonedDateTime.parse(datetimeStr);
                                    LocalDateTime realUtc7Time = realZonedTime.toLocalDateTime();
                                    LocalDateTime clientSystemTime = LocalDateTime.now();

                                    timeOffsetMillis =
                                            Duration.between(clientSystemTime, realUtc7Time)
                                                    .toMillis();
                                    System.out.printf(
                                            "[TimeSyncService] Đồng bộ hoàn tất."
                                                    + " Độ lệch Client: %+d ms%n",
                                            timeOffsetMillis);
                                } else {
                                    System.err.printf(
                                            "[TimeSyncService] API trả về HTTP %d,"
                                                    + " giữ nguyên giờ cục bộ.%n",
                                            response.statusCode());
                                }
                            } catch (Exception e) {
                                System.err.printf(
                                        "[TimeSyncService] Đồng bộ thất bại (%s),"
                                                + " sử dụng giờ cục bộ mặc định.%n",
                                        e.getMessage());
                            }
                        });
    }

    /**
     * Trả về thời điểm hiện tại đã bù sai số so với giờ chuẩn UTC+7.
     * Dùng thay thế cho {@code LocalDateTime.now()} trong mọi logic đếm ngược đấu giá.
     */
    public static LocalDateTime getNow() {
        return LocalDateTime.now().plus(Duration.ofMillis(timeOffsetMillis));
    }
}
