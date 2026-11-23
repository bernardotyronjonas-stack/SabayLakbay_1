package sabaylakbay_1;

import sabaylakbay_1.backend.Backend;
import sabaylakbay_1.model.User;
import sabaylakbay_1.model.UserRole;
import sabaylakbay_1.ui.AppWindow;
import sabaylakbay_1.ui.AuthScreen;
import sabaylakbay_1.ui.Dialogs;
import sabaylakbay_1.ui.Theme;
import sabaylakbay_1.ui.components.PasswordField;
import sabaylakbay_1.ui.components.RoundButton;
import sabaylakbay_1.ui.components.RoundTextField;

import javax.swing.*;
import java.awt.*;

/**
 * LOGIN FORM. The app starts here: run this file.
 * Log In  -> MainDashboard (travelers) or AdminDashboard (admins)
 * Register tab / link -> RegisterForm
 */
public class LoginForm extends AuthScreen {

    private final RoundTextField txtUser = new RoundTextField("Enter your email");
    private final PasswordField txtPass = new PasswordField("Enter your password");

    public LoginForm() {
        super(false);
        int y = 0;
        y = addTitle("Welcome Back!", "Log in to your SabayLakbay account.", y);
        y = addField("Email", txtUser, 0, FORM_W, y);
        y = addField("Password", txtPass, 0, FORM_W, y);

        JLabel forgot = Theme.link("Forgot password?", this::forgotPassword);
        forgot.setHorizontalAlignment(SwingConstants.RIGHT);
        add(forgot, FORM_W - 150, y, 150, 22);
        y += 36;

        RoundButton btnLogin = new RoundButton("Log In");
        btnLogin.addActionListener(e -> login());
        add(btnLogin, 0, y, FORM_W, 46);
        setDefaultButton(btnLogin);   // Enter key = Log In
        y += 64;

        add(footerLink("Don't have an account? ", "Register", this::goToRegister), 0, y, FORM_W, 20);
        finish(y + 20);
    }

    /** Pre-fills the email (used after registering). */
    public LoginForm(String email) {
        this();
        txtUser.setText(email);
    }

    private void login() {
        if (txtUser.getText().isBlank() || txtPass.getValue().isEmpty()) {
            Theme.error(this, "Please enter your email and password.");
            return;
        }
        User user;
        try {
            user = Backend.login(txtUser.getText().trim(), txtPass.getValue());   // BACKEND
        } catch (RuntimeException ex) {
            Theme.error(this, ex.getMessage());
            return;
        }
        if (user.getRole() == UserRole.ADMIN) AppWindow.show(new AdminDashboard(), "SabayLakbay - Admin");
        else if (Backend.getInterests(user.getId()).isEmpty())
            AppWindow.show(new OnboardingForm(), "SabayLakbay - Get started");   // older account: ask once what they like
        else AppWindow.show(new MainDashboard(), "SabayLakbay");
    }

    /**
     * FORGOT PASSWORD: email + a recent password (current or one used before) + a new password.
     * Backend: Backend.resetPassword()
     */
    private void forgotPassword() {
        RoundTextField email = new RoundTextField("you@email.com");
        email.setText(txtUser.getText().trim());
        PasswordField recent = new PasswordField("A password you used on this account");
        PasswordField pass = new PasswordField("At least 8 characters");
        PasswordField again = new PasswordField("Type it again");
        JLabel err = Theme.label(" ", Font.PLAIN, 12, Theme.RED);

        JPanel body = Theme.vbox();
        body.add(formField("Email", email));
        body.add(formField("Recent password", recent));
        body.add(formField("New password", pass));
        body.add(formField("Confirm new password", again));
        body.add(Theme.left(Theme.label("Use at least 8 characters with a number and an uppercase letter.", Font.PLAIN, 12, Theme.TEXT_GRAY)));
        body.add(Theme.gap(6));
        body.add(Theme.left(err));
        body.setPreferredSize(new Dimension(420, body.getPreferredSize().height));

        boolean changed = Dialogs.form(this, "lock", "Forgot your password?",
                "Enter a recent password, then choose a new one.", body, "Change password", () -> {
                    if (!email.getText().contains("@")) { err.setText("Enter the email of your account."); return false; }
                    if (recent.getValue().isEmpty()) { err.setText("Enter a password you used recently."); return false; }
                    if (!pass.getValue().equals(again.getValue())) { err.setText("New passwords don't match."); return false; }
                    try {
                        Backend.resetPassword(email.getText().trim(), recent.getValue(), pass.getValue());   // BACKEND
                        return true;
                    } catch (RuntimeException ex) { err.setText(ex.getMessage()); return false; }
                });
        if (changed) {                       // ready to log in with the new password
            txtUser.setText(email.getText().trim());
            txtPass.setText("");
            txtPass.requestFocusInWindow();
        }
    }

    /** Label above an input, for the Forgot password popups. */
    private static JComponent formField(String label, JComponent input) {
        JPanel f = Theme.clear(new BorderLayout(0, 5));
        f.add(Theme.label(label, Font.BOLD, 12, Theme.TEXT_DARK), BorderLayout.NORTH);
        f.add(input, BorderLayout.CENTER);
        f.setBorder(Theme.pad(0, 0, 12, 0));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, f.getPreferredSize().height));
        return f;
    }

    public static void main(String[] args) {
        // smooth (anti-aliased) text everywhere, like a modern app
        System.setProperty("awt.useSystemAAFontSettings", "lcd");
        System.setProperty("swing.aatext", "true");
        sabaylakbay_1.ui.Look.install();   // app-wide styling for drop-downs, scrollbars, check boxes...
        SwingUtilities.invokeLater(() -> AppWindow.start(new LoginForm(), "SabayLakbay - Log In"));
    }
}
