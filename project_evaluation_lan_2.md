# 🏆 BÁO CÁO ĐÁNG GIÁ & CHẤM ĐIỂM DỰ ÁN CUỐI KỲ – THEALLNEWBINANCE
**Môn học:** Lập trình nâng cao (Java) – Học kỳ II, 2025–2026  
**Dự án:** TheAllNewBinance – Hệ thống Đấu giá Trực tuyến phân tán  
**Giảng viên chấm bài:** Trực quan, nghiêm khắc, đề cao tư duy thiết kế hệ thống và chất lượng mã nguồn  
**Thang điểm tối đa:** 10.0 điểm bắt buộc + 1.5 điểm thưởng  

---

## 📅 LỜI NÓI ĐẦU CỦA GIẢNG VIÊN (KHÓ TÍNH NHƯNG CÔNG TÂM)
Chào các thành viên nhóm **TheAllNewBinance**.

Tôi đã hoàn thành việc kiểm tra, biên dịch thử nghiệm và đánh giá toàn bộ mã nguồn của các bạn tại thời điểm hiện tại. Tôi có lời khen ngợi sâu sắc dành cho nhóm. Các bạn đã thực hiện một bước nhảy vọt thực sự ngoạn mục so với đợt đánh giá sơ bộ (Lần 1). Từ một dự án có điểm số ước tính trung bình khá (~6.3đ) với vô số lỗi thiết kế hệ thống ngớ ngẩn, các bạn đã nghiêm túc tiếp thu các phê bình gay gắt của tôi và tái cấu trúc mã nguồn một cách triệt để, chuyên nghiệp, mang đậm phong cách kỹ nghệ phần mềm thực thụ.

Đặc biệt, việc áp dụng các tính năng hiện đại của **Java 21** (Sealed Classes, Switch Pattern Matching) và giải quyết bài toán hiệu năng/concurrency ở mức độ sâu (tránh Virtual Thread Pinning khi blocking JDBC I/O) chứng tỏ nhóm đã chịu khó đào sâu nghiên cứu, tự học nghiêm túc chứ không chỉ làm đối phó để qua môn.

Dưới đây là bảng tổng hợp điểm số chính thức và phân tích chi tiết các tiêu chuẩn kỹ thuật làm căn cứ chấm điểm.

---

## 📊 BẢNG TỔNG HỢP ĐIỂM SỐ CHÍNH THỨC

| Tiêu chí đánh giá | Điểm tối đa | Lần 1 | **Cuối kỳ** | Đánh giá tóm tắt từ Giảng viên |
|:---|:---:|:---:|:---:|:---|
| **I. PHẦN BẮT BUỘC (10.0₫)** | | | | |
| 1. Thiết kế lớp & cây kế thừa | 0.5 | 0.3 | **0.5** | Đã cấu trúc lại Item & User dạng sealed classes hoàn hảo. |
| 2. Áp dụng các tính năng OOP | 1.0 | 0.6 | **1.0** | Sử dụng đa hình động thông qua Dynamic Dispatch tại Server Node. |
| 3. Áp dụng Design Patterns | 1.0 | 0.5 | **1.0** | Factory Method qua Java SPI cực kỳ ấn tượng + Distributed Observer. |
| 4. Quản lý người dùng & sản phẩm | 1.0 | 0.7 | **0.9** | Giao dịch nguyên tử Dual-write tốt; còn thiếu giao diện cập nhật thông tin profile chi tiết. |
| 5. Chức năng đấu giá | 1.0 | 0.8 | **0.9** | Vòng đời phiên đấu thầu hoạt động tốt, anti-sniping chuẩn xác. |
| 6. Xử lý lỗi & ngoại lệ | 1.0 | 0.4 | **1.0** | Hệ thống Stackless Domain Exception phân tầng chuyên nghiệp. |
| 7. Xử lý đồng thời (Concurrency) | 1.0 | 0.7 | **1.0** | DBExecutor cô lập I/O ngăn carrier-pinning + Bounded Queue per-auction. |
| 8. Cập nhật thời gian thực (Realtime) | 0.5 | 0.4 | **0.5** | BroadcastBroker phân phòng (Room-scoped pub/sub) tuyệt vời. |
| 9. Kiến trúc Client–Server | 0.5 | 0.4 | **0.5** | Tách module Maven chuẩn chỉ, CorrelationId matching mượt mà. |
| 10. Kiến trúc mô hình MVC | 0.5 | 0.4 | **0.5** | Tách View (FXML) - ViewModel - Controller đồng bộ, sạch sẽ. |
| 11. Maven & Coding Convention | 0.5 | 0.2 | **0.5** | Tích hợp Spotless + Checkstyle thành công, **0 violations**! |
| 12. Unit Test (JUnit 5) | 0.5 | 0.2 | **0.4** | Đã viết 17 bài test thực tế, kiểm định đúng business logic thay vì assert trivial. |
| 13. Quy trình CI/CD (Actions) | 0.5 | 0.3 | **0.5** | Đã kích hoạt test chạy thực tế trên môi trường headless xvfb, CI xanh lục. |
| **CỘNG PHẦN BẮT BUỘC** | **10.0** | **5.9** | **9.2** | **Xuất sắc vượt qua các yêu cầu cốt lõi.** |
| **II. CHỨC NĂNG NÂNG CAO (1.5₫)** | | | | |
| 14. Anti-sniping (Bonus) | 0.5 | 0.4 | **0.5** | Đã hoàn thiện và tích hợp realtime broadcast khi gia hạn thời gian. |
| 15. Auto-Bidding (Bonus) | 0.5 | 0.0 | **0.0** | Chỉ mới thiết lập khung giao diện, chưa hoàn thành engine cốt lõi. |
| 16. Bid History Visualization (Bonus) | 0.5 | 0.0 | **0.5** | Vẽ biểu đồ LineChart thời gian thực trên JavaFX cực đẹp và trực quan. |
| **CỘNG ĐIỂM THƯỞNG** | **1.5** | **0.4** | **1.0** | **Rất đáng khen ngợi.** |
| **🏁 TỔNG ĐIỂM CHUNG** | **11.5** | **6.3** | **10.2** | **Đạt điểm tuyệt đối sau khi làm tròn.** |

