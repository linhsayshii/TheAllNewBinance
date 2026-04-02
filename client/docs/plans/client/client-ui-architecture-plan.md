## Plan: Client UI Architecture
Xay kien truc client theo huong page + shared-component + service + theme tokens de dung general page giong mock hien tai nhung du nen tang mo rong 3-5 page, ho tro dark/light theme, va bo sung hot reload (dev mode) cho CSS/FXML. Cach lam: chuan hoa thu muc truoc, sau do tach FXML/CSS thanh module nho, roi refactor controller/scene flow theo rui ro thap.

**Plan file placement (recommended)**
- Official location in repo: docs/plans/client/client-ui-architecture-plan.md
- Reason: dễ version bằng git, dễ discover từ README, tách biệt tài liệu kỹ thuật với mã nguồn runtime.
- Add an index note in README pointing to docs/plans for team onboarding.

**Project Structure Overview (high-level)**
- Layer huong page: `page/*` la layer man hinh, `component/*` la thanh phan tai su dung.
- Layer orchestration: `app/ClientApp`, `scene/*`, `config/*` quan ly lifecycle, routing, runtime flags.
- Layer service: `service/*` gom query/session/theme/resource + hot reload (`HotReloadService`, `CssDependencyIndex`).
- Layer resource: `fxml/pages`, `fxml/components`, `css/base|components|pages|themes`, `i18n`, `images`.
- Layer test: `unit/*` cho logic nhe, `ui/*` cho smoke/UI behavior.
- Nguon chi tiet chinh xac cua cau truc thu muc nam o muc **ASCII Tree (5 core pages)** ben duoi.

**Page Plan Index**
- General page implementation plan: `pages/general-page/implementation-plan.md`
- General page acceptance criteria: `pages/general-page/acceptance-criteria.md`
- General page task checklist: `pages/general-page/task-checklist.md`
- Reusable template for next pages: `templates/page-plan-template.md`

**Docs Workflow (Architecture vs Page-Level)**
- Keep cross-page decisions and shared conventions in this architecture file.
- Keep page-specific implementation detail in `pages/<page-name>/*`.
- When implementing a new page, copy `templates/page-plan-template.md`, then split into 3 files (implementation, acceptance, checklist).
- If a page decision affects another page or shared layer, mirror the impact back into this architecture plan.


