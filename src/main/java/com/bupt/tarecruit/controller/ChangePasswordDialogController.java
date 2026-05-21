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

    /** Current password for verification. */
    @FXML
    private PasswordField currentPasswordField;

    /** New password entry. */
    @FXML
    private PasswordField newPasswordField;

    /** Confirmation of the new password. */
    @FXML
    private PasswordField confirmPasswordField;

    /** Inline validation or service error message. */
    @FXML
    private Label statusLabel;

    /**
     * @param services application service registry
     */
    public void setServices(ServiceRegistry services) {
        this.services = services;
    }

    /**
     * @param stage modal stage used to close the dialog
     */
    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    /**
     * Configures the dialog for an MO password change.
     *
     * @param moId MO identifier
     */
    public void setMoId(String moId) {
        this.moId = moId;
        this.isTaUser = false;
    }

    /**
     * Configures the dialog for a TA password change.
     *
     * @param taId TA identifier
     */
    public void setTaId(String taId) {
        this.taId = taId;
        this.isTaUser = true;
    }

    /**
     * @return true if the user successfully changed the password in this dialog session
     */
    public boolean isPasswordChanged() {
        return passwordChanged;
    }

    /** Clears the status label when the dialog opens. */
    @FXML
    private void initialize() {
        statusLabel.setText("");
    }

    /**
     * Validates fields and calls {@link com.bupt.tarecruit.service.ProfileService} for TA or MO;
     * closes the dialog on success so the parent can show confirmation.
     */
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
            passwordChanged = true;
            // Auto-close; the calling controller surfaces the success dialog so the user
            // does not see a still-open form behind it.
            if (dialogStage != null) {
                dialogStage.close();
            }
        } else {
            statusLabel.setText(result.message());
            statusLabel.setStyle("-fx-text-fill: #e53e3e;");
        }
    }

    /** Closes the dialog without saving. */
    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    /** Closes the dialog (same as cancel). */
    @FXML
    private void handleClose() {
        dialogStage.close();
    }
}
