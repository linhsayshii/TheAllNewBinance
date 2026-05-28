package com.auction.client.ui;

import org.junit.jupiter.api.BeforeAll;
import org.testfx.framework.junit5.ApplicationTest;
import javafx.stage.Stage;

/**
 * Lớp cơ sở cho tất cả UI Integration Tests sử dụng TestFX.
 *
 * <p>Chức năng cốt lõi:
 *
 * <ol>
 *   <li>Kích hoạt chế độ Headless (không cần màn hình) khi chạy trên CI/CD qua Monocle 21.0.2
 *   <li>Khởi tạo JavaFX Application Thread tự động thông qua ApplicationTest
 *   <li>Cung cấp hook {@code start(Stage)} cho lớp con override để nạp Scene cụ thể
 * </ol>
 *
 * <p><b>Cách sử dụng:</b>
 *
 * <pre>{@code
 * class MyPageTest extends BaseUiTest {
 *     @Override
 *     public void start(Stage stage) throws Exception {
 *         FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MyPage.fxml"));
 *         Parent root = loader.load();
 *         stage.setScene(new Scene(root));
 *         stage.show();
 *     }
 *
 *     @Test
 *     void myTest() {
 *         clickOn("#myButton");
 *         verifyThat("#myLabel", hasText("Expected"));
 *     }
 * }
 * }</pre>
 *
 * <p><b>Lưu ý kỹ thuật về Monocle Headless:</b>
 *
 * <ul>
 *   <li>Phiên bản Monocle {@code jdk-12.0.1+2} KHÔNG tương thích với Java 21, sẽ ném {@code
 *       IncompatibleClassChangeError}.
 *   <li>Phiên bản {@code 21.0.2} được sử dụng trong dự án này tương thích 100% với JavaFX 21.0.4.
 *   <li>Các System Properties được đặt trước khi TestFX/JavaFX khởi tạo Platform (trong {@code
 *       @BeforeAll} static).
 * </ul>
 */
public abstract class BaseUiTest extends ApplicationTest {

    /**
     * Kích hoạt chế độ Headless khi System Property "test.headless" là true (mặc định: true).
     *
     * <p>Cấu hình Maven Surefire Plugin trong pom.xml đặt các JVM arguments tương đương, nhưng
     * phương thức này đảm bảo chúng được áp dụng đúng thứ tự ngay cả khi chạy test trực tiếp từ
     * IDE.
     */
    @BeforeAll
    public static void setupHeadlessMode() {
        if (Boolean.parseBoolean(System.getProperty("test.headless", "true"))) {
            System.setProperty("glass.platform", "Monocle");
            System.setProperty("monocle.platform", "Headless");
            // prism.order=sw: Bắt buộc dùng software renderer, tránh lỗi thiếu GPU trên CI/CD
            System.setProperty("prism.order", "sw");
        }
    }

    /**
     * Lớp con override phương thức này để nạp FXML và gắn vào Live Stage.
     *
     * <p><b>Quan trọng:</b> Bắt buộc phải gắn root vào Stage và gọi {@code stage.show()} để Scene
     * Graph trở thành "sống" (Live). Nếu không, các Node sẽ là "mồ côi" (Detached), khiến các
     * thao tác đồ họa như {@code node.getScene().getWindow()} ném ra {@code NullPointerException}.
     */
    @Override
    public void start(Stage stage) throws Exception {
        // Lớp con override để tải Scene cụ thể cần test
    }
}