**ASCII Tree (5 core pages)**
client/
├── src/
│   ├── main/
│   │   ├── java/com/auction/client/
│   │   │   ├── app/
│   │   │   │   └── ClientApp.java
│   │   │   ├── config/
│   │   │   │   ├── AppConfig.java
│   │   │   │   └── SceneRegistry.java
│   │   │   ├── scene/
│   │   │   │   ├── SceneService.java
│   │   │   │   └── NavigationService.java
│   │   │   ├── page/
│   │   │   │   ├── general/
│   │   │   │   │   ├── GeneralPageController.java
│   │   │   │   │   └── GeneralPageViewModel.java
│   │   │   │   ├── login/
│   │   │   │   │   ├── LoginPageController.java
│   │   │   │   │   └── LoginPageViewModel.java
│   │   │   │   ├── register/
│   │   │   │   │   ├── RegisterPageController.java
│   │   │   │   │   └── RegisterPageViewModel.java
│   │   │   │   ├── productdetail/
│   │   │   │   │   ├── ProductDetailPageController.java
│   │   │   │   │   └── ProductDetailPageViewModel.java
│   │   │   │   └── profile/
│   │   │   │       ├── ProfilePageController.java
│   │   │   │       └── ProfilePageViewModel.java
│   │   │   ├── component/
│   │   │   │   ├── shell/
│   │   │   │   │   ├── HeaderComponentController.java
│   │   │   │   │   └── NavbarComponentController.java
│   │   │   │   ├── item/
│   │   │   │   │   └── AuctionCardComponentController.java
│   │   │   │   └── control/
│   │   │   │       ├── SearchBarComponentController.java
│   │   │   │       └── ThemeToggleComponentController.java
│   │   │   ├── service/
│   │   │   │   ├── AuctionQueryService.java
│   │   │   │   ├── UserSessionService.java
│   │   │   │   ├── ThemeService.java
│   │   │   │   ├── ResourceLoader.java
│   │   │   │   ├── HotReloadService.java
│   │   │   │   └── CssDependencyIndex.java
│   │   │   ├── mapper/
│   │   │   │   └── ProductCardMapper.java
│   │   │   ├── dto/
│   │   │   │   └── ProductCardUiModel.java
│   │   │   └── exception/
│   │   │       └── SceneLoadException.java
│   │   └── resources/
│   │       ├── fxml/
│   │       │   ├── pages/
│   │       │   │   ├── general-page.fxml
│   │       │   │   ├── login-page.fxml
│   │       │   │   ├── register-page.fxml
│   │       │   │   ├── product-detail-page.fxml
│   │       │   │   └── profile-page.fxml
│   │       │   └── components/
│   │       │       ├── shell/
│   │       │       │   ├── header.fxml
│   │       │       │   └── navbar.fxml
│   │       │       ├── item/
│   │       │       │   └── auction-card.fxml
│   │       │       └── control/
│   │       │           ├── search-bar.fxml
│   │       │           └── theme-toggle.fxml
│   │       ├── css/
│   │       │   ├── base/
│   │       │   │   ├── tokens.css
│   │       │   │   ├── typography.css
│   │       │   │   ├── spacing.css
│   │       │   │   └── reset.css
│   │       │   ├── components/
│   │       │   │   ├── shell/
│   │       │   │   │   ├── header.css
│   │       │   │   │   └── navbar.css
│   │       │   │   ├── item/
│   │       │   │   │   └── auction-card.css
│   │       │   │   └── control/
│   │       │   │       ├── search-bar.css
│   │       │   │       └── theme-toggle.css
│   │       │   ├── pages/
│   │       │   │   ├── general-page.css
│   │       │   │   ├── login-page.css
│   │       │   │   ├── register-page.css
│   │       │   │   ├── product-detail-page.css
│   │       │   │   └── profile-page.css
│   │       │   ├── themes/
│   │       │   │   ├── light.css
│   │       │   │   └── dark.css
│   │       │   └── app.css
│   │       ├── images/
│   │       │   ├── icons/
│   │       │   ├── placeholders/
│   │       │   └── branding/
│   │       └── i18n/
│   │           ├── messages_vi.properties
│   │           └── messages_en.properties
│   └── test/
│       ├── java/com/auction/client/
│       │   ├── unit/
│       │   │   ├── page/
│       │   │   │   ├── general/GeneralPageViewModelTest.java
│       │   │   │   ├── login/LoginPageViewModelTest.java
│       │   │   │   ├── register/RegisterPageViewModelTest.java
│       │   │   │   ├── productdetail/ProductDetailPageViewModelTest.java
│       │   │   │   └── profile/ProfilePageViewModelTest.java
│       │   │   ├── component/
│       │   │   │   ├── header/HeaderComponentControllerTest.java
│       │   │   │   └── auctioncard/AuctionCardComponentControllerTest.java
│       │   │   └── scene/SceneServiceTest.java
│       │   └── ui/
│       │       ├── GeneralPageUiTest.java
│       │       ├── LoginPageUiTest.java
│       │       ├── RegisterPageUiTest.java
│       │       ├── ProductDetailPageUiTest.java
│       │       └── ProfilePageUiTest.java
│       └── resources/
│           ├── fxml/
│           └── fixtures/

**Checklist plan (execution-ready)**
1. [v] Create full folder skeleton for java/resources/test exactly as tree above.
2. [v] Move current general page code into page/general package and rename controller/viewmodel pattern.
3. [v] Split current monolithic FXML into page shell + reusable component FXML files.
4. [v] Split current CSS into base/components/pages/themes and wire via app.css.
5. [v] Implement SceneRegistry + SceneService + NavigationService for 5-page routing.
6. [v] Add placeholder FXML and controllers for login/register/product-detail/profile pages.
7. [v] Add shared components (header, navbar, search bar, auction card, theme toggle) with dedicated controllers.
8. [~] Introduce ThemeService and verify light/dark switching works across all 5 pages.
9. [v] Add unit tests for page viewmodels and core component controllers.
10. [ ] Add TestFX UI smoke tests for 5 pages and main navigation paths.
11. [~] Run compile + tests and fix resource-path issues.
12. [~] Write architecture notes and contributor conventions to keep structure consistent.

