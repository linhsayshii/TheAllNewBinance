# 🏆 Báo Cáo Chấm Điểm Dự Án – TheAllNewBinance
**Môn:** Lập trình nâng cao – HK II, 2025–2026  
**Tên dự án:** TheAllNewBinance – Hệ thống Đấu giá Trực tuyến  
**Thang điểm tối đa:** 10 điểm bắt buộc + 1.5 điểm thưởng  

---

## Tóm tắt kết quả

| Tiêu chí | Điểm tối đa | Điểm đánh giá | Ghi chú tóm tắt |
|:---|:---:|:---:|:---|
| 1. Thiết kế lớp & cây kế thừa | 0.5 | **0.3** | Có nhưng thiếu chiều sâu theo yêu cầu |
| 2. Áp dụng OOP | 1.0 | **0.6** | Tốt về Encapsulation, yếu về đa hình |
| 3. Design Patterns | 1.0 | **0.5** | Singleton OK; Factory & Observer rất yếu |
| 4. Quản lý người dùng & sản phẩm | 1.0 | **0.7** | Login/register/CRUD đủ; thiếu Update/Delete |
| 5. Chức năng đấu giá | 1.0 | **0.8** | Realtime bidding hoạt động; trạng thái logic tốt |
| 6. Xử lý lỗi & ngoại lệ | 1.0 | **0.4** | Custom exception cực kỳ thiếu |
| 7. Xử lý đồng thời (Concurrency) | 1.0 | **0.7** | DB-level locking tốt; client-side cần cải thiện |
| 8. Cập nhật thời gian thực (Realtime) | 0.5 | **0.4** | WebSocket OK, nhưng broadcast bị giới hạn |
| 9. Kiến trúc Client–Server | 0.5 | **0.4** | Kiến trúc rõ; giao tiếp WebSocket/JSON |
| 10. Áp dụng MVC | 0.5 | **0.4** | Controller/ViewModel/FXML tốt; DAO server OK |
| 11. Maven & Coding Convention | 0.5 | **0.2** | Maven OK; **Checkstyle hoàn toàn vắng mặt** |
| 12. Unit Test (JUnit) | 0.5 | **0.2** | 15 test file nhưng chất lượng thấp; coverage thấp |
| 13. CI/CD (GitHub Actions) | 0.5 | **0.3** | CI chạy được nhưng `-DskipTests` ở build step |
| **TỔNG BẮTBUỘC** | **10.0** | **~5.9** | |
| 14. Anti-sniping (Bonus) | 0.5 | **0.4** | Đã implement, logic đúng |
| 15. Auto-Bidding (Bonus) | 0.5 | **0** | Không có |
| 16. Bid History Visualization (Bonus) | 0.5 | **0** | Không có |
| **TỔNG THƯỞNG** | **1.5** | **~0.4** | |
| **🏁 TỔNG ĐIỂM ƯỚC TÍNH** | **11.5** | **~6.3 / 10** | |

> **Lưu ý:** Đây là điểm kỹ thuật thuần tuý từ phân tích mã nguồn. Điểm thực tế còn phụ thuộc vào bài thuyết trình, khả năng giải thích code, và đánh giá trực tiếp của giảng viên.

---

## Phân tích chi tiết từng tiêu chí

### 📌 Tiêu chí 1 – Thiết kế lớp & cây kế thừa (0.5đ → **0.3đ**)

**Những gì đã làm được:**
- Có class `Entity` (abstract base) với `createdAt`, `updatedAt`, `isDeleted` — pattern hợp lý.
- `User` → `Admin`, `User` → `StandardUser` — đúng kế thừa.
- `Auction`, `Bid`, `Item` là các entity domain riêng biệt.
- UML class diagram có tồn tại (`uml-class-diagram.png`).

**Điểm trừ nghiêm trọng:**
- Đề bài yêu cầu: `User` (abstract) → `Bidder`, `Seller`, `Admin` — **nhóm merge `Bidder` và `Seller` thành một class `User` duy nhất** với `Role` enum. Đây là thiết kế đơn giản hóa, mất tính đa hình thực sự.
- Đề bài yêu cầu: `Item` (abstract) → `Electronics`, `Art`, `Vehicle` — **Item chỉ là một class plain, không có hệ thống con loại nào**.
- `Admin` và `StandardUser` chỉ là **wrapper rỗng** (7 dòng mỗi cái), không có method hay thuộc tính đặc biệt nào.

