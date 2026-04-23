package com.auction.core.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class JsonMapper {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Gson GSON = new GsonBuilder()
            .setDateFormat("yyyy-MM-dd HH:mm:ss")
            .registerTypeAdapter(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
                @Override
                public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
                    return new JsonPrimitive(src.format(FORMATTER));
                }
            })
            .registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
                @Override
                public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                        throws JsonParseException {
                    return LocalDateTime.parse(json.getAsString(), FORMATTER);
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
