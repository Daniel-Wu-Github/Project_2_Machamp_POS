import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

public class ManagerPortalPage {
    private BorderPane root;
    private App app;

    public ManagerPortalPage(App app) {
        this.app = app;
        root = new BorderPane();

        // Sidebar
        VBox sidebar = new VBox(20);
        sidebar.setPadding(new Insets(20));
        Label title = new Label("Manager Portal");
        Button backBtn = new Button("Back to Orders");
        sidebar.getChildren().addAll(title, backBtn);

        backBtn.setOnAction(e -> app.showOrdersPage());

        // Top cards
        HBox topCards = new HBox(20);
        topCards.setPadding(new Insets(10));
        topCards.setAlignment(Pos.CENTER);
        Label earnings = new Label("Daily Earnings: $1298.53");
        Label cost = new Label("Operating Cost: $465.43");
        Label popular = new Label("Popular: Ice Blended Latte");
        topCards.getChildren().addAll(earnings, cost, popular);

        // Chart
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("8:00", 12000));
        series.getData().add(new XYChart.Data<>("12:00", 30000));
        series.getData().add(new XYChart.Data<>("16:00", 50000));
        series.getData().add(new XYChart.Data<>("20:00", 90000));
        lineChart.getData().add(series);
        lineChart.setPrefHeight(300);

        VBox centerBox = new VBox(20, topCards, lineChart);
        centerBox.setPadding(new Insets(20));

        root.setLeft(sidebar);
        root.setCenter(centerBox);
    }

    public BorderPane getRoot() {
        return root;
    }
}
