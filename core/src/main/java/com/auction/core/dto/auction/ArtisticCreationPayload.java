package com.auction.core.dto.auction;

/**
 * Strongly-typed network payload for ArtisticCreation items (ART, MUSIC). All fields are declared
 * with their precise Java target types to prevent Gson-induced ClassCastException at the Server's
 * deserialization boundary.
 */
public class ArtisticCreationPayload extends ItemAttributesPayload {

    private String artist;

    /** Year the work was created. Declared as Integer (not int) to allow null from optional UI. */
    private Integer yearCreated;

    public ArtisticCreationPayload() {}

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public Integer getYearCreated() {
        return yearCreated;
    }

    public void setYearCreated(Integer yearCreated) {
        this.yearCreated = yearCreated;
    }
}
