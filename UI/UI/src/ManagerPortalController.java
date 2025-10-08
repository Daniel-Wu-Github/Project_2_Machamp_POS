package sample;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;

public class ManagerPortalController {

    @FXML
    private Button employeesButton, inventoryButton, reportsButton, menuPricesButton;

    @FXML
    void initialize() {
        employeesButton.setOnAction(e -> SceneController.switchScene("mainView.fxml", e));
        inventoryButton.setOnAction(e -> SceneController.switchScene("customizationView.fxml", e));
        reportsButton.setOnAction(e -> SceneController.switchScene("mainView.fxml", e));
        menuPricesButton.setOnAction(e -> SceneController.switchScene("customizationView.fxml", e));
    }
}