```
// Yêu cầu đề bài:
Item (abstract) → Electronics, Art, Vehicle   ❌ Thiếu hoàn toàn
User (abstract) → Bidder, Seller, Admin       ⚠️ Thiếu Bidder/Seller riêng biệt
```

---

### 📌 Tiêu chí 2 – Áp dụng OOP (1.0đ → **0.6đ**)

**Tốt:**
- **Encapsulation:** Tất cả fields đều `private`/`protected` với getter/setter — tốt.
- **Kế thừa:** Có sử dụng (`Admin extends User`, `StandardUser extends User`).
- **Abstraction:** `Entity` abstract, các `IService` interface, `IAuctionDao` interface — tốt.
- `ObservableList`, `Property` trong JavaFX — sử dụng đúng reactive pattern.

**Yếu:**
- **Polymorphism thực sự:** Không có chỗ nào trong code gọi method qua kiểu `User` mà runtime là `Admin` hay `StandardUser`. Polymorphism chỉ tồn tại trên giấy.
- `Admin` và `StandardUser` không override bất kỳ method nào — kế thừa nhưng không có đa hình.
- Không có `@Override` với logic nghiệp vụ khác nhau giữa các role.
- `Item` không có hệ thống kế thừa → mất điểm Abstraction + Polymorphism.

---

### 📌 Tiêu chí 3 – Design Patterns (1.0đ → **0.5đ**)

**Singleton (✅ Khá tốt):**
- `NetworkService.getInstance()`, `UserSessionService.getInstance()`, `ThemeService.getInstance()`, `NavigationService.getInstance()` — **4 Singleton được implement đúng cách**.
- Dùng `synchronized` trên `UserSessionService` — thread-safe Singleton.

**Factory Method (❌ Hoàn toàn vắng mặt):**
- Đề bài yêu cầu Factory Method để tạo các loại `Item` — **không có factory nào**.
- Không có `ItemFactory`, không có `abstract createItem()` — Pattern này bị bỏ qua hoàn toàn.

**Observer (⚠️ Không phải Observer thực sự):**
- Dùng JavaFX Property binding (`addListener`) — đây là **reactive programming của JavaFX**, không phải Observer Pattern theo nghĩa OOP (không có `Observer` interface, `Subject` interface, `notifyObservers()` method).
- WebSocket push từ server tới client — gần với Observer concept, nhưng không được implement như một design pattern bài bản.

**Điểm thưởng:** Server có `RequestDispatcher` — gần với **Command Pattern** (tốt nhưng không được đề cập).

---

### 📌 Tiêu chí 4 – Quản lý người dùng & sản phẩm (1.0đ → **0.7đ**)

**Đã làm được:**
- ✅ Đăng ký user (`RegisterPageController`, `UserController`)
- ✅ Đăng nhập (`LoginPageController`, `UserController.login()`)
- ✅ Đăng xuất (Logout handler trong `SocketServer`)
- ✅ Tạo phiên đấu giá (Create Auction với upload ảnh Cloudinary)
- ✅ Quản lý Admin (`AdminPageController` với listing management)
- ✅ Profile page (xem thông tin, active bids, active listings)
- ✅ Seller profile public page

**Còn thiếu:**
- ❌ Không có chức năng **Update Item** (sửa sản phẩm)
- ❌ Không có chức năng **Delete Item/Auction** cho người bán thông thường
- ❌ Không có quản lý balance/payment (trường `balance` có trong `User` nhưng không có UI hay logic)
- ❌ Password change/update profile có trong server nhưng chưa rõ UI hoàn thiện chưa

---

### 📌 Tiêu chí 5 – Chức năng đấu giá (1.0đ → **0.8đ**)

**Xuất sắc:**
- ✅ Đặt giá realtime qua WebSocket (PLACE_BID → broadcast)
- ✅ Kiểm tra hợp lệ bid: giá > currentPrice + bidIncrement, auction phải ACTIVE
- ✅ Vòng đời auction: PENDING → ACTIVE → ENDED
- ✅ Transition tự động theo thời gian (`updateCountdown()` trong `AuctionPageViewModel`)
- ✅ Xác định winner tự động khi auction kết thúc
- ✅ Snipe extension (anti-sniping) — bonus đã làm
- ✅ Featured/Star Auction system với carousel
- ✅ Mock mode với simulated bids

**Thiếu:**
- ❌ Thiếu logic **tự động đóng phiên đấu giá phía server** (không có scheduled job/timer phía server để chuyển ACTIVE → ENDED)
- ❌ Phiên ENDED chỉ được detect ở phía client (client-side timeout), server không chủ động broadcast "auction ended"

