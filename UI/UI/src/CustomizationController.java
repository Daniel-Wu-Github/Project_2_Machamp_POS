package sample;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.event.ActionEvent;

public class CustomizationController {

    @FXML
    private Button backBtn, continueBtn;

    @FXML
    void initialize() {
        backBtn.setOnAction(e -> SceneController.switchScene("mainView.fxml", e));
        continueBtn.setOnAction(e -> SceneController.switchScene("managerPortalView.fxml", e));
    }
}
