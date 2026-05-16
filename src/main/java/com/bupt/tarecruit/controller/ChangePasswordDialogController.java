package com.bupt.tarecruit.controller;

import com.bupt.tarecruit.service.ServiceRegistry;
import com.bupt.tarecruit.util.DialogUtil;
import com.bupt.tarecruit.util.OperationResult;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;

/**
 * Controller for the change password dialog.
 * Allows MO and TA users to change their password in a separate dialog.
 */
public class ChangePasswordDialogController {

    private ServiceRegistry services;
    private Stage dialogStage;
    private String moId;
    private String taId;
    private boolean isTaUser = false;
    private boolean passwordChanged = false;

    @FXML
    private PasswordField currentPasswordField;
    @FXML
    private PasswordField newPasswordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Label statusLabel;

    public void setServices(ServiceRegistry services) {
        this.services = services;
    }

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public void setMoId(String moId) {
        this.moId = moId;
        this.isTaUser = false;
    }

    public void setTaId(String taId) {
        this.taId = taId;
        this.isTaUser = true;
    }

    public boolean isPasswordChanged() {
        return passwordChanged;
    }

    @FXML
    private void initialize() {
        statusLabel.setText("");
    }

    @FXML
    private void handleChangePassword() {
        String current = currentPasswordField.getText() == null ? "" : currentPasswordField.getText();
        String newPass = newPasswordField.getText() == null ? "" : newPasswordField.getText();
        String confirm = confirmPasswordField.getText() == null ? "" : confirmPasswordField.getText();

        if (current.isBlank() || newPass.isBlank() || confirm.isBlank()) {
            statusLabel.setText("Please fill in all fields");
            statusLabel.setStyle("-fx-text-fill: #e53e3e;");
            return;
        }

        OperationResult<Void> result;
        if (isTaUser) {
            result = services.profileService().changeTaPassword(taId, current, newPass, confirm);
        } else {
            result = services.profileService().changeMoPassword(moId, current, newPass, confirm);
        }

        if (result.success()) {
            statusLabel.setText(result.message());
            statusLabel.setStyle("-fx-text-fill: #38a169;");
            passwordChanged = true;
            // Clear fields
            currentPasswordField.clear();
            newPasswordField.clear();
            confirmPasswordField.clear();
        } else {
            statusLabel.setText(result.message());
            statusLabel.setStyle("-fx-text-fill: #e53e3e;");
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    @FXML
    private void handleClose() {
        dialogStage.close();
    }
}
