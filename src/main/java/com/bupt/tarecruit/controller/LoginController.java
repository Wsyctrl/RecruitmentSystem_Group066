package com.bupt.tarecruit.controller;
/**
 * Controller for user login view
 * Handles login validation and page navigation
 */
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
 /**
     * Initialize page after loading
     * Bind event for register link
     */
    @FXML
    private void handleLogin() {
        String userId = userIdField.getText();
        String password = passwordField.getText();

        // 输入校验：用户名不能为空
        if (userId == null || userId.trim().isEmpty()) {
            DialogUtil.error("User ID cannot be empty.", navigator.getPrimaryStage());
            return;
        }

        // 输入校验：密码不能为空
        if (password == null || password.trim().isEmpty()) {
            DialogUtil.error("Password cannot be empty.", navigator.getPrimaryStage());
            return;
        }

        try {
            OperationResult<UserSession> result = services.authService().login(userId, password);
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