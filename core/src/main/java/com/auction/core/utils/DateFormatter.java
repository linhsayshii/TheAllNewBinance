package com.auction.core.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateFormatter {
    public static final String PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(PATTERN);

    // Chuyển LocalDateTime thành String (để gửi qua UI hoặc JSON)
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.format(formatter);
    }

    // Parse String thành LocalDateTime (khi nhận String từ mạng)
    public static LocalDateTime parse(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) return null;
        try {
            return LocalDateTime.parse(dateTimeStr.trim(), formatter);
        } catch (DateTimeParseException e) {
            System.err.println("Lỗi parse ngày tháng: " + dateTimeStr);
            return null;
        }
    }
}