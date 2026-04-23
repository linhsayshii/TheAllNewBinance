package com.auction.core.products;

import com.auction.core.Entity;

public class Item extends Entity {
    private Integer id;         // Khớp INT AUTO_INCREMENT
    private Integer sellerId;   // Người bán (FK)
    private String name;
    private String description;
    private String category;
    private String imageUrl;

    public Item(Integer id, Integer sellerId, String name, String description, String category, String imageUrl, Boolean isDeleted) {
        super();
        this.id = id;
        this.sellerId = sellerId;
        this.name = name;
        this.description = description;
        this.category = category;
        this.imageUrl = imageUrl;
        this.isDeleted = isDeleted != null ? isDeleted : false;
    }
    /**
    public Item(Integer id, Integer sellerId, String name, String description, String category, String imageUrl) {
     this(id, sellerId, name, description, category, imageUrl, false);
    }
    */

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public Integer getSellerId() { return sellerId; }
    public void setSellerId(Integer sellerId) { this.sellerId = sellerId; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; updateTimestamp(); }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; updateTimestamp(); }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; updateTimestamp(); }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; updateTimestamp(); }
}