---

### 📌 Tiêu chí 6 – Xử lý lỗi & ngoại lệ (1.0đ → **0.4đ**)

**Điểm nghiêm trọng nhất trong toàn dự án:**

❌ **Custom Exceptions hoàn toàn thiếu:**
- Đề bài yêu cầu: `InvalidBidException`, `AuctionClosedException`, `AuthenticationException`
- Dự án chỉ có **1 custom exception** duy nhất: `SceneLoadException.java` (và đây chỉ là wrap IOException)
- Tất cả lỗi đều dùng `IllegalArgumentException`, `IllegalStateException`, `RuntimeException` — không thể hiện OOP exception hierarchy

**Còn làm:**
- ✅ Try-catch có trong hầu hết các handler
- ✅ Error response được trả về dạng JSON có `success: false` + `message`
- ✅ Null check cơ bản có

**Điểm trừ:**
- Controller throw `new IllegalStateException("Failed to load auction-card component", e)` thay vì custom exception
- MockDataProvider nuốt exception thầm lặng nhiều chỗ (`return new ArrayList<>()` không log)
- Không có exception hierarchy cho domain errors

---

### 📌 Tiêu chí 7 – Xử lý đồng thời / Concurrency (1.0đ → **0.7đ**)

**Tốt:**
- ✅ **SQL `SELECT ... FOR UPDATE`** trong `AuctionDao.updateAuctionForBid()` — pesimistic locking đúng cách để chống race condition ở DB level
- ✅ **Transaction với rollback** trong `updateCurrentPrice()` — atomicity
- ✅ `ConcurrentHashMap` cho `publicAuctionsCache` trong `AuctionService`
- ✅ `ConcurrentHashMap` cho `userSessions` trong `SocketServer`
- ✅ `CompletableFuture.runAsync()` trong `GeneralPageController` để không block UI thread
- ✅ `Platform.runLater()` đúng chỗ ở client

**Yếu:**
- ❌ `MockDataProvider.updateAuctionsStatus()` và `generateSimulatedBidFromOtherUser()` dùng `synchronized` nhưng không consistent (chỉ synchronized method, không synchronized block)
- ❌ Server không dùng Thread Pool tường minh — `WebSocketServer` tự quản lý nhưng không rõ cấu hình
- ❌ Không có `ReentrantLock` hay `volatile` trong logic đấu giá Java thuần (chỉ dùng ở DB level)
- ❌ `AuctionDao.updateCurrentPrice()` có connection leak tiềm ẩn (dùng `DBConnection.getConnection()` không trong try-with-resources)

---

### 📌 Tiêu chí 8 – Realtime Update (0.5đ → **0.4đ**)

**Tốt:**
- ✅ WebSocket-based realtime (thay vì polling) — kiến trúc đúng
- ✅ Khi bid thành công, server gửi response chứa `Bid` object về người bidder
- ✅ `StarAuctionCarouselController` đăng ký handler cho `PLACE_BID` event để cập nhật giá realtime
- ✅ `AuctionPageController` cập nhật giá và bid history realtime

**Còn thiếu:**
- ❌ **Server không broadcast tới tất cả clients** khi có bid mới — chỉ gửi response về người đặt giá. Những người khác đang xem cùng phiên đấu giá **không nhận được update**.
- ❌ Không có Publish-Subscribe mechanism trên server (không có `auctionSubscribers` map)
- ❌ Auction ENDED không được broadcast tự động

---

### 📌 Tiêu chí 9 – Kiến trúc Client–Server (0.5đ → **0.4đ**)

**Tốt:**
- ✅ Tách biệt rõ 3 module Maven: `core`, `server`, `client`
- ✅ Giao tiếp qua WebSocket với JSON protocol
- ✅ `EventType` enum chuẩn hóa tất cả request/response type
- ✅ `RequestDispatcher` trên server — routing rõ ràng
- ✅ `CorrelationId` cho request-response matching — thiết kế async đúng cách
- ✅ Server-side session management (`userSessions`)
- ✅ Authorization: anonymous vs authenticated vs admin

