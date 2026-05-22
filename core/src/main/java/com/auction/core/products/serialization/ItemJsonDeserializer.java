package com.auction.core.products.serialization;

import com.auction.core.products.CategoryType;
import com.auction.core.products.Item;
import com.auction.core.products.LuxuryCollectible;
import com.auction.core.products.attribute.AttributeKey;
import com.auction.core.products.factory.ItemFactoryProvider;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Polymorphic Gson deserializer for {@link Item} and its sealed subclasses.
 *
 * <p>Deserializing workflow:
 *
 * <ol>
 *   <li>Read {@code category} field to determine target subclass.
 *   <li>Collect all remaining fields from the flat JSON into a raw Map (numbers as Double).
 *   <li><b>CRITICAL FIX</b>: Normalize all Number values to their target types (via {@link
 *       AttributeKey#getByName}) <em>before</em> calling the factory, so that concrete factories
 *       and constructors receive correctly-typed values.
 *   <li>Delegate to {@link ItemFactoryProvider} to construct the correct subclass.
 *   <li>For {@link LuxuryCollectible}, populate the Heterogeneous Container with recognized dynamic
 *       attribute keys.
 * </ol>
 */
public class ItemJsonDeserializer implements JsonDeserializer<Item> {

    private static final java.util.Set<String> COMMON_FIELDS =
            java.util.Set.of(
                    "id", "sellerId", "name", "description", "category", "imageUrl", "isDeleted");

    @Override
    public Item deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {

        JsonObject obj = json.getAsJsonObject();

        // 1. Resolve category
        if (!obj.has("category")) {
            throw new JsonParseException("Item JSON is missing required field 'category'");
        }
        String categoryStr = obj.get("category").getAsString();
        CategoryType category;
        try {
            category = CategoryType.valueOf(categoryStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new JsonParseException("Unknown item category: " + categoryStr, e);
        }

        // 2. Extract common fields
        Integer id = obj.has("id") ? obj.get("id").getAsInt() : null;
        Integer sellerId = obj.has("sellerId") ? obj.get("sellerId").getAsInt() : null;
        String name = obj.has("name") ? obj.get("name").getAsString() : "";
        String description = obj.has("description") ? obj.get("description").getAsString() : "";
        String imageUrl = obj.has("imageUrl") ? obj.get("imageUrl").getAsString() : "";
        Boolean isDeleted = obj.has("isDeleted") ? obj.get("isDeleted").getAsBoolean() : false;

        // 3. Collect remaining fields as raw attributes (numbers kept as Double for now)
        Map<String, Object> rawAttrs = new HashMap<>();
        rawAttrs.put("category", categoryStr); // factory needs it to set category on the item
        for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
            String key = entry.getKey();
            if (COMMON_FIELDS.contains(key)) {
                continue;
            }
            JsonElement val = entry.getValue();
            if (val.isJsonNull()) {
                rawAttrs.put(key, null);
            } else if (val.isJsonPrimitive()) {
                JsonPrimitive primitive = val.getAsJsonPrimitive();
                if (primitive.isBoolean()) {
                    rawAttrs.put(key, primitive.getAsBoolean());
                } else if (primitive.isString()) {
                    rawAttrs.put(key, primitive.getAsString());
                } else if (primitive.isNumber()) {
                    rawAttrs.put(key, primitive.getAsDouble()); // raw Double from Gson
                }
            }
        }

        // 4. CRITICAL FIX: Normalize all numbers to their declared target types BEFORE factory call
        Map<String, Object> normalizedAttrs = new HashMap<>();
        for (Map.Entry<String, Object> entry : rawAttrs.entrySet()) {
            String keyStr = entry.getKey();
            Object rawVal = entry.getValue();
            AttributeKey<?> attrKey = AttributeKey.getByName(keyStr);
            if (attrKey != null && rawVal != null) {
                normalizedAttrs.put(keyStr, castToTargetType(rawVal, attrKey.getType()));
            } else {
                normalizedAttrs.put(keyStr, rawVal);
            }
        }

        // 5. Delegate creation to the registered factory
        Item item =
                ItemFactoryProvider.getFactory(category)
                        .createItem(
                                id,
                                sellerId,
                                name,
                                description,
                                imageUrl,
                                isDeleted,
                                normalizedAttrs);

        // 6. Populate Heterogeneous Container for LuxuryCollectible
        if (item instanceof LuxuryCollectible lc) {
            for (Map.Entry<String, Object> entry : normalizedAttrs.entrySet()) {
                AttributeKey<?> key = AttributeKey.getByName(entry.getKey());
                if (key != null && entry.getValue() != null) {
                    putAttributeHelper(lc, key, entry.getValue());
                }
            }
        }

        return item;
    }

    /**
     * Converts a raw {@link Number} (as produced by Gson or JDBC) to the declared target type.
     * Using {@code Number} as the check covers all numeric sources: Gson produces Double, JDBC
     * produces Integer / Long / BigDecimal depending on the driver.
     */
    private Object castToTargetType(Object rawVal, Class<?> targetType) {
        if (rawVal instanceof Number numberVal) {
            if (targetType == Integer.class || targetType == int.class) {
                return numberVal.intValue();
            } else if (targetType == Long.class || targetType == long.class) {
                return numberVal.longValue();
            } else if (targetType == Float.class || targetType == float.class) {
                return numberVal.floatValue();
            } else if (targetType == Double.class || targetType == double.class) {
                return numberVal.doubleValue();
            }
        }
        return rawVal;
    }

    @SuppressWarnings("unchecked")
    private <T> void putAttributeHelper(LuxuryCollectible lc, AttributeKey<T> key, Object value) {
        lc.putAttribute(key, (T) value);
    }
}
