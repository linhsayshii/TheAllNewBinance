# Class Diagram - TheAllNewBinance

Tài liệu này mô tả sơ đồ lớp tổng quan của dự án theo 3 module: `core`, `server`, `client`.

```mermaid
classDiagram
    direction LR

    namespace Core {
        class Entity
        class User
        class StandardUser
        class Admin
        class Item
        class Auction
        class Bid
        class EventType

        class IUserService {
            <<interface>>
        }
        class IAuctionService {
            <<interface>>
        }
        class IBidService {
            <<interface>>
        }
        class IItemService {
            <<interface>>
        }

        Entity <|-- User
        User <|-- StandardUser
        User <|-- Admin
        Entity <|-- Item
        Entity <|-- Auction
        Entity <|-- Bid
    }

    namespace Server {
        class DBConnection

        class IUserDao {
            <<interface>>
        }
        class IAuctionDao {
            <<interface>>
        }
        class IBidDao {
            <<interface>>
        }
        class IItemDao {
            <<interface>>
        }

        class UserDao
        class AuctionDao
        class BidDao
        class ItemDao

        class UserService
        class AuctionService
        class BidService
        class ItemService

        class UserController
        class AuctionController
        class BidController
        class RequestDispatcher
        class SocketServer
        class ServerApp

        IUserDao <|.. UserDao
        IAuctionDao <|.. AuctionDao
        IBidDao <|.. BidDao
        IItemDao <|.. ItemDao

        UserDao --> DBConnection
        AuctionDao --> DBConnection
        BidDao --> DBConnection
        ItemDao --> DBConnection

        IUserService <|.. UserService
        IAuctionService <|.. AuctionService
        IBidService <|.. BidService
        IItemService <|.. ItemService

        UserService --> IUserDao
        AuctionService --> IAuctionDao
        BidService --> IBidDao
        BidService --> IAuctionService
        BidService --> IUserDao
        ItemService --> IItemDao

        UserController --> IUserService
        AuctionController --> IAuctionService
        BidController --> IBidService

        RequestDispatcher --> UserController
        RequestDispatcher --> AuctionController
        RequestDispatcher --> BidController
        SocketServer --> RequestDispatcher
        ServerApp --> SocketServer
    }

    namespace Client {
        class ClientApp
        class AuctionClient
        class NetworkService
        class UserSessionService
        class AuctionQueryService
        class SceneService
        class NavigationService
        class LifecycleAwareController {
            <<interface>>
        }

        class LoginPageController
        class LoginPageViewModel
        class RegisterPageController
        class RegisterPageViewModel
        class GeneralPageController
        class GeneralPageViewModel
        class ProductDetailPageController
        class ProductDetailPageViewModel
        class ProfilePageController
        class ProfilePageViewModel

        class AuctionCardComponentController
        class HeaderComponentController
        class NavbarComponentController
        class ThemeToggleComponentController
        class SearchBarComponentController

        NetworkService --> AuctionClient
        AuctionQueryService --> NetworkService
        NavigationService --> SceneService

        LoginPageController --> LoginPageViewModel
        LoginPageController --> NetworkService
        LoginPageController --> UserSessionService

        RegisterPageController --> RegisterPageViewModel
        RegisterPageController --> NetworkService
        RegisterPageController --> UserSessionService

        GeneralPageController --> GeneralPageViewModel
        GeneralPageViewModel --> AuctionQueryService

        ProductDetailPageController --> ProductDetailPageViewModel
        ProductDetailPageController --> NetworkService

        ProfilePageController --> ProfilePageViewModel
        ProfilePageController --> UserSessionService

        SceneService --> LifecycleAwareController
        ClientApp --> NetworkService
        ClientApp --> SceneService
        ClientApp --> NavigationService
    }

    UserSessionService --> User
    AuctionService --> Auction
    BidService --> Bid
    AuctionDao --> Auction
    BidDao --> Bid
    ItemDao --> Item
    UserDao --> User
    AuctionClient ..> EventType : message type
```
