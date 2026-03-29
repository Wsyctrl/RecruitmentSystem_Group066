package com.bupt.tarecruit.controller;

import com.bupt.tarecruit.entity.UserSession;
import com.bupt.tarecruit.util.DialogUtil;
import com.bupt.tarecruit.util.OperationResult;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController extends BaseController {

    @FXML
    private TextField userIdField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Hyperlink registerLink;

    @FXML
    private void initialize() {
        registerLink.setOnAction(e -> navigator.showRegister());
    }

    @FXML
    private void handleLogin() {
        try {
            OperationResult<UserSession> result = services.authService().login(userIdField.getText(), passwordField.getText());
            if (result.success()) {
                navigator.showDashboard(result.data());
            } else {
                DialogUtil.error(result.message(), navigator.getPrimaryStage());
            }
        } catch (IllegalArgumentException ex) {
            DialogUtil.error(ex.getMessage(), navigator.getPrimaryStage());
        }
    }
}