Ghi chu trang thai:
- [v]: Hoan thanh.
- [~]: Da lam mot phan, can chot them.
- [ ]: Chua lam.


**Folder Diagram (overview only)**
- Pages in scope: general, login, register, product-detail, profile.
- Java layer summary: `app|config|scene|page|component|service|mapper|dto|exception`.
- Resource layer summary: `fxml/pages`, `fxml/components`, `css/base|components|pages|themes`, `images`, `i18n`.
- Test layer summary: `unit/page`, `unit/component`, `unit/scene`, `ui`, `test-resources`.
- Chi tiet day du va cap nhat nhat van duoc chuan hoa trong muc **ASCII Tree (5 core pages)**.


1. Phase 1 - Baseline and Naming (foundation, low risk)
1. Chốt chuẩn đặt tên: package theo feature, FXML/CSS theo kebab-case, controller theo vai trò (Page vs Component).
2. Chuẩn hóa điểm vào app: giữ ClientApp gọn, chuyển trách nhiệm điều phối scene sang service chuyên dụng.
3. Tạo skeleton thư mục java/resources/test trước khi di chuyển logic để tránh đổi nhiều lần.

2. Phase 2 - Target Structure Setup (*blocks all later phases*)
1. Tạo cấu trúc Java: app, config, scene, page, component, service, viewmodel, mapper, exception.
2. Tạo cấu trúc resources: fxml/pages, fxml/components/shell, fxml/components/item, fxml/components/control, css/base, css/components, css/pages, css/themes, images/icons, images/placeholders, i18n.
3. Tạo cấu trúc test: unit + ui tách riêng, thêm test-resources cho FXML mock và fixture data.

3. Phase 3 - General Page Decomposition (*depends on phase 2*)
1. Tách general page thành page shell + component includes: header, top menu, auction section, product card.
2. Chuyển class điều khiển hiện tại thành page controller mới, thêm component controllers cho phần tái sử dụng.
3. Đưa dữ liệu hiển thị card vào view model + DTO hiển thị để tránh hardcode text trong FXML.
4. Đảm bảo general page render giống ảnh mock ở desktop trước, sau đó tinh chỉnh responsive theo width breakpoints JavaFX.

4. Phase 4 - CSS Architecture with Theme Support (*parallel with phase 3 after folders exist*)
1. Tách style thành 4 lớp: base tokens, component styles, page styles, themes.
2. Định nghĩa token màu/chữ/khoảng cách cho light + dark ngay từ đầu để đổi theme không sửa component CSS.
3. Giữ một file stylesheet tổng chỉ làm nhiệm vụ import và thứ tự ưu tiên.
4. Ràng buộc selector theo namespace component/page để tránh xung đột khi có thêm màn khác.

5. Phase 5 - Scene and Navigation Refactor (*depends on phase 3*)
1. Thay scene manager monolithic bằng scene service + scene registry kiểu enum/id để tránh string path rải rác.
2. Tách resource loading và hot-reload dev thành service riêng để production path không bị nhiễu.
3. Chuẩn bị navigation contract cho 3-5 pages (general/login/detail/profile/cart), dù chưa implement hết UI.

6. Phase 6 - Testing Strategy (Unit + UI TestFX) (*depends on phases 3,5*)
1. Unit tests cho page viewmodel và controller logic không phụ thuộc scene graph nặng.
2. UI tests với TestFX cho các hành vi quan trọng: load page, click menu/header actions, render card list, theme toggle.
3. Thêm smoke test khởi động scene chính để bắt lỗi đường dẫn FXML/CSS sớm trong CI.

7. Phase 7 - Hardening and Documentation (*final phase*)
1. Viết tài liệu architecture ngắn: quy ước thêm page/component mới, nơi đặt CSS, cách đăng ký scene.
2. Bổ sung checklist review để tránh quay lại kiểu file monolithic (FXML/CSS/controller).
3. Dọn code cũ sau khi toàn bộ reference đã chuyển, tránh xóa sớm gây gãy app.

