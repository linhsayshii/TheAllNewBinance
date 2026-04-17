# Huong Dan Trien Khai Database Cho Module Server

Tai lieu nay huong dan khoi tao CSDL MySQL cho module `server` bang file `schema.sql`.

## 1. Yeu cau moi truong

- MySQL 8.x
- Java 21
- Maven 3.9+

## 2. Tao CSDL va bang

Tu thu muc goc du an, chay lenh:

```bash
mysql -u root -p < server/src/resource/schema.sql
```

Sau khi chay xong, script se tao:

- Database: `theallnewbinance`
- User: `binance`@`localhost`
- Password: `PasswordCucManh!`

Va cac bang:

- `users`
- `items`
- `auctions`
- `bids`

## 3. Cau hinh ket noi trong server

Mo file:

- `server/src/main/java/com/auction/server/dao/DBConnection.java`

Cap nhat 3 hang sau:

```java
private static final String DB_NAME = "theallnewbinance";
private static final String USER = "checkin_user";
private static final String PASSWORD = "PasswordCucManh!";
```

Luu y:

- URL trong code dang dung host `localhost:3306`.
- Neu ban dung host/port khac, sua hang `URL` trong `DBConnection`.

## 4. Build va chay server

Tu thu muc goc du an:

```bash
mvn -pl server -am clean compile
mvn -pl server -am exec:java -Dexec.mainClass=com.auction.server.ServerApp
```

Neu du an chua cau hinh `exec-maven-plugin`, ban co the chay bang IDE voi class:

- `com.auction.server.ServerApp`

## 5. Kiem tra nhanh

Dang nhap MySQL va kiem tra:

```sql
USE theallnewbinance;
SHOW TABLES;
DESC users;
DESC items;
DESC auctions;
DESC bids;
```

## 6. Ghi chu quan trong

- Ban yeu cau dat file trong `server/src/resource`, tai lieu nay da duoc tao dung vi tri do.
- Theo convention Maven, tai nguyen runtime thuong nam trong `server/src/main/resources`.
- Neu ban muon dong bo theo convention Maven, co the copy 2 file nay sang `server/src/main/resources`.
