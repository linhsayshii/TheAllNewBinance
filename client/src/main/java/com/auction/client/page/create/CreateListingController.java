package com.auction.client.page.create;

import com.auction.client.config.SceneRegistry;
import com.auction.client.scene.LifecycleAwareController;
import com.auction.client.scene.NavigationService;
import com.auction.client.service.NetworkService;
import com.auction.client.service.UserSessionService;
import com.auction.core.dto.auction.CreateAuctionRequest;
import com.auction.core.protocol.EventType;
import com.auction.core.utils.JsonMapper;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class CreateListingController implements Initializable, LifecycleAwareController {

    private static final String HANDLER_ID = "CREATE_LISTING_PAGE";

    @FXML private TextField titleInput;
    @FXML private ComboBox<String> categoryInput;
    @FXML private TextField imageUrlInput;
    @FXML private TextArea descriptionInput;

    @FXML private TextField startingPriceInput;
    @FXML private TextField bidIncrementInput;

    @FXML private DatePicker startDateInput;
    @FXML private TextField startTimeInput;
    @FXML private DatePicker endDateInput;
    @FXML private TextField endTimeInput;

    private java.io.File selectedImageFile = null;
    private String uploadedImageUrl = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        categoryInput.setItems(
                FXCollections.observableArrayList(
                        "Electronics", "Collectibles", "Art", "Fashion", "Automotive", "Other"));

        startDateInput.setValue(LocalDate.now());
        startTimeInput.setText(LocalTime.now().plusMinutes(5).withSecond(0).withNano(0).toString());

        endDateInput.setValue(LocalDate.now().plusDays(3));
        endTimeInput.setText(LocalTime.now().plusMinutes(5).withSecond(0).withNano(0).toString());

        NetworkService.getInstance()
                .getClient()
                .addResponseHandler(
                        EventType.CREATE_AUCTION, HANDLER_ID, this::handleCreateAuctionResponse);
    }

    @FXML
    private void handleBack() {
        NavigationService.getInstance().navigateTo(SceneRegistry.PROFILE_PAGE);
    }

    @FXML
    private void handleCancel() {
        handleBack();
    }

    @FXML
    private void handleImagePick() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Select Image");
        fileChooser
                .getExtensionFilters()
                .addAll(
                        new javafx.stage.FileChooser.ExtensionFilter(
                                "Image Files", "*.png", "*.jpg", "*.jpeg"));
        java.io.File file = fileChooser.showOpenDialog(titleInput.getScene().getWindow());
        if (file != null) {
            // Validation: Size < 5MB
            if (file.length() > 5 * 1024 * 1024) {
                showAlert("Validation Error", "Image size must be less than 5MB");
                return;
            }
            selectedImageFile = file;
            imageUrlInput.setText(file.getName());
            // Reset chuỗi URL đã upload
            uploadedImageUrl = null;
        }
    }

    @FXML
    private void handleSubmit() {
        if (!UserSessionService.getInstance().isAuthenticated()) {
            showAlert("Error", "You must be logged in to create a listing.");
            return;
        }

        String title = titleInput.getText();
        String category = categoryInput.getValue();
        String imageUrl = imageUrlInput.getText();
        String description = descriptionInput.getText();

        if (title == null
                || title.isBlank()
                || category == null
                || description == null
                || description.isBlank()) {
            showAlert("Validation Error", "Please fill in all required item details.");
            return;
        }

        Double startPrice;
        Double increment;
        try {
            startPrice = Double.parseDouble(startingPriceInput.getText());
            increment = Double.parseDouble(bidIncrementInput.getText());
            if (startPrice < 0 || increment <= 0) {
                showAlert("Validation Error", "Prices must be positive numbers.");
                return;
            }
        } catch (NumberFormatException e) {
            showAlert("Validation Error", "Please enter valid numeric values for prices.");
            return;
        }

        LocalDateTime startDateTime;
        LocalDateTime endDateTime;
        try {
            startDateTime =
                    LocalDateTime.of(
                            startDateInput.getValue(), LocalTime.parse(startTimeInput.getText()));
            endDateTime =
                    LocalDateTime.of(
                            endDateInput.getValue(), LocalTime.parse(endTimeInput.getText()));
        } catch (DateTimeParseException | NullPointerException e) {
            showAlert("Validation Error", "Please enter valid dates and times (HH:mm format).");
            return;
        }

        if (endDateTime.isBefore(startDateTime) || endDateTime.isEqual(startDateTime)) {
            showAlert("Validation Error", "End time must be after start time.");
            return;
        }

        Integer sellerId = UserSessionService.getInstance().getCurrentUser().getId();

        // 1. Thêm validation ảnh: phải chọn ảnh mới hoặc đã có link cũ
        if (selectedImageFile == null && (uploadedImageUrl == null || uploadedImageUrl.isBlank())) {
            if (imageUrlInput.getText() != null && !imageUrlInput.getText().isBlank()) {
                uploadedImageUrl =
                        imageUrlInput.getText(); // Chấp nhận nếu gõ URL thủ công thay vì chọn File
            } else {
                showAlert("Validation Error", "Please select an image or provide a URL.");
                return;
            }
        }

        // 2. Chuyển logic gửi Item vào hàm riêng (tránh lặp)
        if (selectedImageFile != null && uploadedImageUrl == null) {
            // Xin chữ ký từ mây
            com.auction.core.dto.item.GetUploadSignatureRequest req =
                    new com.auction.core.dto.item.GetUploadSignatureRequest("auction_items");

            // Xóa handler lỡ có trước đó
            NetworkService.getInstance()
                    .getClient()
                    .removeResponseHandler(EventType.GET_UPLOAD_SIGNATURE, HANDLER_ID);
            NetworkService.getInstance()
                    .getClient()
                    .addResponseHandler(
                            EventType.GET_UPLOAD_SIGNATURE,
                            HANDLER_ID,
                            rs -> {
                                Map<?, ?> map = null;
                                try {
                                    map = (Map<?, ?>) JsonMapper.fromJson((String) rs, Map.class);
                                } catch (Exception e) {
                                }

                                if (map != null
                                        && map.containsKey("status")
                                        && "success".equals(map.get("status"))) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> data =
                                            (Map<String, Object>) map.get("data");
                                    String signature = (String) data.get("signature");
                                    long timestamp = ((Number) data.get("timestamp")).longValue();
                                    String apiKey = (String) data.get("apiKey");

                                    // Chờ lấy được thì Post Cloudinary
                                    uploadToCloudinaryAsync(
                                            selectedImageFile,
                                            signature,
                                            timestamp,
                                            apiKey,
                                            "auction_items",
                                            (secureUrl) -> {
                                                this.uploadedImageUrl = secureUrl; // cache
                                                sendCreateAuctionRequest(
                                                        sellerId,
                                                        title,
                                                        description,
                                                        category,
                                                        secureUrl,
                                                        startPrice,
                                                        increment,
                                                        startDateTime,
                                                        endDateTime);
                                            },
                                            (err) -> {
                                                Platform.runLater(
                                                        () ->
                                                                showAlert(
                                                                        "Upload Error",
                                                                        "Could not upload image: "
                                                                                + err));
                                            });
                                } else {
                                    Platform.runLater(
                                            () ->
                                                    showAlert(
                                                            "Error",
                                                            "Could not acquire upload signature: "
                                                                    + JsonMapper.toJson(rs)));
                                }
                            });
            NetworkService.getInstance().sendRequest(EventType.GET_UPLOAD_SIGNATURE, req);
        } else {
            sendCreateAuctionRequest(
                    sellerId,
                    title,
                    description,
                    category,
                    uploadedImageUrl,
                    startPrice,
                    increment,
                    startDateTime,
                    endDateTime);
        }
    }

    private void sendCreateAuctionRequest(
            Integer sellerId,
            String title,
            String description,
            String category,
            String imageUrl,
            Double startPrice,
            Double increment,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime) {
        CreateAuctionRequest request =
                new CreateAuctionRequest(
                        sellerId,
                        title,
                        description,
                        category,
                        imageUrl,
                        startPrice,
                        increment,
                        startDateTime,
                        endDateTime);
        NetworkService.getInstance().sendRequest(EventType.CREATE_AUCTION, request);
    }

    private void uploadToCloudinaryAsync(
            java.io.File file,
            String signature,
            long timestamp,
            String apiKey,
            String folder,
            java.util.function.Consumer<String> onSuccess,
            java.util.function.Consumer<String> onError) {
        new Thread(
                        () -> {
                            try {
                                String boundary = "---boundary" + System.currentTimeMillis();
                                String cloudName = "dpclmah9p";
                                String uploadUrl =
                                        "https://api.cloudinary.com/v1_1/"
                                                + cloudName
                                                + "/image/upload";

                                java.util.List<byte[]> byteArrays = new java.util.ArrayList<>();

                                java.util.function.BiConsumer<String, String> addFormField =
                                        (name, value) -> {
                                            byteArrays.add(
                                                    ("--" + boundary + "\r\n")
                                                            .getBytes(
                                                                    java.nio.charset
                                                                            .StandardCharsets
                                                                            .UTF_8));
                                            byteArrays.add(
                                                    ("Content-Disposition: form-data; name=\""
                                                                    + name
                                                                    + "\"\r\n\r\n")
                                                            .getBytes(
                                                                    java.nio.charset
                                                                            .StandardCharsets
                                                                            .UTF_8));
                                            byteArrays.add(
                                                    (value + "\r\n")
                                                            .getBytes(
                                                                    java.nio.charset
                                                                            .StandardCharsets
                                                                            .UTF_8));
                                        };

                                addFormField.accept("api_key", apiKey);
                                addFormField.accept("timestamp", String.valueOf(timestamp));
                                addFormField.accept("signature", signature);
                                addFormField.accept("folder", folder);

                                byteArrays.add(
                                        ("--" + boundary + "\r\n")
                                                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                                byteArrays.add(
                                        ("Content-Disposition: form-data; name=\"file\";"
                                                        + " filename=\""
                                                        + file.getName()
                                                        + "\"\r\n")
                                                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                                byteArrays.add(
                                        ("Content-Type: application/octet-stream\r\n\r\n")
                                                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                                byteArrays.add(java.nio.file.Files.readAllBytes(file.toPath()));
                                byteArrays.add(
                                        ("\r\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
                                byteArrays.add(
                                        ("--" + boundary + "--\r\n")
                                                .getBytes(java.nio.charset.StandardCharsets.UTF_8));

                                int totalLength = byteArrays.stream().mapToInt(b -> b.length).sum();
                                byte[] body = new byte[totalLength];
                                int destPos = 0;
                                for (byte[] b : byteArrays) {
                                    System.arraycopy(b, 0, body, destPos, b.length);
                                    destPos += b.length;
                                }

                                java.net.http.HttpRequest httpRequest =
                                        java.net.http.HttpRequest.newBuilder()
                                                .uri(java.net.URI.create(uploadUrl))
                                                .header(
                                                        "Content-Type",
                                                        "multipart/form-data; boundary=" + boundary)
                                                .POST(
                                                        java.net.http.HttpRequest.BodyPublishers
                                                                .ofByteArray(body))
                                                .build();

                                java.net.http.HttpClient client =
                                        java.net.http.HttpClient.newHttpClient();
                                java.net.http.HttpResponse<String> response =
                                        client.send(
                                                httpRequest,
                                                java.net.http.HttpResponse.BodyHandlers.ofString());

                                if (response.statusCode() == 200 || response.statusCode() == 201) {
                                    @SuppressWarnings("unchecked")
                                    Map<String, Object> map =
                                            (Map<String, Object>)
                                                    JsonMapper.fromJson(response.body(), Map.class);
                                    String secureUrl = (String) map.get("secure_url");
                                    if (secureUrl != null) {
                                        onSuccess.accept(secureUrl);
                                    } else {
                                        onError.accept("Secure URL missing in response");
                                    }
                                } else {
                                    onError.accept(
                                            "HTTP "
                                                    + response.statusCode()
                                                    + ": "
                                                    + response.body());
                                }
                            } catch (Exception e) {
                                onError.accept(e.getMessage());
                            }
                        })
                .start();
    }

    private void handleCreateAuctionResponse(String rawJson) {
        try {
            Map<?, ?> response = JsonMapper.fromJson(rawJson, Map.class);
            Object success = response.get("success");

            Platform.runLater(
                    () -> {
                        if (Boolean.TRUE.equals(success)) {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Success");
                            alert.setHeaderText(null);
                            alert.setContentText("Your listing has been created successfully!");
                            alert.showAndWait();
                            handleBack();
                        } else {
                            String msg = (String) response.get("message");
                            showAlert("Error", "Failed to create listing: " + msg);
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }

    @Override
    public void onUnload() {
        NetworkService.getInstance()
                .getClient()
                .removeResponseHandler(EventType.CREATE_AUCTION, HANDLER_ID);
    }
}
