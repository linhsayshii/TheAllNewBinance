package com.auction.server;

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
import com.auction.server.services.BidQueueManager;
import com.auction.server.services.BidService;
import com.auction.server.services.FeaturedAuctionBatchJob;
import com.auction.server.services.UserService;

public class ServerApp {
    public static void main(String[] args) {
        // 1. Dependency Injection - Instantiating DAOs
        IUserDao userDao = new UserDao();
        IAuctionDao auctionDao = new AuctionDao();
        IBidDao bidDao = new BidDao();
        IItemDao itemDao = new ItemDao();

        // 2. Dependency Injection - Instantiating Services
        IUserService userService = new UserService(userDao);
        IAuctionService auctionService = new AuctionService(auctionDao, itemDao, userDao);

        // 3. Queue-based bid processing (replaces DB row lock)
        BidQueueManager bidQueueManager = new BidQueueManager(bidDao, auctionService);
        IBidService bidService = new BidService(bidDao, auctionService, userDao, bidQueueManager);

        // 4. Dependency Injection - Instantiating Controllers
        UserController userCtrl = new UserController(userService);
        AuctionController auctionCtrl = new AuctionController(auctionService);
        BidController bidCtrl = new BidController(bidService);
        ItemController itemCtrl = new ItemController();

        // 4. Instantiating RequestDispatcher
        RequestDispatcher dispatcher = new RequestDispatcher(userCtrl, auctionCtrl, bidCtrl);

        // 6. Start Server
        int port = 8080;
        SocketServer server = new SocketServer(port, dispatcher);
        server.start();

        System.out.println("TheAllNewBinance Auction Server is warming up and binding to port " + port);
    }
}
