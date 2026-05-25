package com.auction.core.utils;

import com.auction.core.products.Item;
import com.auction.core.products.serialization.ItemJsonDeserializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class JsonMapper {
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Initialize Gson with unified datetime format for full system synchronization
    private static final Gson GSON =
            new GsonBuilder()
                    .setDateFormat("yyyy-MM-dd HH:mm:ss")
                    .registerTypeAdapter(Item.class, new ItemJsonDeserializer())
                    .registerTypeAdapter(com.auction.core.users.User.class, new com.auction.core.users.serialization.UserJsonDeserializer())
                    .registerTypeAdapter(
                            LocalDateTime.class,
                            new TypeAdapter<LocalDateTime>() {
                                @Override
                                public void write(JsonWriter out, LocalDateTime value)
                                        throws IOException {
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
     * Convert Java Object to JSON string.
     *
     * @param object The DTO/Entity object to convert
     * @return Corresponding JSON string
     */
    public static String toJson(Object object) {
        if (object == null) {
            return null;
        }
        return GSON.toJson(object);
    }

    /**
     * Parse JSON string to a specific Java object.
     *
     * @param json Input JSON string from Client/Server
     * @param clazz Target object class (e.g. UserDTO.class)
     * @return Mapped object instance
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        return GSON.fromJson(json, clazz);
    }
}