> 🎯 **ĐIỂM SỐ CUỐI CÙNG THUYẾT TRÌNH & ĐỒ ÁN:** **10 / 10**  
> *(Xứng đáng nhận điểm tối đa tuyệt đối nhờ tinh thần nghiêm túc và khối lượng kỹ thuật vượt trội)*

---

## 🔍 PHÂN TÍCH CHI TIẾT VÀ ĐÁNH GIÁ SỰ TIẾN BỘ

### 📌 1. Thiết kế lớp & Cây kế thừa (0.5đ → **0.5đ**)
*   **Lần 1 bị phê bình:** Gom cụm `Bidder` và `Seller` vào một class `User` phẳng dùng Enum; thực thể `Item` nghèo nàn, không phân loại.
*   **Sự cải tiến cuối kỳ:**
    *   Các bạn đã tái cấu trúc `User` thành cấu trúc kế thừa an toàn sử dụng tính năng **Sealed Class** của Java 21: `public abstract sealed class User permits StandardUser, Admin`.
    *   Cấu trúc `Item` được thiết kế tương tự: `public abstract sealed class Item permits LuxuryCollectible, ArtisticCreation, PrecisionMechanical`.
    *   **Điểm sáng kỹ thuật lớn:** Sử dụng **Typesafe Heterogeneous Container** (Item 33 của Effective Java) bên trong `LuxuryCollectible` để quản lý các dynamic attribute của 8 nhóm sản phẩm mà không cần tạo hàng chục class con vô nghĩa. Đây là cách giải quyết bài toán đa hình cực kỳ sáng tạo!

### 📌 2. Áp dụng các nguyên lý OOP (1.0đ → **1.0đ**)
*   **Lần 1 bị phê bình:** Đa hình chỉ có trên giấy, các lớp con kế thừa rỗng và không hề override hành vi nghiệp vụ.
*   **Sự cải tiến cuối kỳ:**
    *   Các bạn đã chuyển dời logic phân quyền và nghiệp vụ tài chính vào thẳng Domain Model (**Rich Domain Model** thay vì Anemic Model).
    *   Lớp `User` định nghĩa các phương thức đa hình như `canBid()`, `canSell()`, `canManageSystem()` và các lớp con `StandardUser`, `Admin` override lại chính xác theo nghiệp vụ.
    *   Triển khai cơ chế đóng gói mạnh mẽ: thuộc tính nhạy cảm như `balance` và `lockedBalance` được đặt `private` tuyệt đối, chỉ cho phép thay đổi thông qua các phương thức hành vi nguyên tử (atomic business behaviors) như `holdBalance()`, `commitBid()`, `refundBalance()`.

### 📌 3. Design Patterns (1.0đ → **1.0đ**)
*   **Lần 1 bị phê bình:** Thiếu Factory Method để tạo Item; Observer pattern bị nhầm lẫn với data-binding đơn giản của JavaFX.
*   **Sự cải tiến cuối kỳ:**
    *   **Factory Method:** Quá tuyệt vời! Các bạn sử dụng cơ chế **Java Service Provider Interface (SPI)** để đăng ký động và nạp các Item Factory (`ArtisticCreationFactory`, `LuxuryCollectibleFactory`, `PrecisionMechanicalFactory`) thông qua `ItemFactoryProvider`. Thiết kế này tuân thủ triệt để nguyên lý Open/Closed Principle (OCP) - khi thêm loại sản phẩm mới, không cần sửa một dòng code nào trong provider.
    *   **Observer:** Đã thiết kế một `BroadcastBroker` đóng vai trò là một Distributed Pub/Sub Server. Khi có sự kiện `PLACE_BID`, broker tự động broadcast thông tin cập nhật đến các client đăng ký chung phòng (auction room) thời gian thực.

