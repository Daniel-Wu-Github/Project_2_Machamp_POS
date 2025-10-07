package sample;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;

public class MainViewController {

    @FXML
    void initialize() {
        // If you have buttons in mainView.fxml to go to customization or manager, wire them here.
        // Example (you can add fx:id to buttons in FXML):
        // orderButton.setOnAction(e -> SceneController.switchScene("customizationView.fxml", e));
        // managerButton.setOnAction(e -> SceneController.switchScene("managerPortalView.fxml", e));
    }
}
