import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class CustomizationPage {
    private BorderPane root;
    private App app;

    public CustomizationPage(App app, String drinkName) {
        this.app = app;
        root = new BorderPane();

        // Sidebar
        VBox sidebar = new VBox(20);
        sidebar.setPadding(new Insets(20));
        Label title = new Label("Customization");
        Button backBtn = new Button("Back to Orders");
        sidebar.getChildren().addAll(title, backBtn);

        backBtn.setOnAction(e -> app.showOrdersPage());

        // Drink section
        VBox centerBox = new VBox(15);
        centerBox.setAlignment(Pos.TOP_CENTER);
        centerBox.setPadding(new Insets(20));

        Label drinkTitle = new Label(drinkName);
        Rectangle placeholder = new Rectangle(150, 150, Color.LIGHTGRAY);

        HBox sizeBox = new HBox(10, new Button("Small"), new Button("Medium"), new Button("Large"));
        sizeBox.setAlignment(Pos.CENTER);

        HBox sugarBox = new HBox(10, new Button("No Sugar"), new Button("Half Sugar"), new Button("Normal"));
        sugarBox.setAlignment(Pos.CENTER);

        VBox toppingsBox = new VBox(10,
                new Button("Add Boba"),
                new Button("Add Lychee Jelly"),
                new Button("Add Pudding"));
        toppingsBox.setAlignment(Pos.CENTER);

        Button continueBtn = new Button("Continue →");

        centerBox.getChildren().addAll(drinkTitle, placeholder, sizeBox, sugarBox, toppingsBox, continueBtn);

        root.setLeft(sidebar);
        root.setCenter(centerBox);
    }

    public BorderPane getRoot() {
        return root;
    }
}
