package com.auction.core.dto.auction;

/**
 * Abstract base class for polymorphic item attribute payloads transmitted over the network
 * boundary. Each concrete subclass carries the type-safe attributes for one product group.
 *
 * <p>The {@code "type"} discriminator field is injected and consumed by {@link
 * com.auction.core.dto.auction.serialization.ItemAttributesPayloadSerializer} to enable Gson
 * Polymorphic Deserialization at the Socket boundary without {@code ClassCastException} risk.
 */
public abstract class ItemAttributesPayload {}
