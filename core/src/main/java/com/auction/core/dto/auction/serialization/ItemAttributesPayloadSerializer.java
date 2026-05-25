package com.auction.core.dto.auction.serialization;

import com.auction.core.dto.auction.ArtisticCreationPayload;
import com.auction.core.dto.auction.ItemAttributesPayload;
import com.auction.core.dto.auction.LuxuryCollectiblePayload;
import com.auction.core.dto.auction.PrecisionMechanicalPayload;
import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;

/**
 * Custom Gson adapter implementing Polymorphic Serialization/Deserialization for {@link
 * ItemAttributesPayload} using a {@code "type"} discriminator field.
 *
 * <p><b>StackOverflowError Prevention</b>: A {@code pristineGson} instance — completely isolated
 * from this adapter — is used for the actual field-level serialization. This breaks the infinite
 * recursive delegation loop that would occur if {@code context.serialize(src)} were called instead,
 * since Gson would re-route the call back into this very adapter.
 *
 * <p><b>Future-Proof</b>: Because {@code pristineGson} uses reflection, any new fields added to
 * subclass Payloads are automatically serialized/deserialized without modifying this adapter.
 *
 * <p>Type mappings:
 * <ul>
 *   <li>{@code "luxury"} ↔ {@link LuxuryCollectiblePayload}
 *   <li>{@code "artistic"} ↔ {@link ArtisticCreationPayload}
 *   <li>{@code "precision"} ↔ {@link PrecisionMechanicalPayload}
 * </ul>
 */
public class ItemAttributesPayloadSerializer
        implements JsonSerializer<ItemAttributesPayload>, JsonDeserializer<ItemAttributesPayload> {

    /**
     * Pristine Gson instance — intentionally bare, with NO custom type adapters registered. Used to
     * break the infinite recursive delegation loop during serialization.
     */
    private final Gson pristineGson = new Gson();

    @Override
    public JsonElement serialize(
            ItemAttributesPayload src, Type typeOfSrc, JsonSerializationContext context) {
        // Use pristineGson to safely serialize the concrete subclass fields via reflection.
        // Calling context.serialize(src) would cause infinite recursion back into this adapter.
        JsonObject result = pristineGson.toJsonTree(src).getAsJsonObject();

        if (src instanceof LuxuryCollectiblePayload) {
            result.addProperty("type", "luxury");
        } else if (src instanceof ArtisticCreationPayload) {
            result.addProperty("type", "artistic");
        } else if (src instanceof PrecisionMechanicalPayload) {
            result.addProperty("type", "precision");
        }
        return result;
    }

    @Override
    public ItemAttributesPayload deserialize(
            JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();
        JsonElement typeElem = jsonObject.get("type");
        if (typeElem == null) {
            throw new JsonParseException(
                    "Missing 'type' discriminator field in ItemAttributesPayload JSON payload");
        }
        String type = typeElem.getAsString();
        // pristineGson deserializes directly to the concrete class — no recursive adapter call.
        return switch (type) {
            case "luxury" -> pristineGson.fromJson(json, LuxuryCollectiblePayload.class);
            case "artistic" -> pristineGson.fromJson(json, ArtisticCreationPayload.class);
            case "precision" -> pristineGson.fromJson(json, PrecisionMechanicalPayload.class);
            default ->
                    throw new JsonParseException(
                            "Unknown ItemAttributesPayload type discriminator: " + type);
        };
    }
}
