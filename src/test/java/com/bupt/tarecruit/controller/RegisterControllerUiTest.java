package com.bupt.tarecruit.controller;

import com.bupt.tarecruit.entity.Role;
import com.bupt.tarecruit.util.PortalMode;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TestFX UI tests for RegisterController registration flow.
 */
class RegisterControllerUiTest extends BaseUiTest {

    /** @return TA portal mode under test */
    @Override
    protected PortalMode getPortalMode() {
        return PortalMode.TA_PORTAL;
    }

    /** Opens the registration view before each test. */
    @Override
    protected void navigateInitialView() {
        navigator.showRegister();
    }
    /** Verifies renders register form. */
    @Test
    @DisplayName("Register view shows core fields and the role hint")
    void rendersRegisterForm() {
        assertNotNull(fx("#userIdField"));
        assertNotNull(fx("#passwordField"));
        assertNotNull(fx("#confirmPasswordField"));
        Label hint = fx("#registerRoleHintLabel");
        assertNotNull(hint);
        assertEquals("Register as TA", hint.getText());
    }
    /** Verifies role choice hidden. */
    @Test
    @DisplayName("Role choice is hidden in TA portal but still set to TA")
    void roleChoiceHidden() {
        ChoiceBox<Role> roleChoice = fx("#roleChoice");
        assertNotNull(roleChoice);
        assertFalse(roleChoice.isVisible());
        assertFalse(roleChoice.isManaged());
        assertEquals(Role.TA, roleChoice.getValue());
    }
    /** Verifies mo portal changes hint. */
    @Test
    @DisplayName("MO portal renders 'Register as MO'")
    void moPortalChangesHint() {
        runOnFx(() -> {
            navigator = new com.bupt.tarecruit.util.SceneNavigator(primaryStage, services,
                    PortalMode.MO_PORTAL);
            navigator.showRegister();
        });
        WaitForAsyncUtils.waitForFxEvents();
        Label hint = fx("#registerRoleHintLabel");
        assertEquals("Register as MO", hint.getText());
        ChoiceBox<Role> roleChoice = fx("#roleChoice");
        assertEquals(Role.MO, roleChoice.getValue());
    }
    /** Verifies empty email rejected. */
    @Test
    @DisplayName("Empty email shows an error and stays on register")
    void emptyEmailRejected() {
        fillForm("", "abc", "abc");
        clickRegister();
        WaitForAsyncUtils.waitForFxEvents();
        assertNotNull(fx("#confirmPasswordField"), "Should stay on register view");
        assertTrue(services.profileService().findTa("").isEmpty());
    }
    /** Verifies empty password rejected. */
    @Test
    @DisplayName("Empty password shows an error and stays on register")
    void emptyPasswordRejected() {
        fillForm("new.ta@bupt.edu.cn", "", "");
        clickRegister();
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(services.profileService().findTa("new.ta@bupt.edu.cn").isEmpty());
    }
    /** Verifies empty confirm rejected. */
    @Test
    @DisplayName("Empty confirm password shows an error and stays on register")
    void emptyConfirmRejected() {
        fillForm("new.ta@bupt.edu.cn", "secret", "");
        clickRegister();
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(services.profileService().findTa("new.ta@bupt.edu.cn").isEmpty());
    }
    /** Verifies invalid email rejected. */
    @Test
    @DisplayName("Invalid bupt email format is rejected")
    void invalidEmailRejected() {
        fillForm("notabupt@example.com", "secret", "secret");
        clickRegister();
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(services.profileService().findTa("notabupt@example.com").isEmpty());
    }
    /** Verifies mismatched passwords rejected. */
    @Test
    @DisplayName("Mismatched passwords are rejected")
    void mismatchedPasswordsRejected() {
        fillForm("ta.zoe@bupt.edu.cn", "secret", "different");
        clickRegister();
        WaitForAsyncUtils.waitForFxEvents();
        assertTrue(services.profileService().findTa("ta.zoe@bupt.edu.cn").isEmpty());
    }
    /** Verifies existing email rejected. */
    @Test
    @DisplayName("Existing email cannot be re-registered")
    void existingEmailRejected() {
        fillForm("ta.alice@bupt.edu.cn", "another", "another");
        clickRegister();
        WaitForAsyncUtils.waitForFxEvents();
        // Password should remain pass1234 (seed).
        assertEquals("pass1234",
                services.profileService().findTa("ta.alice@bupt.edu.cn").orElseThrow().getPassword());
    }
    /** Verifies valid registration creates account. */
    @Test
    @DisplayName("Valid TA registration creates the account and navigates to login")
    void validRegistrationCreatesAccount() {
        fillForm("ta.newby@bupt.edu.cn", "newpass1", "newpass1");
        clickRegister();
        WaitForAsyncUtils.waitForFxEvents();
        // Account should be created.
        assertTrue(services.profileService().findTa("ta.newby@bupt.edu.cn").isPresent());
        // We should be back on the login screen (registerLink hyperlink only exists there).
        assertNotNull(fx("#registerLink"));
    }
    /** Verifies back to sign in navigates to login. */
    @Test
    @DisplayName("Back to sign in returns to the login view")
    void backToSignInNavigatesToLogin() {
        runOnFx(() -> {
            Button back = findButtonByText("Back to sign in");
            assertNotNull(back, "Back to sign in button should exist");
            back.fire();
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertNotNull(fx("#registerLink"));
    }
    /** Verifies back to browse navigates to guest dashboard. */
    @Test
    @DisplayName("Back-to-browse arrow returns to TA guest dashboard")
    void backToBrowseNavigatesToGuestDashboard() {
        runOnFx(() -> {
            Button back = fx("#backButton");
            assertTrue(back.isVisible(), "TA portal shows the back button on register");
            back.fire();
        });
        WaitForAsyncUtils.waitForFxEvents();
        Label welcome = fx("#welcomeLabel");
        assertNotNull(welcome);
        assertEquals("Browse jobs as guest", welcome.getText());
    }

    /**
     * Sets registration form fields on the FX thread.
     *
     * @param email   user id / email
     * @param pwd     password
     * @param confirm confirmation password
     */
    private void fillForm(String email, String pwd, String confirm) {
        runOnFx(() -> {
            ((TextField) fx("#userIdField")).setText(email);
            ((PasswordField) fx("#passwordField")).setText(pwd);
            ((PasswordField) fx("#confirmPasswordField")).setText(confirm);
        });
    }

    /** Fires the Register button on the registration form. */
    private void clickRegister() {
        runOnFx(() -> {
            Button btn = findButtonByText("Register");
            assertNotNull(btn, "Register button should exist");
            btn.fire();
        });
        try {
            Thread.sleep(120);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        WaitForAsyncUtils.waitForFxEvents();
    }

    /**
     * @param text exact button label
     * @return first matching button, or {@code null}
     */
    private Button findButtonByText(String text) {
        for (Node n : primaryStage.getScene().getRoot().lookupAll(".button")) {
            if (n instanceof Button b && text.equals(b.getText())) {
                return b;
            }
        }
        return null;
    }
}
