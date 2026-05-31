package com.auction.core.products.serialization;

import com.auction.core.dto.auction.ArtisticCreationPayload;
import com.auction.core.dto.auction.ItemAttributesPayload;
import com.auction.core.dto.auction.LuxuryCollectiblePayload;
import com.auction.core.dto.auction.PrecisionMechanicalPayload;
import com.auction.core.products.CategoryType;
import com.auction.core.products.Item;
import com.auction.core.products.LuxuryCollectible;
import com.auction.core.products.attribute.LuxuryAttributes;
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
 * <p>This deserializer is used at the <em>Client boundary</em> when receiving an {@code Item}
 * embedded inside an {@code Auction} response JSON. It reads the flat JSON representation and
 * reconstructs a strongly-typed {@link ItemAttributesPayload} before delegating to the factory,
 * eliminating all raw Map access and manual type casting.
 *
 * <p>Deserializing workflow:
 *
 * <ol>
 *   <li>Read {@code category} field to determine target product group and subclass.
 *   <li>Extract common fields (id, sellerId, name, etc.).
 *   <li>Collect product-group-specific fields from the JSON and construct the appropriate {@link
 *       ItemAttributesPayload} subclass.
 *   <li>Delegate to {@link ItemFactoryProvider} passing the strongly-typed payload.
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

        // 3. Collect all non-common fields into a raw map for payload construction
        Map<String, Object> rawAttrs = new HashMap<>();
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
                    rawAttrs.put(key, primitive.getAsDouble());
                }
            }
        }

        // 4. Build strongly-typed payload from raw fields
        ItemAttributesPayload payload = buildPayload(category, rawAttrs);

        // 5. Delegate creation to the registered factory with the typed payload
        Item item =
                ItemFactoryProvider.getFactory(category)
                        .createItem(id, sellerId, name, description, imageUrl, isDeleted, payload);

        // 6. For LuxuryCollectible, re-inflate dynamic attributes into the Heterogeneous Container.
        // The factory constructor sets fixed fields; dynamic container must be populated
        // separately.
        if (item instanceof LuxuryCollectible lc && payload instanceof LuxuryCollectiblePayload p) {
            if (p.getWatchMovement() != null) {
                lc.putAttribute(LuxuryAttributes.WATCH_MOVEMENT, p.getWatchMovement());
            }
            if (p.getBottleSize() != null) {
                lc.putAttribute(LuxuryAttributes.BOTTLE_SIZE, p.getBottleSize());
            }
            if (p.getFashionSize() != null) {
                lc.putAttribute(LuxuryAttributes.FASHION_SIZE, p.getFashionSize());
            }
        }

        return item;
    }

    /**
     * Builds a strongly-typed {@link ItemAttributesPayload} subclass from the raw field map,
     * normalizing Gson's default Double numbers to their correct target types.
     */
    private ItemAttributesPayload buildPayload(CategoryType category, Map<String, Object> raw) {
        return switch (category) {
            case WATCHES, FASHION, COLLECTIBLES, WINE -> {
                LuxuryCollectiblePayload p = new LuxuryCollectiblePayload();
                p.setBrand(getString(raw, "brand"));
                p.setCondition(getString(raw, "condition", getString(raw, "itemCondition")));
                Object certObj = raw.get("hasCertificate");
                p.setHasCertificate(Boolean.TRUE.equals(certObj));
                p.setWatchMovement(getString(raw, LuxuryAttributes.WATCH_MOVEMENT.getName()));
                Number bottle = getNumber(raw, LuxuryAttributes.BOTTLE_SIZE.getName());
                if (bottle != null) {
                    p.setBottleSize(bottle.doubleValue());
                }
                p.setFashionSize(getString(raw, LuxuryAttributes.FASHION_SIZE.getName()));
                yield p;
            }
            case ART, MUSIC -> {
                ArtisticCreationPayload p = new ArtisticCreationPayload();
                p.setArtist(getString(raw, "artist"));
                Number year = getNumber(raw, "yearCreated");
                if (year != null) {
                    p.setYearCreated(year.intValue());
                }
                yield p;
            }
            case SPORTS, CAMERAS -> {
                PrecisionMechanicalPayload p = new PrecisionMechanicalPayload();
                p.setModel(getString(raw, "model"));
                Number warranty = getNumber(raw, "warrantyMonths");
                if (warranty != null) {
                    p.setWarrantyMonths(warranty.intValue());
                }
                yield p;
            }
        };
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val instanceof String s ? s : null;
    }

    /** Allows a primary key and an alternate key name (e.g. DB column vs DTO field naming). */
    private String getString(Map<String, Object> map, String primaryKey, String alternateValue) {
        String primary = getString(map, primaryKey);
        return primary != null ? primary : alternateValue;
    }

    private Number getNumber(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val instanceof Number n ? n : null;
    }
}