**Thiếu:**
- ❌ Server không hỗ trợ multi-client broadcast (đã nêu ở #8)
- ❌ Không có load balancing hay connection pool rõ ràng

---

### 📌 Tiêu chí 10 – Mô hình MVC (0.5đ → **0.4đ**)

**Tốt:**
- ✅ JavaFX + FXML tách biệt View khỏi Controller — đúng MVC
- ✅ ViewModel pattern (`AuctionPageViewModel`, `GeneralPageViewModel`, etc.) — MVVM approach
- ✅ `ObservableList`, `Property` binding — View tự cập nhật qua data binding
- ✅ Server: Controller → Service → DAO — 3 tầng rõ ràng
- ✅ 23 FXML files + tương ứng Controllers riêng biệt

**Thiếu:**
- ❌ `GeneralPageController.java` vi phạm Single Responsibility — vừa load data, vừa quản lý carousel animation logic (>300 dòng)
- ❌ `AuctionDao.java` là 534 dòng — quá lớn, nên tách
- ❌ Một số Controller truy cập trực tiếp `NetworkService` thay vì qua ViewModel

---

### 📌 Tiêu chí 11 – Maven & Coding Convention (0.5đ → **0.2đ**)

**Tốt:**
- ✅ Maven multi-module đúng cách (`core`, `server`, `client`)
- ✅ `dependencyManagement` trong parent POM — quản lý version tập trung
- ✅ Maven profiles (`-Pmock`, `-Pdev`) — linh hoạt
- ✅ Java 21 target

**Nghiêm trọng:**
- ❌ **Checkstyle hoàn toàn vắng mặt** — không có plugin, không có config file. Đây là yêu cầu tường minh của đề bài.
- ❌ Coding convention không nhất quán: một số chỗ dùng tab, chỗ dùng space; một số method dùng `camelCase` không chuẩn (e.g., `UserInformationReques.java` — typo ngay trong tên class)
- ❌ Unused commented-out code (`Item.java` line 24-27: constructor bị comment)

---

### 📌 Tiêu chí 12 – Unit Test (0.5đ → **0.2đ**)

**Có nhưng chất lượng thấp:**
- ✅ 15 test file hiện diện (unit + UI test)
- ✅ Các test `AuctionPageViewModelTest` — 3 test case có giá trị thực sự (PENDING→ACTIVE, ACTIVE→ENDED transitions)
- ✅ CI workflow chạy test

**Nghiêm trọng:**
- ❌ Nhiều test **không có assertion thực sự:**
  - `GeneralPageViewModelTest`: chỉ test `assertNotNull(viewModel.loadFeaturedAuctions())` — trivial
  - `LoginPageViewModelTest`: `assertTrue(viewModel.validateCredentials("demo", "1234"))` — test hardcode
  - `AuctionCardComponentControllerTest`: `assertNotNull(new AuctionCardComponentController())` — constructor test vô nghĩa
- ❌ **Code coverage ước tính << 60%** (yêu cầu ≥ 60%)
- ❌ Không có test cho `AuctionService`, `BidService`, `AuctionDao`, `MockDataProvider`
- ❌ Không test edge cases: bid thấp hơn giá hiện tại, bid khi auction ENDED, concurrent bids

---

### 📌 Tiêu chí 13 – CI/CD (0.5đ → **0.3đ**)

**Tốt:**
- ✅ GitHub Actions có (`ci.yml`)
- ✅ Chạy trên push và pull request
- ✅ Setup JDK 21
- ✅ Chạy `xvfb-run mvn test` (để test JavaFX headless)

**Thiếu:**
- ❌ Build step dùng `-DskipTests` → **build không chạy tests**, chỉ compile
- ❌ Không có cache `~/.m2` (chậm)
- ❌ Không có step report test results hay upload artifacts
- ❌ Workflow chỉ trigger trên 3 nhánh cố định (không cover tất cả feature branches)

```yaml
# Hiện tại:
run: mvn -B package --file pom.xml -DskipTests   # ❌ Bỏ qua test!

# Nên là:
run: mvn -B verify --file pom.xml                 # ✅ Build + Test
```

---

### 📌 Tiêu chí 14 – Anti-sniping (Bonus 0.5đ → **0.4đ**)

✅ **Đã implement đúng:**
- `Auction.applySnipeExtension()` — logic threshold 120 giây, gia hạn 120 giây
- `AuctionService.applySnipeExtension()` — gọi method và persist
- `AuctionDao.extendAuction()` — UPDATE DB
- Được gọi trong `BidService` khi xử lý bid thành công

Trừ 0.1đ vì: server không broadcast thời gian mới tới các client khác.

### 📌 Tiêu chí 15 – Auto-Bidding (Bonus 0.5đ → **0đ**)
❌ Không có tính năng này.

### 📌 Tiêu chí 16 – Bid History Visualization (Bonus 0.5đ → **0đ**)
❌ Không có `LineChart` hay biểu đồ nào cho lịch sử giá.

---

## 💪 Điểm mạnh nổi bật của dự án

1. **Kiến trúc phân tầng rất tốt:** 3-module Maven (core/server/client) với dependency đúng chiều — chuyên nghiệp hơn hầu hết dự án sinh viên
2. **WebSocket realtime thực sự:** Không phải polling, giao tiếp event-driven — tiên tiến
3. **CorrelationId mechanism:** Request-response async matching — thiết kế rất chuẩn
4. **Mock mode hoàn chỉnh:** `-Pmock` profile với `MockAuctionClient` và `MockDataProvider` — DX tốt
5. **UI phong phú:** 23 FXML screens, Star Auction Carousel, Admin panel, animations — giao diện đẹp hơn mức trung bình rất nhiều
6. **DB-level concurrency:** `FOR UPDATE` trong SQL — thực sự handle race condition ở tầng storage
7. **Hot reload tài nguyên** trong dev mode — DX tiên tiến
8. **Async image loading** với `ImageLoader` — không block UI thread

---

## ⚠️ Điểm yếu cần cải thiện ngay

### P0 – Gây mất điểm nhiều nhất

| Vấn đề | Điểm bị mất ước tính |
|:---|:---:|
| Thiếu Custom Exceptions (`InvalidBidException`, `AuctionClosedException`, etc.) | ~0.5đ |
| Thiếu Factory Method Pattern | ~0.3đ |
| Không có Checkstyle | ~0.2đ |
| Unit test chất lượng thấp, coverage < 30% | ~0.2đ |
| CI/CD build dùng `-DskipTests` | ~0.1đ |

### P1 – Nên fix để cải thiện điểm

| Vấn đề | Ghi chú |
|:---|:---|
| Item hierarchy (Electronics, Art, Vehicle) | Thêm abstract Item và các subclass |
| Server không broadcast bid updates | Thêm auction subscription map |
| Auto auction ENDED trên server | Thêm scheduled job |
| Admin/StandardUser rỗng | Thêm behavior riêng |

---

## 🚀 Đề xuất hướng phát triển tiếp theo

### Tuần tới (trước demo - ưu tiên cao)

1. **Thêm Custom Exceptions** — 1–2 giờ, impact lớn nhất:
   ```java
   // core/exception/
   public class InvalidBidException extends RuntimeException { ... }
   public class AuctionClosedException extends RuntimeException { ... }
   public class AuthenticationException extends RuntimeException { ... }
   // Sau đó replace IllegalStateException/IllegalArgumentException bằng các exception này
   ```

2. **Thêm Checkstyle vào pom.xml** — 30 phút:
   ```xml
   <plugin>
     <groupId>org.apache.maven.plugins</groupId>
     <artifactId>maven-checkstyle-plugin</artifactId>
     <version>3.3.0</version>
   </plugin>
   ```

3. **Fix CI/CD** — bỏ `-DskipTests`:
   ```yaml
   run: mvn -B verify --file pom.xml  # thay vì -DskipTests
   ```

4. **Cải thiện Unit Tests** — thêm meaningful assertions cho ít nhất:
   - `MockDataProvider` tests
   - `AuctionService` bid validation tests
   - Edge case: bid khi auction đã kết thúc

### Tuần sau (nếu còn thời gian)

5. **Server-side broadcast** khi có bid mới:
   ```java
   // Trong BidService sau khi bid thành công:
   broadcastToAuctionWatchers(auctionId, bidUpdateJson);
   ```

6. **Factory Method cho Item** — tạo `ItemFactory`:
   ```java
   public abstract class Item { ... }
   public class Electronics extends Item { ... }
   public class Art extends Item { ... }
   // ItemFactory.createItem(String type, ...) 
   ```

7. **Bid History Chart** (LineChart) — điểm thưởng 0.5đ:
   ```java
   // Thêm LineChart trong auction-page.fxml
   // Populate từ bids List với createdAt vs amount
   ```

8. **Auto-Bidding** — điểm thưởng 0.5đ thêm:
   ```java
   // Thêm MaxBidSetting entity
   // Khi bid mới vào, check và auto-counter
   ```

---

*Báo cáo dựa trên phân tích mã nguồn tại thời điểm 2026-05-21. Không có thay đổi nào được thực hiện vào mã nguồn.*
