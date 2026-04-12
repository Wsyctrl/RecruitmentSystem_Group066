package com.bupt.tarecruit.controller;

import com.bupt.tarecruit.entity.Role;
import com.bupt.tarecruit.util.DialogUtil;
import com.bupt.tarecruit.util.OperationResult;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * Controller for the registration view.
 * Handles user registration input, role selection, and navigation back to the login page.
 */
public class RegisterController extends BaseController {

    /**
     * Choice box used to select the user role during registration.
     */
    @FXML
    private ChoiceBox<Role> roleChoice;

    /**
     * Input field for the user ID.
     */
    @FXML
    private TextField userIdField;

    /**
     * Input field for the password.
     */
    @FXML
    private PasswordField passwordField;

    /**
     * Input field for confirming the password.
     */
    @FXML
    private PasswordField confirmPasswordField;

    /**
     * Initializes the registration view after the FXML components are loaded.
     * Sets the available roles and the default selected role.
     */
    @FXML
    private void initialize() {
        roleChoice.setItems(FXCollections.observableArrayList(Role.TA, Role.MO));
        roleChoice.setValue(Role.TA);
    }

    /**
     * Handles the registration action triggered from the UI.
     * Delegates validation and registration to {@link com.bupt.tarecruit.service.AuthService}.
     */
    @FXML
    private void handleRegister() {
        Role role = roleChoice.getValue() == null ? Role.TA : roleChoice.getValue();
        String userId = userIdField.getText();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        OperationResult<Void> result = services.authService()
                .register(role, userId, password, confirmPassword);

        if (result.success()) {
            DialogUtil.info(result.message(), navigator.getPrimaryStage());
            navigator.showLogin();
        } else {
            DialogUtil.error(result.message(), navigator.getPrimaryStage());
        }
    }

    /**
     * Navigates the user back to the login page.
     */
    @FXML
    private void handleBackToLogin() {
        navigator.showLogin();
    }
}