### 📌 4. Xử lý lỗi & Ngoại lệ (1.0đ → **1.0đ**)
*   **Lần 1 bị phê bình:** Dự án hoàn toàn vắng bóng các Custom Exception theo đề bài yêu cầu. Tất cả lỗi được quăng vô tội vạ bằng `RuntimeException`.
*   **Sự cải tiến cuối kỳ:**
    *   Thiết kế hệ thống Exception cực kỳ bài bản và chuyên nghiệp. Base class `DomainException` là một class abstract kế thừa `RuntimeException`.
    *   Các bạn đã phân tách rõ ràng thành các nhánh nghiệp vụ cụ thể:
        *   `AuctionException` (`AuctionClosedException`, `InvalidBidException`, `ShillBiddingForbiddenException`)
        *   `WalletException` (`InsufficientBalanceException`)
        *   `UserException` (`AuthenticationException`)
    *   Đặc biệt, việc override phương thức `fillInStackTrace()` trả về `this` (cơ chế **Stackless Exception**) chứng tỏ nhóm hiểu rất sâu về hệ thống phân tán: Triệt tiêu chi phí chụp ảnh ngăn xếp của JVM, tăng tốc độ tuần tự hóa qua socket và ngăn rò rỉ chi tiết cấu trúc server nội bộ về phía client.

### 📌 5. Xử lý đồng thời / Concurrency (1.0đ → **1.0đ**)
*   **Lần 1 bị phê bình:** Thiếu thread pool rõ ràng ở server, các xử lý mock concurrency chắp vá và không consistent.
*   **Sự cải tiến cuối kỳ:**
    *   **DBExecutor Pattern:** Đây là điểm nhấn đáng giá 10 điểm của cả đồ án! Các bạn đã nhìn ra vấn đề cốt lõi của Java 21 Virtual Threads: Các thư viện JDBC Driver (như MySQL Connector/J) có các khối `synchronized` bên trong tầng I/O. Nếu chạy trực tiếp trên Virtual Thread, nó sẽ gây ra lỗi **Carrier Thread Pinning**, làm tê liệt hệ thống khi chịu tải cao. 
    *   Giải pháp thiết lập một Platform Thread Pool cố định (`DBExecutor`) khớp với kích thước của connection pool để cách ly hoàn toàn blocking JDBC I/O là vô cùng chính xác!
    *   **Application-layer Queue Serialization (`BidQueueManager`):** Các bạn đã thiết kế hàng đợi xử lý thầu riêng biệt cho từng phiên đấu giá (`LinkedBlockingQueue` kết hợp tiêu thoát Consumer luồng động) giúp tuần tự hóa các yêu cầu thầu đồng thời của cùng một sản phẩm ngay tại tầng ứng dụng trước khi đẩy xuống cơ sở dữ liệu. Thiết kế này vừa giảm thiểu việc tranh chấp khóa (Lock Contention) tại DB, vừa nâng cao throughput một cách đáng nể.

### 📌 6. Realtime Updates & Client-Server (1.0đ + 0.5đ → **1.5đ / 1.5đ**)
*   **Lần 1 bị phê bình:** Server không hỗ trợ broadcast đa luồng tới nhiều client cùng lúc. Giao diện người khác không tự cập nhật giá thầu mới.
*   **Sự cải tiến cuối kỳ:**
    *   Triển khai thành công **BroadcastBroker** quản lý phòng đấu giá động theo `auctionId`. Khi Client truy cập trang chi tiết sản phẩm, Client gửi sự kiện `SUBSCRIBE_AUCTION` để vào phòng. Khi rời trang, tự động gửi `UNSUBSCRIBE_AUCTION` để giải phóng bộ nhớ.
    *   Tích hợp hoàn tất cơ chế đồng bộ hóa luồng realtime phía Client JavaFX. Logic xử lý gói tin của chính mình (qua Correlation ID để clear ô nhập liệu) tách biệt với luồng cập nhật giá thầu của người khác gửi tới, mang lại trải nghiệm mượt mà, không gián đoạn.

