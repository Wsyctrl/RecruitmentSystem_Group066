package com.bupt.tarecruit.controller;

import com.bupt.tarecruit.util.PortalMode;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Window;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TestFX UI tests for LoginController login and navigation.
 */
class LoginControllerUiTest extends BaseUiTest {

    @Override
    protected PortalMode getPortalMode() {
        return PortalMode.TA_PORTAL;
    }

    @Override
    protected void seedData() {
        super.seedData();
        services.authService().register(com.bupt.tarecruit.entity.Role.TA,
                "ta.disabled@bupt.edu.cn", "pass1234", "pass1234");
        services.profileService().findTa("ta.disabled@bupt.edu.cn").ifPresent(d -> {
            d.setFullName("Disabled");
            d.setDisabled(true);
            services.profileService().updateTa(d);
        });

        services.authService().register(com.bupt.tarecruit.entity.Role.MO,
                "mo.cathy@bupt.edu.cn", "mopass", "mopass");
    }
    /** Verifies renders login form. */
    @Test
    @DisplayName("Login view renders core fields and register link")
    void rendersLoginForm() {
        assertNotNull(fx("#userIdField"), "userIdField should be present");
        assertNotNull(fx("#passwordField"), "passwordField should be present");
        Hyperlink registerLink = fx("#registerLink");
        assertNotNull(registerLink, "registerLink should be present");
        assertEquals("No account? Register", registerLink.getText());
    }
    /** Verifies ta portal shows back button. */
    @Test
    @DisplayName("TA portal shows the back-to-browse button on login")
    void taPortalShowsBackButton() {
        Button backButton = fx("#backButton");
        assertNotNull(backButton);
        assertTrue(backButton.isVisible());
        assertTrue(backButton.isManaged());
    }
    /** Verifies notice banner shows on configure. */
    @Test
    @DisplayName("Notice banner appears when configureForPortal supplies a notice")
    void noticeBannerShowsOnConfigure() {
        runOnFx(() -> navigator.showLoginWithNotice("Please sign in first."));
        WaitForAsyncUtils.waitForFxEvents();
        HBox noticeBox = fx("#noticeBox");
        Label noticeLabel = fx("#noticeLabel");
        assertNotNull(noticeBox);
        assertNotNull(noticeLabel);
        assertTrue(noticeBox.isVisible());
        assertEquals("Please sign in first.", noticeLabel.getText());
    }
    /** Verifies empty email shows error. */
    @Test
    @DisplayName("Empty email triggers validation alert")
    void emptyEmailShowsError() {
        runOnFx(() -> {
            TextField uid = fx("#userIdField");
            PasswordField pwd = fx("#passwordField");
            uid.setText("");
            pwd.setText("anything");
        });
        clickLogin();
        WaitForAsyncUtils.waitForFxEvents();
        // Still on login (no dashboard).
        assertNotNull(fx("#userIdField"), "Should remain on login after validation failure");
    }
    /** Verifies empty password shows error. */
    @Test
    @DisplayName("Empty password triggers validation alert")
    void emptyPasswordShowsError() {
        runOnFx(() -> {
            TextField uid = fx("#userIdField");
            PasswordField pwd = fx("#passwordField");
            uid.setText("ta.alice@bupt.edu.cn");
            pwd.setText("");
        });
        clickLogin();
        WaitForAsyncUtils.waitForFxEvents();
        assertNotNull(fx("#userIdField"));
    }
    /** Verifies invalid email keeps login. */
    @Test
    @DisplayName("Invalid email format keeps user on login")
    void invalidEmailKeepsLogin() {
        runOnFx(() -> {
            TextField uid = fx("#userIdField");
            PasswordField pwd = fx("#passwordField");
            uid.setText("notanemail");
            pwd.setText("password");
        });
        clickLogin();
        WaitForAsyncUtils.waitForFxEvents();
        assertNotNull(fx("#userIdField"));
    }
    /** Verifies wrong password shows error. */
    @Test
    @DisplayName("Wrong password keeps user on login view")
    void wrongPasswordShowsError() {
        runOnFx(() -> {
            TextField uid = fx("#userIdField");
            PasswordField pwd = fx("#passwordField");
            uid.setText("ta.alice@bupt.edu.cn");
            pwd.setText("wrong");
        });
        clickLogin();
        WaitForAsyncUtils.waitForFxEvents();
        assertNotNull(fx("#userIdField"), "Should remain on login after wrong password");
    }
    /** Verifies disabled account is rejected. */
    @Test
    @DisplayName("Disabled account shows error and does not log in")
    void disabledAccountIsRejected() {
        runOnFx(() -> {
            TextField uid = fx("#userIdField");
            PasswordField pwd = fx("#passwordField");
            uid.setText("ta.disabled@bupt.edu.cn");
            pwd.setText("pass1234");
        });
        clickLogin();
        WaitForAsyncUtils.waitForFxEvents();
        assertNotNull(fx("#userIdField"), "Disabled accounts cannot sign in");
    }
    /** Verifies mo account blocked on ta portal. */
    @Test
    @DisplayName("MO/Admin accounts cannot sign in from TA portal")
    void moAccountBlockedOnTaPortal() {
        runOnFx(() -> {
            TextField uid = fx("#userIdField");
            PasswordField pwd = fx("#passwordField");
            uid.setText("mo.bob@bupt.edu.cn");
            pwd.setText("pass1234");
        });
        clickLogin();
        WaitForAsyncUtils.waitForFxEvents();
        assertNotNull(fx("#userIdField"));
    }
    /** Verifies valid ta login navigates to dashboard. */
    @Test
    @DisplayName("Valid TA login navigates to dashboard")
    void validTaLoginNavigatesToDashboard() {
        runOnFx(() -> {
            TextField uid = fx("#userIdField");
            PasswordField pwd = fx("#passwordField");
            uid.setText("ta.alice@bupt.edu.cn");
            pwd.setText("pass1234");
        });
        clickLogin();
        WaitForAsyncUtils.waitForFxEvents();
        Label welcome = fx("#welcomeLabel");
        assertNotNull(welcome, "TA dashboard should render after successful login");
        assertTrue(welcome.getText().contains("Alice"));
    }
    /** Verifies register link navigates. */
    @Test
    @DisplayName("Register hyperlink navigates to registration view")
    void registerLinkNavigates() {
        runOnFx(() -> {
            Hyperlink link = fx("#registerLink");
            link.fire();
        });
        WaitForAsyncUtils.waitForFxEvents();
        assertNotNull(fx("#confirmPasswordField"),
                "Register view should be loaded (confirmPasswordField appears only there)");
    }
    /** Verifies back to browse loads guest dashboard. */
    @Test
    @DisplayName("Back-to-browse button navigates to TA guest dashboard")
    void backToBrowseLoadsGuestDashboard() {
        runOnFx(() -> navigator.showLoginWithNotice("Please sign in first."));
        WaitForAsyncUtils.waitForFxEvents();
        runOnFx(() -> {
            // Configure back-button visible (already visible because TA portal)
            Button back = fx("#backButton");
            assertTrue(back.isVisible());
            back.fire();
        });
        WaitForAsyncUtils.waitForFxEvents();
        // Now guest dashboard has #welcomeLabel == "Browse jobs as guest"
        Label welcome = fx("#welcomeLabel");
        assertNotNull(welcome);
        assertEquals("Browse jobs as guest", welcome.getText());
    }
    /** Verifies mo portal has no back button. */
    @Test
    @DisplayName("MO portal hides back button entirely")
    void moPortalHasNoBackButton() throws Exception {
        runOnFx(() -> {
            navigator = new com.bupt.tarecruit.util.SceneNavigator(primaryStage, services,
                    PortalMode.MO_PORTAL);
            navigator.showLogin();
        });
        WaitForAsyncUtils.waitForFxEvents();
        Button back = fx("#backButton");
        assertNotNull(back);
        assertFalse(back.isVisible(), "MO portal hides the back button on the login view");
    }

    private void clickLogin() {
        // Fire the Sign in button by lookup via text, since FXML didn't give it an fx:id.
        runOnFx(() -> {
            for (javafx.scene.Node n : primaryStage.getScene().getRoot().lookupAll(".button")) {
                if (n instanceof Button b && "Sign in".equals(b.getText())) {
                    b.fire();
                    return;
                }
            }
        });
        WaitForAsyncUtils.waitForFxEvents();
        // Also wait briefly so any alert listener has a chance to react.
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        WaitForAsyncUtils.waitForFxEvents();
        // Silence unused-import warning when no Window references needed.
        @SuppressWarnings("unused")
        Class<?> ignore = Window.class;
    }
}
