package com.auction.core.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JsonMapper {
    // Khởi tạo Gson với format ngày giờ chuẩn để đồng bộ toàn hệ thống
    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .create();

    /**
     * Chuyển Java Object thành chuỗi JSON
     * @param object Đối tượng DTO/Entity cần chuyển
     * @return Chuỗi JSON tương ứng
     */
    public static String toJson(Object object) {
        if (object == null) return null;
        return GSON.toJson(object);
    }

    /**
     * Parse chuỗi JSON thành đối tượng Java cụ thể
     * @param json Chuỗi JSON đầu vào từ Client/Server
     * @param clazz Class của đối tượng mong muốn (VD: UserDTO.class)
     * @return Đối tượng đã được map dữ liệu
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) return null;
        return GSON.fromJson(json, clazz);
    }
}
