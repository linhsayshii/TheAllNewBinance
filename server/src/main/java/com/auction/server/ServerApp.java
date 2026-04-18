package com.auction.server;

import com.auction.server.dao.IAuctionDao;
import com.auction.server.dao.IBidDao;
import com.auction.server.dao.IUserDao;
import com.auction.core.services.IAuctionService;
import com.auction.core.services.IBidService;
import com.auction.core.services.IUserService;
import com.auction.server.controller.AuctionController;
import com.auction.server.controller.BidController;
import com.auction.server.controller.RequestDispatcher;
import com.auction.server.controller.UserController;
import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.BidDao;
import com.auction.server.dao.UserDao;
import com.auction.server.network.SocketServer;
import com.auction.server.services.AuctionService;
import com.auction.server.services.BidService;
import com.auction.server.services.UserService;

public class ServerApp {
    public static void main(String[] args) {
        // 1. Dependency Injection - Instantiating DAOs
        IUserDao userDao = new UserDao();
        IAuctionDao auctionDao = new AuctionDao();
        IBidDao bidDao = new BidDao();

        // 2. Dependency Injection - Instantiating Services
        IUserService userService = new UserService(userDao);
        IAuctionService auctionService = new AuctionService(auctionDao);
        IBidService bidService = new BidService(bidDao, auctionService, userDao);

        // 3. Dependency Injection - Instantiating Controllers
        UserController userCtrl = new UserController(userService);
        AuctionController auctionCtrl = new AuctionController(auctionService);
        BidController bidCtrl = new BidController(bidService);

        // 4. Instantiating RequestDispatcher
        RequestDispatcher dispatcher = new RequestDispatcher(userCtrl, auctionCtrl, bidCtrl);

        // 5. Start Server
        int port = 8080;
        SocketServer server = new SocketServer(port, dispatcher);
        server.start();

        System.out.println("TheAllNewBinance Auction Server is warming up and binding to port " + port);
    }
}
