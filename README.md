# HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN (TheAllNewBinance)
> **Bài tập lớn nhóm 12 - Lớp học phần: 2526II_UET.CS2043_2**  
> Dự án phát triển hệ thống đấu giá trực tuyến đa người dùng dựa trên kiến trúc Java Client-Server Maven Multi-Module.

### 👥 Thành viên nhóm 12:
| STT | Họ và tên | Mã sinh viên |
| :-: | :--- | :-: |
| 1 | Hoàng Việt Linh | 25020234 |
| 2 | Trần Quốc Khánh | 25020220 |
| 3 | Nguyễn Hải Đăng | 25020116 |
| 4 | Nguyễn Công Minh | 25020266 |

---

## 📝 Giới thiệu bài toán & Phạm vi hệ thống

Hệ thống đấu giá trực tuyến (**TheAllNewBinance**) là một ứng dụng phần mềm cho phép người bán (Seller) đăng bán sản phẩm và người mua (Bidder) tham gia đấu giá cạnh tranh trực tiếp theo thời gian thực. Hệ thống được triển khai trên nền tảng mạng Internet, kết nối giữa giao diện người dùng JavaFX máy khách và máy chủ dịch vụ qua luồng truyền tin song công WebSocket.

### Phạm vi và Đặc tả kỹ thuật hệ thống:
- **Kiến trúc mạng:** Sử dụng mô hình Client-Server dựa trên sự kiện (Event-driven) thông qua giao thức WebSocket (JSON Payload).
- **Bộ máy xử lý CSDL:** Tích hợp trực tiếp hệ cơ sở dữ liệu quan hệ MySQL, sử dụng các thủ tục Trigger để kiểm soát tính toàn vẹn của trạng thái phiên đấu giá, giá khởi điểm và người bán.
- **Tải ảnh phi tập trung (Cloudinary Integration):** Để tối ưu hóa băng thông và dung lượng lưu trữ trên máy chủ, Client tương tác trực tiếp với **Cloudinary API** thông qua chữ ký xác thực an toàn (Secure Signature) do Server cấp để tải ảnh sản phẩm lên đám mây và chỉ lưu trữ URL liên kết trong CSDL.
- **Đồng bộ thời gian (Time Synchronization):** Tích hợp dịch vụ tự động đồng bộ giờ chuẩn UTC+7 (`TimeSyncService`) ngay sau khi Client thiết lập kết nối mạng với máy chủ, loại bỏ hiện tượng lệch múi giờ hoặc sai lệch thời gian cục bộ của máy khách làm ảnh hưởng đến bộ đếm ngược (countdown).
- **Thăng cấp phiên nổi bật (Star Auction):** Hỗ trợ tính năng quảng cáo nâng hạng phiên đấu giá hoạt động nổi bật, được điều hành bởi tiến trình lập lịch ngầm tự động tắt trạng thái khi hết thời hạn quảng cáo.

---

## 🛠️ Công nghệ sử dụng

Dưới đây là các công nghệ và thư viện cốt lõi được áp dụng thực tế trong mã nguồn:

| Thành phần | Công nghệ / Thư viện | Phiên bản |
|:---|:---|:---|
| **Core** | Java SE | JDK 21 |
| **Build Tool** | Apache Maven | 3.9+ |
| **Giao diện Client** | JavaFX | 21.0.4 |
| **Giao tiếp mạng** | Java-WebSocket | 1.5.3 |
| **Cơ sở dữ liệu** | MySQL | 8.4.0 |
| **JSON Parser** | Google Gson | 2.10.1 |
| **Bảo mật** | jBCrypt | 0.4 |
| **Unit Test** | JUnit 5 & Mockito | 5.10.0 / 5.11.0 |

---

## 📁 Cấu trúc thư mục dự án

