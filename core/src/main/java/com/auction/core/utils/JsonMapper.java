package com.auction.core.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class JsonMapper {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Khởi tạo Gson với format ngày giờ chuẩn để đồng bộ toàn hệ thống
    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .registerTypeAdapter(LocalDateTime.class, new TypeAdapter<LocalDateTime>() {
                @Override
                public void write(JsonWriter out, LocalDateTime value) throws IOException {
                    if (value == null) {
                        out.nullValue();
                    } else {
                        out.value(FORMATTER.format(value));
                    }
                }

                @Override
                public LocalDateTime read(JsonReader in) throws IOException {
                    if (in.peek() == com.google.gson.stream.JsonToken.NULL) {
                        in.nextNull();
                        return null;
                    }
                    return LocalDateTime.parse(in.nextString(), FORMATTER);
                }
            })
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
