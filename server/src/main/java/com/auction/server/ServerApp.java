package com.auction.server;

import com.auction.core.products.attribute.LuxuryAttributes;
import com.auction.core.products.factory.ItemFactoryProvider;
import com.auction.core.services.IAuctionService;
import com.auction.core.services.IBidService;
import com.auction.core.services.IUserService;
import com.auction.server.controller.AuctionController;
import com.auction.server.controller.BidController;
import com.auction.server.controller.ItemController;
import com.auction.server.controller.RequestDispatcher;
import com.auction.server.controller.UserController;
import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.BidDao;
import com.auction.server.dao.ItemDao;
import com.auction.server.dao.UserDao;
import com.auction.server.dao.impl.IAuctionDao;
import com.auction.server.dao.impl.IBidDao;
import com.auction.server.dao.impl.IItemDao;
import com.auction.server.dao.impl.IUserDao;
import com.auction.server.network.SocketServer;
import com.auction.server.services.AuctionService;
import com.auction.server.services.AuctionSettlementScheduler;
import com.auction.server.services.BidQueueManager;
import com.auction.server.services.BidService;
import com.auction.server.services.FeaturedAuctionBatchJob;
import com.auction.server.services.UserService;
import java.util.logging.Logger;

public class ServerApp {

    private static final Logger LOGGER = Logger.getLogger(ServerApp.class.getName());

    public static void main(String[] args) {
        // Đồng bộ múi giờ JVM hệ thống về UTC+7 trước khi bất kỳ class nào khởi tạo
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

        // ── Phase 7: Application Startup Initialization ──────────────────────

        // Step 1: Force JVM class-load of LuxuryAttributes to pre-populate the KEY_POOL.
        // Without this, JVM lazy-loading may leave KEY_POOL empty when the first JSON
        // arrives, causing AttributeKey.getByName() to return null silently.
        // Using LuxuryAttributes.class.getName() (not a String literal) ensures the compiler
        // catches any future package rename or class move immediately.
        try {
            Class.forName(LuxuryAttributes.class.getName());
            LOGGER.info("LuxuryAttributes class loaded – KEY_POOL pre-populated.");
        } catch (ClassNotFoundException e) {
            LOGGER.severe("Failed to load LuxuryAttributes: " + e.getMessage());
            return; // Cannot proceed safely without attribute keys
        }

        // Step 2: Scan SPI descriptors and freeze the ItemFactory registry.
        // Must run before any DAO or WebSocket handler that creates Item instances.
        ItemFactoryProvider.initialize();

        // ── Dependency Injection ──────────────────────────────────────────────

        // 1. Instantiating DAOs
        IUserDao userDao = new UserDao();
        IAuctionDao auctionDao = new AuctionDao();
        IBidDao bidDao = new BidDao();
        IItemDao itemDao = new ItemDao();

        // 2. Dependency Injection - Instantiating Services
        IUserService userService = new UserService(userDao);
        IAuctionService auctionService = new AuctionService(auctionDao, itemDao, userDao);

        // 3. Queue-based bid processing — tiêm trực tiếp DAOs tránh ép kiểu
        BidQueueManager bidQueueManager =
                new BidQueueManager(bidDao, auctionService, auctionDao, userDao);
        IBidService bidService = new BidService(bidDao, auctionService, userDao, bidQueueManager);

        // 4. Dependency Injection - Instantiating Controllers
        UserController userCtrl = new UserController(userService);
        AuctionController auctionCtrl = new AuctionController(auctionService);
        BidController bidCtrl = new BidController(bidService);
        ItemController itemCtrl = new ItemController();

        // 4. Instantiating RequestDispatcher
        RequestDispatcher dispatcher =
                new RequestDispatcher(userCtrl, auctionCtrl, bidCtrl, itemCtrl, userDao);

        // 5. Khởi tạo và kích hoạt Schedulers ngầm
        FeaturedAuctionBatchJob featuredJob = new FeaturedAuctionBatchJob(auctionDao);
        featuredJob.start();

        AuctionSettlementScheduler settlementScheduler =
                new AuctionSettlementScheduler(auctionDao, bidDao, userDao);
        settlementScheduler.start();

        // 6. Start Server
        int port = 8080;
        SocketServer server = new SocketServer(port, dispatcher);
        server.start();

        // Đăng ký Shutdown Hook để giải phóng Thread Pools an toàn
        Runtime.getRuntime()
                .addShutdownHook(
                        new Thread(
                                () -> {
                                    System.out.println(
                                            "[ServerApp] Shutdown hook triggered."
                                                    + " Releasing background pools...");
                                    featuredJob.stop();
                                    settlementScheduler.stop();
                                }));

        System.out.println(
                "TheAllNewBinance Auction Server is warming up and binding to port " + port);
    }
}