```
TheAllNewBinance/
├── .github/workflows/         # Cấu hình CI/CD (Chạy unit test tự động khi push code)
├── core/                      # Module dùng chung (Shared Module) cho Client và Server
│   └── src/main/java/com/auction/core/
│       ├── auction/           # Quản lý logic đấu giá (Auction, Bid)
│       ├── dto/               # Đối tượng chuyển đổi dữ liệu qua JSON (DTOs)
│       ├── exception/         # Các ngoại lệ nghiệp vụ hệ thống (Custom Exceptions)
│       ├── products/          # Lớp sản phẩm và ItemFactory
│       ├── protocol/          # Định nghĩa giao thức gửi/nhận qua Socket (EventType)
│       ├── services/          # Các Interface dịch vụ chung (IUserService, IAuctionService...)
│       ├── users/             # Định nghĩa tài khoản và phân quyền (User, Admin)
│       ├── utils/             # Các lớp tiện ích chung (PasswordHasher, JsonMapper)
│       └── Entity.java        # Lớp thực thể cơ sở (Base Entity)
├── server/                    # Module xử lý phía Máy chủ (Socket / WebSocket Server)
│   ├── src/main/java/com/auction/server/
│   │   ├── controller/        # Tiếp nhận & điều hướng gói tin yêu cầu (UserController, AuctionController...)
│   │   ├── dao/               # Tương tác cơ sở dữ liệu MySQL (UserDao, AuctionDao, DBConnection...)
│   │   ├── services/          # Xử lý logic máy chủ (BidQueueManager, AuctionSettlementScheduler...)
│   │   ├── network/           # Hạ tầng Socket (SocketServer, BroadcastBroker, ClientConnection)
│   │   └── ServerApp.java     # Lớp khởi động server chính
│   └── src/resource/          # Chứa tệp tin SQL CSDL (schema.sql, mock_data.sql)
├── client/                    # Module giao diện người dùng (JavaFX GUI Desktop)
│   ├── src/main/java/com/auction/client/
│   │   ├── app/               # Khởi chạy ứng dụng (ClientApp, Launcher)
│   │   ├── page/              # Controller xử lý logic giao diện FXML (Login, Register, Dashboard...)
│   │   ├── service/           # Quản lý WebSocket Client và đồng bộ thời gian (NetworkService...)
│   │   ├── config/            # Cấu hình hệ thống và Đăng ký Scene (AppConfig, SceneRegistry)
│   │   └── component/         # Các component UI tái sử dụng (Header, Navbar, SearchBar...)
│   └── src/main/resources/    # Tài nguyên đồ họa của Client
│       ├── fxml/              # Giao diện màn hình FXML
│       ├── css/               # File CSS (app.css)
│       ├── fonts/             # Bộ font chữ sử dụng trong ứng dụng
│       └── mockdata/          # Dữ liệu JSON giả lập chạy thử Offline
└── pom.xml                    # Cấu hình dự án Maven tổng thể (Multi-Module Configuration)
```

---

## 🚀 Hướng dẫn chạy chương trình (Đa hệ điều hành)

> [!NOTE]
> Server hiện đang hoạt động tại địa chỉ **`binance.hnglinh.io.vn`**.
>
> Do đó, người dùng **chỉ cần khởi động Client App là có thể sử dụng các chức năng của hệ thống** (không cần cài đặt MySQL hay chạy Server local).

---

