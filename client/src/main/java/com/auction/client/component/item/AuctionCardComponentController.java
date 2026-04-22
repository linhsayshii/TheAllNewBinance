package com.auction.client.component.item;

import com.auction.client.config.SceneRegistry;
import com.auction.client.dto.ProductCardUiModel;
import com.auction.client.scene.NavigationService;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

public class AuctionCardComponentController {

	private static final long MAX_CLICK_DURATION_MILLIS = 250;

	@FXML
	private Label imageLabel;

	@FXML
	private Label timeLeftLabel;

	@FXML
	private Label titleLabel;

	@FXML
	private Label priceLabel;

	@FXML
	private Label biddersLabel;

	@FXML
	private Region progressFill;

	private long pressStartedAtNanos;

	@FXML
	private void handleMousePressed(MouseEvent event) {
		if (event != null && event.getButton() == MouseButton.PRIMARY) {
			pressStartedAtNanos = System.nanoTime();
		}
	}

	@FXML
	private void handleOpenProductDetail(MouseEvent event) {
		if (!isPrimaryClickOnly(event)) {
			return;
		}

		NavigationService.getInstance().navigateTo(SceneRegistry.PRODUCT_DETAIL_PAGE);
	}

	private boolean isPrimaryClickOnly(MouseEvent event) {
		long pressDurationMillis = (System.nanoTime() - pressStartedAtNanos) / 1_000_000L;
		return event != null
			&& event.getButton() == MouseButton.PRIMARY
			&& event.isStillSincePress()
			&& pressDurationMillis <= MAX_CLICK_DURATION_MILLIS;
	}

	public void setData(ProductCardUiModel model) {
		if (model == null) {
			return;
		}

		imageLabel.setText("Item Image");
		timeLeftLabel.setText("End in " + model.timeLeft());
		titleLabel.setText(model.title());
		priceLabel.setText(model.currentBid());
		biddersLabel.setText("People bidding");

		double clampedProgress = 0.65;
		progressFill.setPrefWidth(190 * clampedProgress);
	}
}