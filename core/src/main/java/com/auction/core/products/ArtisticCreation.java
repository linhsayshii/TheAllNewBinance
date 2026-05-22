package com.auction.core.products;

/**
 * Concrete sealed subclass for art and music items. Covers categories: ART, MUSIC.
 *
 * <p>Fixed fields: artist name and year of creation.
 */
public final class ArtisticCreation extends Item {

    private final String artist;
    private final Integer yearCreated;

    public ArtisticCreation(
            Integer id,
            Integer sellerId,
            String name,
            String description,
            CategoryType category,
            String imageUrl,
            Boolean isDeleted,
            String artist,
            Integer yearCreated) {
        super(id, sellerId, name, description, category, imageUrl, isDeleted);
        this.artist = artist;
        this.yearCreated = yearCreated;
    }

    public String getArtist() {
        return artist;
    }

    public Integer getYearCreated() {
        return yearCreated;
    }

    @Override
    public String getDetailedSpecs() {
        String year = yearCreated != null ? String.valueOf(yearCreated) : "Unknown";
        return String.format("Artist: %s | Created In: %s", artist, year);
    }
}