**Relevant files**
- /mnt/windows-data/learn/2nd Term - 2526/lap trinh nang cao/TheAllNewBinance/client/src/main/java/com/auction/client/app/ClientApp.java - entrypoint + lifecycle hot reload
- /mnt/windows-data/learn/2nd Term - 2526/lap trinh nang cao/TheAllNewBinance/client/src/main/java/com/auction/client/scene/SceneService.java - scene switching + stylesheet reload hooks
- /mnt/windows-data/learn/2nd Term - 2526/lap trinh nang cao/TheAllNewBinance/client/src/main/java/com/auction/client/scene/NavigationService.java - navigation wrapper theo SceneRegistry
- /mnt/windows-data/learn/2nd Term - 2526/lap trinh nang cao/TheAllNewBinance/client/src/main/java/com/auction/client/service/HotReloadService.java - watcher cho CSS/FXML va dispatch tren JavaFX thread
- /mnt/windows-data/learn/2nd Term - 2526/lap trinh nang cao/TheAllNewBinance/client/src/main/java/com/auction/client/service/CssDependencyIndex.java - parse de quy @import tu app.css
- /mnt/windows-data/learn/2nd Term - 2526/lap trinh nang cao/TheAllNewBinance/client/src/main/java/com/auction/client/service/ResourceLoader.java - source/classpath resource resolution cho dev/prod
- /mnt/windows-data/learn/2nd Term - 2526/lap trinh nang cao/TheAllNewBinance/client/src/main/java/com/auction/client/config/AppConfig.java - dev/hotReload flags + source resource root detection
- /mnt/windows-data/learn/2nd Term - 2526/lap trinh nang cao/TheAllNewBinance/client/src/main/resources/fxml/pages/general-page.fxml - page shell hien tai
- /mnt/windows-data/learn/2nd Term - 2526/lap trinh nang cao/TheAllNewBinance/client/src/main/resources/css/app.css - CSS entrypoint va import graph

**Verification**
1. Build client module thành công: compile + test không lỗi sau mỗi phase.
2. Snapshot thủ công general page trước/sau refactor: bố cục, spacing, typography khớp mock.
3. Kiểm tra scene load và chuyển scene qua registry không còn hardcoded path phân tán.
4. Chạy Unit tests và TestFX UI smoke trong pipeline local.
5. Bật/tắt theme light-dark và xác nhận component không vỡ màu/chữ.

**Decisions**
- In scope: Kiến trúc cho 3-5 pages, component dùng chung toàn app, chuẩn bị theme ngay, test Unit + UI TestFX.
- Out of scope hiện tại: implement đầy đủ toàn bộ 3-5 page business logic; chỉ dựng nền kiến trúc và general page làm chuẩn.
- Rủi ro chính: tách FXML/CSS quá nhanh có thể lệch UI; giảm rủi ro bằng phase hóa và visual verify mỗi bước.

**Further Considerations**
1. Chọn mô hình state: ViewModel thuần JavaFX Property (khuyên dùng) hoặc service callback đơn giản (ít học hơn nhưng khó scale).
2. Bo sung test cho hot reload (debounce, CSS reload decision, FXML reload policy) vi tinh nang da duoc implement trong dev profile.
3. Khi thêm page mới, ưu tiên template page/component chuẩn để onboarding nhanh cho thành viên mới.

**Conflict Audit (Plan vs Codebase hien tai)**
1. Conflict duong dan cu: cac file `client/src/main/java/com/auction/client/ClientApp.java`, `client/src/main/java/com/auction/client/util/SceneManager.java`, `client/src/main/java/com/auction/client/controller/GeneralController.java`, `client/src/main/resources/fxml/general.fxml`, `client/src/main/resources/css/style.css` da khong con ton tai.
2. Conflict checklist: trong plan cu, muc 5-9 dang o trang thai chua lam nhung codebase da co implementation cho routing/page placeholders/components/unit tests.
3. Conflict hot reload: hot reload khong con la "can can nhac" nua, ma da implement bang `HotReloadService` + `CssDependencyIndex` + `ResourceLoader` dev mode.
4. Conflict testing expectation: muc TestFX chua phu hop trang thai hien tai vi module client chua khai bao dependency TestFX va test UI hien tai la smoke JUnit co ban.

---

**Current Progress Note (2026-03-29)**
- Da cap nhat lai plan theo cau truc du an client hien tai sau khi them hot reload.
- Da cap nhat danh sach Relevant files va loai bo duong dan cu khong ton tai.
- Da danh dau lai checklist theo trang thai thuc te (hoan thanh/partial/chua lam).
- Da bo sung muc Conflict Audit de chi ro cac diem lech giua plan cu va codebase hien tai.