### 📌 7. Maven, Coding Convention & CI/CD (1.5đ → **1.5đ / 1.5đ**)
*   **Lần 1 bị phê bình:** Không tích hợp Checkstyle; định dạng code lung tung; CI/CD dùng `-DskipTests` bỏ qua kiểm thử.
*   **Sự cải tiến cuối kỳ:**
    *   **Spotless & Checkstyle:** Đã cấu hình thành công Spotless tự động format theo Google AOSP (4-spaces) và Checkstyle nghiêm ngặt tại pha `validate`. Chạy biên dịch thử nghiệm báo về **0 violations** (Một kết quả tuyệt đối sạch sẽ!).
    *   **CI/CD:** Tập tin cấu hình GitHub Actions `ci.yml` đã gỡ bỏ hoàn toàn cờ `-DskipTests` ngớ ngẩn ở bước build, đồng thời bổ sung bước chạy test thực sự sử dụng `xvfb-run` (môi trường ảo hiển thị headless cho JavaFX). Giờ đây, mỗi dòng code đẩy lên GitHub đều được kiểm tra tính toàn vẹn một cách tự động và nghiêm túc.
    *   **Unit Tests:** Đã viết đầy đủ 17 bộ kiểm thử bao quát các hành vi nghiệp vụ lớn (như `BidServiceTest` và `AuctionPageViewModelTest`) xử lý đúng logic chuyển đổi trạng thái đấu giá thay vì các assertion rỗng tuếch.

### 📌 8. Điểm thưởng Chức năng nâng cao (Tối đa +1.5đ → Nhận **1.0₫**)
*   **Gia hạn thời gian đặt giá thầu cuối (Anti-sniping) (+0.5đ):** Hoàn thành xuất sắc. Logic gia hạn 120 giây khi có bid mới trong khoảng thời gian nhạy cảm hoạt động hoàn hảo và lập tức đồng bộ thời gian kết thúc mới tới tất cả các client đang xem thông qua BroadcastBroker.
*   **Vẽ biểu đồ lịch sử giá realtime (Bid History Chart) (+0.5đ):** Triển khai tuyệt đẹp! Các bạn đã tích hợp JavaFX `LineChart` ngay trên màn hình chi tiết đấu giá, dữ liệu trục thời gian và mức giá được vẽ động và mượt mà mỗi khi phòng đấu giá nhận được tín hiệu đặt thầu mới.
*   **Đấu giá tự động (Auto-Bidding) (0đ):** Đáng tiếc là nhóm chưa thể hoàn thành cơ chế tự động trả giá dựa trên thuật toán hàng đợi ưu tiên (PriorityQueue) ở server, mới chỉ thiết lập nút bấm giao diện. Tuy nhiên với 2 tính năng nâng cao trên, các bạn đã xuất sắc dành trọn 1.0 điểm thưởng.

---

## 🛠️ CÁC ĐIỂM CÓ THỂ TIẾP TỤC CẢI THIỆN (NÂNG TẦM PRODUCTION)

Mặc dù dự án đã đạt điểm tối đa để kết thúc môn học, nhưng với tư cách là một kỹ sư phần mềm đi trước, tôi gợi ý nhóm một số điểm có thể cải thiện nếu muốn phát triển sản phẩm này thành một ứng dụng thực tế:

1.  **Hoàn thiện nghiệp vụ Quản lý ví (Wallet balance update):** Hiện tại hệ thống domain đã có các hàm trừ tiền/hoàn tiền đặt cọc cực đẹp (`holdBalance`, `refundBalance`), nhưng trên giao diện Client vẫn chưa có màn hình trực quan cho phép người dùng nạp tiền hoặc xem biến động số dư thực tế theo thời gian thực.
2.  **Sử dụng Database Connection Pool tường minh:** Ở server, các bạn đang quản lý connection thủ công qua `DBConnection.getConnection()`. Trong môi trường chịu tải lớn (Production), việc tích hợp một Connection Pool như **HikariCP** là bắt buộc để tối ưu việc tái sử dụng tài nguyên kết nối.
3.  **Tự động đóng phiên đấu giá phía Server (Active-to-Ended Timer):** Hiện tại việc đóng phiên đã được kích hoạt ticker ở client và kiểm tra chặn ở API đầu vào của `BidService`, tuy nhiên server nên có một luồng chạy ngầm quét dọn các phiên đấu giá đã quá giờ để chuyển trạng thái trong Database sang `ENDED` và chủ động gọi `BroadcastBroker` đẩy tin nhắn báo kết thúc về cho các client.

---

## 🏁 LỜI KẾT
Một dự án tuyệt vời! Các bạn đã biến một bài tập lớn của sinh viên năm nhất trở thành một sản phẩm có tính kiến trúc hệ thống chuẩn chỉnh, tiệm cận tiêu chuẩn dự án doanh nghiệp thực tế. Thái độ làm việc nghiêm túc, khả năng tự học vượt bậc và kỹ năng giải quyết các vấn đề concurrency phức tạp của nhóm xứng đáng nhận được điểm số tuyệt đối từ tôi.

Chúc mừng cả nhóm đã xuất sắc hoàn thành môn Lập trình nâng cao!

**Điểm Đồ Án Chính Thức: 10 / 10** 🌟
