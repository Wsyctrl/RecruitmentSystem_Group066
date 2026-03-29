package com.bupt.tarecruit.controller;

import com.bupt.tarecruit.entity.Role;
import com.bupt.tarecruit.util.DialogUtil;
import com.bupt.tarecruit.util.OperationResult;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController extends BaseController {

    @FXML
    private ChoiceBox<Role> roleChoice;
    @FXML
    private TextField userIdField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private void initialize() {
        roleChoice.setItems(FXCollections.observableArrayList(Role.TA, Role.MO));
        roleChoice.setValue(Role.TA);
    }

    @FXML
    private void handleRegister() {
        Role role = roleChoice.getValue() == null ? Role.TA : roleChoice.getValue();
        OperationResult<Void> result = services.authService()
                .register(role, userIdField.getText(),
                        passwordField.getText(), confirmPasswordField.getText());
        if (result.success()) {
            DialogUtil.info(result.message(), navigator.getPrimaryStage());
            navigator.showLogin();
        } else {
            DialogUtil.error(result.message(), navigator.getPrimaryStage());
        }
    }

    @FXML
    private void handleBackToLogin() {
        navigator.showLogin();
    }
}
