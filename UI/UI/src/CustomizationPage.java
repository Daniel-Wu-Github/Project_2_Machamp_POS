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

            // Size buttons (track selection)
            Button smallBtn = new Button("Small");
            Button medBtn = new Button("Medium");
            Button largeBtn = new Button("Large");
            HBox sizeBox = new HBox(10, smallBtn, medBtn, largeBtn);
            sizeBox.setAlignment(Pos.CENTER);

            // Sugar buttons
            Button noSugarBtn = new Button("No Sugar");
            Button halfSugarBtn = new Button("Half Sugar");
            Button normalSugarBtn = new Button("Normal");
            HBox sugarBox = new HBox(10, noSugarBtn, halfSugarBtn, normalSugarBtn);
            sugarBox.setAlignment(Pos.CENTER);

            // Toppings (toggleable)
            Button bobaBtn = new Button("Add Boba");
            Button lycheeBtn = new Button("Add Lychee Jelly");
            Button puddingBtn = new Button("Add Pudding");
            VBox toppingsBox = new VBox(10, bobaBtn, lycheeBtn, puddingBtn);
            toppingsBox.setAlignment(Pos.CENTER);

            Button continueBtn = new Button("Continue →");

            // State variables
            final String[] selectedSize = {"Medium"};
            final String[] selectedSugar = {"Normal"};
            final java.util.Set<String> selectedToppings = new java.util.HashSet<>();

            // Button actions update state and visual style
            smallBtn.setOnAction(e -> { selectedSize[0] = "Small"; });
            medBtn.setOnAction(e -> { selectedSize[0] = "Medium"; });
            largeBtn.setOnAction(e -> { selectedSize[0] = "Large"; });

            noSugarBtn.setOnAction(e -> { selectedSugar[0] = "No Sugar"; });
            halfSugarBtn.setOnAction(e -> { selectedSugar[0] = "Half Sugar"; });
            normalSugarBtn.setOnAction(e -> { selectedSugar[0] = "Normal"; });

            bobaBtn.setOnAction(e -> toggleTopping("Boba", bobaBtn, selectedToppings));
            lycheeBtn.setOnAction(e -> toggleTopping("Lychee Jelly", lycheeBtn, selectedToppings));
            puddingBtn.setOnAction(e -> toggleTopping("Pudding", puddingBtn, selectedToppings));

        centerBox.getChildren().addAll(drinkTitle, placeholder, sizeBox, sugarBox, toppingsBox, continueBtn);

            // Continue -> submit simple order to DB or fallback
            continueBtn.setOnAction(e -> {
                String toppings = String.join(", ", selectedToppings);
                // simple pricing model: base 3.50, size add: small -0.5, medium 0, large +0.75, toppings +0.5 each
                double total = 3.50;
                switch (selectedSize[0]) {
                    case "Small": total -= 0.50; break;
                    case "Large": total += 0.75; break;
                    default: break;
                }
                total += selectedToppings.size() * 0.50;

                boolean ok = DB.insertOrder(drinkName, selectedSize[0], selectedSugar[0], toppings, total);
                if (ok) {
                    System.out.println("Order submitted: " + drinkName + " - $" + String.format("%.2f", total));
                } else {
                    System.out.println("Failed to submit order to DB. Order totals to $" + String.format("%.2f", total));
                }
                // Return to orders page after submit
                app.showOrdersPage();
            });

        root.setLeft(sidebar);
        root.setCenter(centerBox);
    }

    public BorderPane getRoot() {
        return root;
    }

    private void toggleTopping(String name, Button btn, java.util.Set<String> selected) {
        if (selected.contains(name)) {
            selected.remove(name);
            btn.setStyle("");
        } else {
            selected.add(name);
            btn.setStyle("-fx-background-color: lightgreen;");
        }
    }
}