### CÁCH 1: Chạy bằng bản Release đóng gói sẵn (.jar)
👉 **[Tải các tệp tin JAR Release mới nhất tại đây](https://github.com/linhsayshii/TheAllNewBinance/releases/latest)**

Sau khi tải về tệp JAR tương ứng với hệ điều hành của bạn, mở Terminal/Command Prompt tại thư mục chứa tệp và chạy lệnh:
```bash
java -jar client-1.0-<platform>.jar
```
---

### CÁCH 2: Chạy trực tiếp từ Mã nguồn sử dụng Maven

Thực hiện các câu lệnh này từ **thư mục gốc** của dự án (`/TheAllNewBinance`).

#### 1. Build dự án:
```bash
mvn clean install -DskipTests
```

#### 2. Khởi chạy Client:
```bash
mvn javafx:run -pl client
```

---

### CÁCH 3: Tự khởi chạy Server cục bộ (Tùy chọn cho Nhà phát triển)
Nếu bạn muốn tự chạy cơ sở dữ liệu và máy chủ của riêng mình để phát triển hoặc debug cục bộ:

1. **Thiết lập CSDL MySQL:**
    - Tạo CSDL `theallnewbinance` với bảng mã `utf8mb4`.
    - Tạo người dùng `binance` với mật khẩu `PasswordCucManh!` và cấp toàn quyền cho cơ sở dữ liệu trên.
    - Thực thi lần lượt tệp `server/src/resource/schema.sql` (khởi tạo cấu trúc bảng và trigger) và `server/src/resource/mock_data.sql` (nạp dữ liệu mẫu).
2. **Khởi chạy Server cục bộ:**
   ```bash
   mvn exec:java -pl server -Dexec.mainClass="com.auction.server.ServerApp"
   ```
   *(Hoặc chạy tệp jar đóng gói sau khi package: `java -jar server/target/server-1.0.jar`)*
3. **Cấu hình Client kết nối cục bộ:**
    - Mở tệp `ClientApp.java`, đổi địa chỉ kết nối từ `"wss://binance.hnglinh.io.vn"` sang địa chỉ cục bộ `"ws://localhost:8080"`.
    - Tiến hành build lại dự án (`mvn clean install -DskipTests`) và khởi chạy Client.

---

## 🏆 Danh sách chức năng đã hoàn thành

### 1. Thiết kế Hướng đối tượng & Mô hình Kiến trúc
* [x] Đạt chuẩn thiết kế OOP chặt chẽ: Encapsulation (đóng gói), Inheritance (kế thừa), Polymorphism (đa hình), Abstraction (trừu tượng).
* [x] Áp dụng các **Design Pattern**:
    - **Singleton:** Quản lý kết nối CSDL và các dịch vụ kết nối mạng.
    - **Factory Method:** Tạo động các dòng sản phẩm đấu giá (`LuxuryCollectible`, `ArtisticCreation`, `PrecisionMechanical`).
    - **Observer:** Đồng bộ trạng thái giá thầu trực tiếp từ máy chủ đến tất cả client đang xem.
* [x] Kiến trúc phân tầng rõ ràng **Client-Server + mô hình MVC** (tách biệt Model - View - Controller).

### 2. Chức năng cốt lõi (Bắt buộc)
* [x] Đăng ký / Đăng nhập tài khoản, băm mật khẩu bảo mật tuyệt đối bằng jBCrypt.
* [x] Quản lý sản phẩm đấu giá (Thêm, sửa, xóa sản phẩm).
* [x] Tham gia đấu giá: Đặt giá thầu thời gian thực, tự động từ chối nếu giá đặt thấp hơn hoặc không thỏa mãn bước giá.
* [x] Tự động hóa kết thúc phiên: Đếm ngược thời gian và tự động đóng phiên đấu giá, tìm ra người thắng cuộc.
* [x] Xử lý lỗi & Ngoại lệ đầy đủ trên toàn hệ thống mạng (`InvalidBidException`, `AuctionClosedException`...).

### 3. Kỹ thuật nâng cao & Xử lý đồng thời (Concurrency)
* [x] **Xử lý đấu giá đồng thời an toàn:** Tích hợp bộ điều phối hàng đợi **BidQueueManager** để sắp xếp tuần tự các gói tin gửi lên từ nhiều người dùng cùng lúc. Tránh xung đột luồng ghi CSDL, tránh lỗi Lost Update và đảm bảo không bao giờ xuất hiện tình trạng Double-Winner.
* [x] Cập nhật giao diện thời gian thực qua giao thức WebSocket (không cần Refresh/Reload trang).

### 4. Chức năng nâng cao tự chọn (+1.5đ)
* [x] **Anti-sniping Algorithm (Gia hạn thời gian):** Tự động kéo dài thời gian phiên đấu giá thêm 120 giây nếu có người đặt giá thành công trong 120 giây cuối cùng.
* [x] **Bid History LineChart:** Trực quan hóa biến động giá của phiên đấu giá thông qua biểu đồ đường động, tự vẽ điểm mới mỗi khi nhận được bid hợp lệ.
* [ ] *Auto-Bidding (Đấu giá tự động) - Tính năng này chưa được phát triển.*

---

## 🔗 Liên kết tài liệu & Video Demo
* 📘 **Báo cáo chi tiết dự án (PDF):** [Xem Báo cáo PDF](./docs/project-report.pdf)
* 🎥 **Video hoạt động thực tế:** [Xem Video](https://drive.google.com/drive/folders/17thuwnIl0uE4xxBArFNs9TjdTDpm33-M?usp=drive_link)
