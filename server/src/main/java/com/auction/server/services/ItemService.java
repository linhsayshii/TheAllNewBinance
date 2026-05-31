package com.auction.server.services;

import com.auction.core.dto.auction.ArtisticCreationPayload;
import com.auction.core.dto.auction.ItemAttributesPayload;
import com.auction.core.dto.auction.LuxuryCollectiblePayload;
import com.auction.core.dto.auction.PrecisionMechanicalPayload;
import com.auction.core.products.CategoryType;
import com.auction.core.products.Item;
import com.auction.core.products.factory.ItemFactoryProvider;
import com.auction.core.services.IItemService;
import com.auction.server.dao.impl.IItemDao;
import java.util.concurrent.CompletableFuture;

public class ItemService implements IItemService {
    private final IItemDao itemDao;

    public ItemService(IItemDao item) {
        this.itemDao = item;
    }

    @Override
    public CompletableFuture<Item> addProduct(
            int sellerId, String name, String description, String category, String imageUrl) {
        return CompletableFuture.supplyAsync(
                () -> {
                    CategoryType catEnum = CategoryType.valueOf(category.trim().toUpperCase());

                    // Build a minimal default payload for each product group.
                    // Full attribute data is provided via CreateAuctionRequest in normal flow;
                    // this method is a fallback for simple item creation without attributes.
                    ItemAttributesPayload defaultPayload;
                    switch (catEnum) {
                        case WATCHES:
                        case FASHION:
                        case COLLECTIBLES:
                        case WINE:
                            defaultPayload = new LuxuryCollectiblePayload();
                            break;
                        case ART:
                        case MUSIC:
                            defaultPayload = new ArtisticCreationPayload();
                            break;
                        default:
                            defaultPayload = new PrecisionMechanicalPayload();
                            break;
                    }

                    Item item =
                            ItemFactoryProvider.getFactory(catEnum)
                                    .createItem(
                                            null,
                                            sellerId,
                                            name,
                                            description,
                                            imageUrl,
                                            false,
                                            defaultPayload);
                    itemDao.addItem(item);
                    return item;
                });
    }

    @Override
    public CompletableFuture<Void> updateProduct(Item item) {
        return CompletableFuture.runAsync(() -> itemDao.updateItem(item));
    }

    @Override
    public CompletableFuture<Void> removeProduct(Item item) {
        return CompletableFuture.runAsync(
                () -> {
                    item.setDeleted(true);
                    itemDao.deleteItem(item);
                });
    }
}
