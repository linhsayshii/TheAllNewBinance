package com.auction.core.products.attribute;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Type-safe key for the Heterogeneous Container pattern. Each key carries its target type at
 * compile time, enabling safe cast-free lookups. Keys auto-register into a static KEY_POOL to
 * support reverse-lookup from flat JSON/DB strings.
 *
 * @param <T> The type of the attribute value this key maps to.
 */
public final class AttributeKey<T> {

    private final String name;
    private final Class<T> type;

    /** Global registry pool for reverse-lookup by plain string name (e.g. from JSON / DB). */
    private static final Map<String, AttributeKey<?>> KEY_POOL = new ConcurrentHashMap<>();

    private AttributeKey(String name, Class<T> type) {
        this.name = name;
        this.type = type;
    }

    /**
     * Factory method: creates a new AttributeKey and registers it into the KEY_POOL. Must be called
     * once per key constant (typically in an Attributes interface static init).
     *
     * @param name Unique string identifier for this key. Leading/trailing whitespace is trimmed.
     * @param type Runtime class token for the value type.
     * @param <T> The value type.
     * @return A new, registered AttributeKey instance.
     */
    public static synchronized <T> AttributeKey<T> of(String name, Class<T> type) {
        AttributeKey<T> key = new AttributeKey<>(name.trim(), type);
        KEY_POOL.put(name.trim(), key);
        return key;
    }

    /**
     * Reverse-lookup an AttributeKey by its plain string name. Used by Gson deserializers and JDBC
     * mappers to convert flat strings back to typed keys.
     *
     * @param name The plain string name, whitespace is trimmed before lookup.
     * @return The registered AttributeKey, or null if not found.
     */
    public static AttributeKey<?> getByName(String name) {
        if (name == null) {
            return null;
        }
        return KEY_POOL.get(name.trim());
    }

    public String getName() {
        return name;
    }

    public Class<T> getType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AttributeKey<?> that = (AttributeKey<?>) o;
        return Objects.equals(name, that.name) && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    @Override
    public String toString() {
        return "AttributeKey{name='" + name + "', type=" + type.getSimpleName() + "}";
    }
}
