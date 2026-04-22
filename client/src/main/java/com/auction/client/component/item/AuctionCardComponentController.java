package com.auction.client.component.item;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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

	private static final List<AuctionCardViewData> FAKE_DATA = List.of(
		new AuctionCardViewData("Item Image", "Vintage Rolex Submariner", "$12,600", "End in 02h 31m", "36 people bidding", 0.62),
		new AuctionCardViewData("Item Image", "Hermes Birkin 30 Togo", "$8,900", "End in 11h 20m", "18 people bidding", 0.45),
		new AuctionCardViewData("Item Image", "1968 Fender Telecaster", "$15,300", "End in 01d 03h", "52 people bidding", 0.84)
	);

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

	private boolean hasExternalData;
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

	@FXML
	private void initialize() {
		if (!hasExternalData) {
			int randomIndex = ThreadLocalRandom.current().nextInt(FAKE_DATA.size());
			applyData(FAKE_DATA.get(randomIndex));
		}
	}

	public void setData(ProductCardUiModel model) {
		if (model == null) {
			return;
		}

		hasExternalData = true;
		applyData(new AuctionCardViewData(
			"Item Image",
			model.title(),
			model.currentBid(),
			"End in " + model.timeLeft(),
			"36 people bidding",
			0.65
		));
	}

	public void useFakeData(int index) {
		int boundedIndex = Math.floorMod(index, FAKE_DATA.size());
		hasExternalData = true;
		applyData(FAKE_DATA.get(boundedIndex));
	}

	private void applyData(AuctionCardViewData data) {
		imageLabel.setText(data.imageText());
		timeLeftLabel.setText(data.timeLeft());
		titleLabel.setText(data.title());
		priceLabel.setText(data.price());
		biddersLabel.setText(data.bidders());

		double clampedProgress = Math.max(0.0, Math.min(1.0, data.progressPercent()));
		progressFill.setPrefWidth(190 * clampedProgress);
	}

	private record AuctionCardViewData(
		String imageText,
		String title,
		String price,
		String timeLeft,
		String bidders,
		double progressPercent
	) {
	}
}