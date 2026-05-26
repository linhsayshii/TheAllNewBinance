# Diff Details

Date : 2026-05-25 19:37:19

Directory /mnt/windows-data/01_learn/k70_year_1/term_2/lap_trinh_nang_cao/projects/TheAllNewBinance

Total : 46 files,  1057 codes, 392 comments, 219 blanks, all 1668 lines

[Summary](results.md) / [Details](details.md) / [Diff Summary](diff.md) / Diff Details

## Files
| filename | language | code | comment | blank | total |
| :--- | :--- | ---: | ---: | ---: | ---: |
| [client/src/main/java/com/auction/client/app/ClientApp.java](/client/src/main/java/com/auction/client/app/ClientApp.java) | Java | 9 | 2 | 3 | 14 |
| [client/src/main/java/com/auction/client/mock/MockDataProvider.java](/client/src/main/java/com/auction/client/mock/MockDataProvider.java) | Java | 9 | 1 | 1 | 11 |
| [client/src/main/java/com/auction/client/page/auction/AuctionPageController.java](/client/src/main/java/com/auction/client/page/auction/AuctionPageController.java) | Java | 18 | 6 | 2 | 26 |
| [client/src/main/java/com/auction/client/page/create/CreateListingController.java](/client/src/main/java/com/auction/client/page/create/CreateListingController.java) | Java | 59 | 11 | 9 | 79 |
| [client/src/main/java/com/auction/client/page/create/strategy/ArtisticCreationStrategy.java](/client/src/main/java/com/auction/client/page/create/strategy/ArtisticCreationStrategy.java) | Java | 65 | 6 | 11 | 82 |
| [client/src/main/java/com/auction/client/page/create/strategy/CategoryDisplayStrategy.java](/client/src/main/java/com/auction/client/page/create/strategy/CategoryDisplayStrategy.java) | Java | 11 | 47 | 7 | 65 |
| [client/src/main/java/com/auction/client/page/create/strategy/LuxuryCollectibleStrategy.java](/client/src/main/java/com/auction/client/page/create/strategy/LuxuryCollectibleStrategy.java) | Java | 103 | 13 | 15 | 131 |
| [client/src/main/java/com/auction/client/page/create/strategy/PrecisionMechanicalStrategy.java](/client/src/main/java/com/auction/client/page/create/strategy/PrecisionMechanicalStrategy.java) | Java | 65 | 6 | 11 | 82 |
| [client/src/main/java/com/auction/client/page/productdetail/ProductDetailPageController.java](/client/src/main/java/com/auction/client/page/productdetail/ProductDetailPageController.java) | Java | 18 | 6 | 1 | 25 |
| [client/src/main/resources/fxml/pages/create-listing-page.fxml](/client/src/main/resources/fxml/pages/create-listing-page.fxml) | XML | 2 | 1 | 1 | 4 |
| [client/src/test/java/com/auction/client/unit/page/auction/AuctionPageViewModelTest.java](/client/src/test/java/com/auction/client/unit/page/auction/AuctionPageViewModelTest.java) | Java | 4 | 0 | 1 | 5 |
| [core/src/main/java/com/auction/core/dto/auction/ArtisticCreationPayload.java](/core/src/main/java/com/auction/core/dto/auction/ArtisticCreationPayload.java) | Java | 18 | 6 | 9 | 33 |
| [core/src/main/java/com/auction/core/dto/auction/CreateAuctionRequest.java](/core/src/main/java/com/auction/core/dto/auction/CreateAuctionRequest.java) | Java | 9 | 4 | 3 | 16 |
| [core/src/main/java/com/auction/core/dto/auction/ItemAttributesPayload.java](/core/src/main/java/com/auction/core/dto/auction/ItemAttributesPayload.java) | Java | 2 | 8 | 2 | 12 |
| [core/src/main/java/com/auction/core/dto/auction/LuxuryCollectiblePayload.java](/core/src/main/java/com/auction/core/dto/auction/LuxuryCollectiblePayload.java) | Java | 46 | 8 | 19 | 73 |
| [core/src/main/java/com/auction/core/dto/auction/PrecisionMechanicalPayload.java](/core/src/main/java/com/auction/core/dto/auction/PrecisionMechanicalPayload.java) | Java | 18 | 8 | 9 | 35 |
| [core/src/main/java/com/auction/core/dto/auction/serialization/ItemAttributesPayloadSerializer.java](/core/src/main/java/com/auction/core/dto/auction/serialization/ItemAttributesPayloadSerializer.java) | Java | 51 | 26 | 7 | 84 |
| [core/src/main/java/com/auction/core/dto/user/UserDto.java](/core/src/main/java/com/auction/core/dto/user/UserDto.java) | Java | 40 | 8 | 7 | 55 |
| [core/src/main/java/com/auction/core/products/factory/ArtisticCreationFactory.java](/core/src/main/java/com/auction/core/products/factory/ArtisticCreationFactory.java) | Java | -17 | 4 | -3 | -16 |
| [core/src/main/java/com/auction/core/products/factory/ItemFactory.java](/core/src/main/java/com/auction/core/products/factory/ItemFactory.java) | Java | 0 | -2 | 0 | -2 |
| [core/src/main/java/com/auction/core/products/factory/LuxuryCollectibleFactory.java](/core/src/main/java/com/auction/core/products/factory/LuxuryCollectibleFactory.java) | Java | -10 | -6 | -4 | -20 |
| [core/src/main/java/com/auction/core/products/factory/PrecisionMechanicalFactory.java](/core/src/main/java/com/auction/core/products/factory/PrecisionMechanicalFactory.java) | Java | -17 | 2 | -3 | -18 |
| [core/src/main/java/com/auction/core/products/serialization/ItemJsonDeserializer.java](/core/src/main/java/com/auction/core/products/serialization/ItemJsonDeserializer.java) | Java | 19 | 3 | 2 | 24 |
| [core/src/main/java/com/auction/core/protocol/EventType.java](/core/src/main/java/com/auction/core/protocol/EventType.java) | Java | 2 | 0 | 0 | 2 |
| [core/src/main/java/com/auction/core/users/Admin.java](/core/src/main/java/com/auction/core/users/Admin.java) | Java | 7 | 6 | 3 | 16 |
| [core/src/main/java/com/auction/core/users/StandardUser.java](/core/src/main/java/com/auction/core/users/StandardUser.java) | Java | 11 | 8 | 4 | 23 |
| [core/src/main/java/com/auction/core/users/User.java](/core/src/main/java/com/auction/core/users/User.java) | Java | 38 | 45 | 8 | 91 |
| [core/src/main/java/com/auction/core/users/UserFactory.java](/core/src/main/java/com/auction/core/users/UserFactory.java) | Java | 56 | 39 | 8 | 103 |
| [core/src/main/java/com/auction/core/users/serialization/UserJsonDeserializer.java](/core/src/main/java/com/auction/core/users/serialization/UserJsonDeserializer.java) | Java | 48 | 3 | 9 | 60 |
| [core/src/main/java/com/auction/core/utils/JsonMapper.java](/core/src/main/java/com/auction/core/utils/JsonMapper.java) | Java | 5 | 2 | 0 | 7 |
| [project\_evaluation\_final.md](/project_evaluation_final.md) | Markdown | 98 | 0 | 28 | 126 |
| [server/src/main/java/com/auction/server/concurrency/DBExecutor.java](/server/src/main/java/com/auction/server/concurrency/DBExecutor.java) | Java | 12 | 33 | 7 | 52 |
| [server/src/main/java/com/auction/server/controller/BaseController.java](/server/src/main/java/com/auction/server/controller/BaseController.java) | Java | 3 | 0 | 0 | 3 |
| [server/src/main/java/com/auction/server/dao/AuctionDao.java](/server/src/main/java/com/auction/server/dao/AuctionDao.java) | Java | 16 | 2 | 4 | 22 |
| [server/src/main/java/com/auction/server/dao/ItemDao.java](/server/src/main/java/com/auction/server/dao/ItemDao.java) | Java | 0 | 2 | -2 | 0 |
| [server/src/main/java/com/auction/server/dao/UserDao.java](/server/src/main/java/com/auction/server/dao/UserDao.java) | Java | 27 | 8 | 2 | 37 |
| [server/src/main/java/com/auction/server/dao/impl/IAuctionDao.java](/server/src/main/java/com/auction/server/dao/impl/IAuctionDao.java) | Java | 3 | 4 | 1 | 8 |
| [server/src/main/java/com/auction/server/dao/impl/IItemDao.java](/server/src/main/java/com/auction/server/dao/impl/IItemDao.java) | Java | 3 | 4 | 1 | 8 |
| [server/src/main/java/com/auction/server/network/BroadcastBroker.java](/server/src/main/java/com/auction/server/network/BroadcastBroker.java) | Java | 61 | 41 | 15 | 117 |
| [server/src/main/java/com/auction/server/network/SocketServer.java](/server/src/main/java/com/auction/server/network/SocketServer.java) | Java | 59 | 12 | 11 | 82 |
| [server/src/main/java/com/auction/server/services/AuctionService.java](/server/src/main/java/com/auction/server/services/AuctionService.java) | Java | 52 | 11 | 8 | 71 |
| [server/src/main/java/com/auction/server/services/BidService.java](/server/src/main/java/com/auction/server/services/BidService.java) | Java | 2 | 0 | 0 | 2 |
| [server/src/main/java/com/auction/server/services/ItemService.java](/server/src/main/java/com/auction/server/services/ItemService.java) | Java | 16 | 3 | 1 | 20 |
| [server/src/main/java/com/auction/server/services/UserService.java](/server/src/main/java/com/auction/server/services/UserService.java) | Java | -3 | 0 | 0 | -3 |
| [server/src/resource/schema.sql](/server/src/resource/schema.sql) | MS SQL | 8 | 1 | 1 | 10 |
| [server/src/test/java/com/auction/server/services/BidServiceTest.java](/server/src/test/java/com/auction/server/services/BidServiceTest.java) | Java | 11 | 0 | 0 | 11 |

[Summary](results.md) / [Details](details.md) / [Diff Summary](diff.md) / Diff Details