package com.auction.core.users.serialization;

import com.auction.core.users.User;
import com.auction.core.users.UserFactory;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Polymorphic Gson deserializer for {@link User} and its sealed subclasses.
 */
public class UserJsonDeserializer implements JsonDeserializer<User> {

    @Override
    public User deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();

        Integer id = obj.has("id") && !obj.get("id").isJsonNull()
                ? obj.get("id").getAsInt() : null;
        String username = obj.has("username") && !obj.get("username").isJsonNull()
                ? obj.get("username").getAsString() : "";
        String roleStr = obj.has("role") && !obj.get("role").isJsonNull()
                ? obj.get("role").getAsString() : "STANDARD";
        String password = obj.has("password") && !obj.get("password").isJsonNull()
                ? obj.get("password").getAsString() : "";
        String fullName = obj.has("fullName") && !obj.get("fullName").isJsonNull()
                ? obj.get("fullName").getAsString() : "";
        String email = obj.has("email") && !obj.get("email").isJsonNull()
                ? obj.get("email").getAsString() : "";
        Boolean isActive = obj.has("isActive") && !obj.get("isActive").isJsonNull()
                ? obj.get("isActive").getAsBoolean() : true;

        BigDecimal balance = obj.has("balance") && !obj.get("balance").isJsonNull()
                ? obj.get("balance").getAsBigDecimal() : BigDecimal.ZERO;
        BigDecimal lockedBalance = obj.has("lockedBalance") && !obj.get("lockedBalance").isJsonNull()
                ? obj.get("lockedBalance").getAsBigDecimal() : BigDecimal.ZERO;

        User user = UserFactory.rehydrateUser(
                roleStr, id, username, password, fullName, email, balance, lockedBalance, isActive);

        if (obj.has("createdAt") && !obj.get("createdAt").isJsonNull()) {
            user.setCreatedAt(context.deserialize(obj.get("createdAt"), LocalDateTime.class));
        }
        if (obj.has("updatedAt") && !obj.get("updatedAt").isJsonNull()) {
            user.setUpdatedAt(context.deserialize(obj.get("updatedAt"), LocalDateTime.class));
        }
        if (obj.has("isDeleted") && !obj.get("isDeleted").isJsonNull()) {
            user.setDeleted(obj.get("isDeleted").getAsBoolean());
        }

        return user;
    }
}
