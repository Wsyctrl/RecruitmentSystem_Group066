package com.bupt.tarecruit.controller;

import com.bupt.tarecruit.util.PortalMode;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TestFX UI tests for ChangePasswordDialogController.
 */
class ChangePasswordDialogControllerUiTest extends BaseUiTest {

    @Override
    protected PortalMode getPortalMode() {
        return PortalMode.MO_PORTAL;
    }

    @Override
    protected void navigateInitialView() {
        // No app view needed for this dialog test.
    }

    private record Loaded(ChangePasswordDialogController controller, Stage stage, Parent root) {}

    private Loaded openDialog(boolean ta, String userId) {
        AtomicReference<Loaded> ref = new AtomicReference<>();
        runOnFx(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/change-password-dialog.fxml"));
                Parent root = loader.load();
                ChangePasswordDialogController c = loader.getController();
                c.setServices(services);
                Stage st = new Stage();
                st.setScene(new Scene(root));
                if (ta) c.setTaId(userId);
                else c.setMoId(userId);
                c.setDialogStage(st);
                st.show();
                ref.set(new Loaded(c, st, root));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        WaitForAsyncUtils.waitForFxEvents();
        return ref.get();
    }

    private Button findButton(Parent root, String text) {
        for (var n : root.lookupAll(".button")) {
            if (n instanceof Button b && text.equals(b.getText())) return b;
        }
        return null;
    }
    /** Verifies initial render. */
    @Test
    @DisplayName("Initial render shows three password fields and empty status label")
    void initialRender() {
        Loaded l = openDialog(true, "ta.alice@bupt.edu.cn");
        assertNotNull(l.root.lookup("#currentPasswordField"));
        assertNotNull(l.root.lookup("#newPasswordField"));
        assertNotNull(l.root.lookup("#confirmPasswordField"));
        Label status = (Label) l.root.lookup("#statusLabel");
        assertNotNull(status);
        assertEquals("", status.getText());
        runOnFx(l.stage::close);
    }
    /** Verifies blank fields show error. */
    @Test
    @DisplayName("Empty fields produce a 'Please fill in all fields' status and no change")
    void blankFieldsShowError() {
        Loaded l = openDialog(true, "ta.alice@bupt.edu.cn");
        runOnFx(() -> findButton(l.root, "Confirm").fire());
        WaitForAsyncUtils.waitForFxEvents();
        Label status = (Label) l.root.lookup("#statusLabel");
        assertEquals("Please fill in all fields", status.getText());
        assertFalse(l.controller.isPasswordChanged());
        assertTrue(l.stage.isShowing(), "Dialog stays open on validation failure");
        runOnFx(l.stage::close);
    }
    /** Verifies wrong current password. */
    @Test
    @DisplayName("Wrong current password shows error and stays open")
    void wrongCurrentPassword() {
        Loaded l = openDialog(true, "ta.alice@bupt.edu.cn");
        runOnFx(() -> {
            ((PasswordField) l.root.lookup("#currentPasswordField")).setText("WRONG");
            ((PasswordField) l.root.lookup("#newPasswordField")).setText("brandnew");
            ((PasswordField) l.root.lookup("#confirmPasswordField")).setText("brandnew");
            findButton(l.root, "Confirm").fire();
        });
        WaitForAsyncUtils.waitForFxEvents();
        Label status = (Label) l.root.lookup("#statusLabel");
        assertEquals("Current password is incorrect", status.getText());
        assertFalse(l.controller.isPasswordChanged());
        // Password unchanged in DAO.
        assertEquals("pass1234",
                services.profileService().findTa("ta.alice@bupt.edu.cn").orElseThrow().getPassword());
        runOnFx(l.stage::close);
    }
    /** Verifies mismatched new passwords. */
    @Test
    @DisplayName("Mismatched new passwords are rejected")
    void mismatchedNewPasswords() {
        Loaded l = openDialog(true, "ta.alice@bupt.edu.cn");
        runOnFx(() -> {
            ((PasswordField) l.root.lookup("#currentPasswordField")).setText("pass1234");
            ((PasswordField) l.root.lookup("#newPasswordField")).setText("aaaaaaa");
            ((PasswordField) l.root.lookup("#confirmPasswordField")).setText("bbbbbbb");
            findButton(l.root, "Confirm").fire();
        });
        WaitForAsyncUtils.waitForFxEvents();
        Label status = (Label) l.root.lookup("#statusLabel");
        assertEquals("New passwords do not match", status.getText());
        assertFalse(l.controller.isPasswordChanged());
        runOnFx(l.stage::close);
    }
    /** Verifies successful ta change. */
    @Test
    @DisplayName("Valid change updates TA password and closes the dialog")
    void successfulTaChange() {
        Loaded l = openDialog(true, "ta.alice@bupt.edu.cn");
        runOnFx(() -> {
            ((PasswordField) l.root.lookup("#currentPasswordField")).setText("pass1234");
            ((PasswordField) l.root.lookup("#newPasswordField")).setText("freshSecret");
            ((PasswordField) l.root.lookup("#confirmPasswordField")).setText("freshSecret");
            findButton(l.root, "Confirm").fire();
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(l.controller.isPasswordChanged());
        assertFalse(l.stage.isShowing(), "Dialog auto-closes on success");
        assertEquals("freshSecret",
                services.profileService().findTa("ta.alice@bupt.edu.cn").orElseThrow().getPassword());
    }
    /** Verifies successful mo change. */
    @Test
    @DisplayName("Valid change updates MO password and closes the dialog")
    void successfulMoChange() {
        Loaded l = openDialog(false, "mo.bob@bupt.edu.cn");
        runOnFx(() -> {
            ((PasswordField) l.root.lookup("#currentPasswordField")).setText("pass1234");
            ((PasswordField) l.root.lookup("#newPasswordField")).setText("newmopass");
            ((PasswordField) l.root.lookup("#confirmPasswordField")).setText("newmopass");
            findButton(l.root, "Confirm").fire();
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(l.controller.isPasswordChanged());
        assertFalse(l.stage.isShowing());
        assertEquals("newmopass",
                services.profileService().findMo("mo.bob@bupt.edu.cn").orElseThrow().getPassword());
    }
    /** Verifies cancel closes dialog. */
    @Test
    @DisplayName("Cancel button closes dialog without changing password")
    void cancelClosesDialog() {
        Loaded l = openDialog(true, "ta.alice@bupt.edu.cn");
        runOnFx(() -> findButton(l.root, "Cancel").fire());
        WaitForAsyncUtils.waitForFxEvents();
        assertFalse(l.controller.isPasswordChanged());
        assertFalse(l.stage.isShowing());
        assertEquals("pass1234",
                services.profileService().findTa("ta.alice@bupt.edu.cn").orElseThrow().getPassword());
    }
}
