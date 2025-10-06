import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class OrdersPage {
    private BorderPane root;
    private App app;

    public OrdersPage(App app) {
        this.app = app;
        root = new BorderPane();

        // Sidebar
        VBox sidebar = new VBox(20);
        sidebar.setPadding(new Insets(20));
        Label title = new Label("Machamp POS");
        Button orderBtn = new Button("Order");
        Button managerBtn = new Button("Manager");
        sidebar.getChildren().addAll(title, orderBtn, managerBtn);

        managerBtn.setOnAction(e -> app.showManagerPortal());

        // Top menu
        HBox topMenu = new HBox(10);
        topMenu.setPadding(new Insets(10));
        topMenu.setAlignment(Pos.CENTER);
        Button drinksTab = new Button("Drinks");
        Button foodTab = new Button("Food");
        Button merchTab = new Button("Merch");
        topMenu.getChildren().addAll(drinksTab, foodTab, merchTab);

        // Orders grid
        FlowPane drinksGrid = new FlowPane(20, 20);
        drinksGrid.setPadding(new Insets(20));

        String[] drinks = {"Milk Tea", "Ice Blended Latte", "Matcha", "Green Tea"};
        for (String drink : drinks) {
            VBox itemBox = createDrinkBox(drink);
            drinksGrid.getChildren().add(itemBox);
        }

        root.setLeft(sidebar);
        root.setTop(topMenu);
        root.setCenter(drinksGrid);
    }

    private VBox createDrinkBox(String name) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);

        Rectangle placeholder = new Rectangle(150, 150, Color.LIGHTGRAY);
        Label label = new Label(name);
        box.getChildren().addAll(placeholder, label);

        // On click → customization page
        box.setOnMouseClicked(e -> app.showCustomizationPage(name));

        return box;
    }

    public BorderPane getRoot() {
        return root;
    }
}
