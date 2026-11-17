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

/**
 * LOGIN FORM. The app starts here: run this file.
 * Log In  -> MainDashboard (travelers) or AdminDashboard (admins)
 * Register tab / link -> RegisterForm
 */
public class LoginForm extends AuthScreen {

    private final RoundTextField txtUser = new RoundTextField("Enter your email");
    private final PasswordField txtPass = new PasswordField("Enter your password");
    private final JCheckBox chkRemember = checkBox("Remember me");

    public LoginForm() {
        super(false);
        int y = 0;
        y = addTitle("Welcome Back!", "Log in to your SabayLakbay account.", y);
        y = addField("Email", txtUser, 0, FORM_W, y);
        y = addField("Password", txtPass, 0, FORM_W, y);

        add(chkRemember, -4, y, 170, 22);
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

    private void forgotPassword() {
        String email = Dialogs.inputLine(this, "lock", "Forgot your password?",
                "Enter the email of your account and we'll send you a reset link.", "you@email.com", "Send link");
        if (email == null) return;
        if (!email.contains("@")) { Theme.error(this, "Please enter a valid email."); return; }
        // BACKEND: Backend.sendPasswordReset() saves to MySQL
        Theme.run(this, () -> Backend.sendPasswordReset(email.trim()));
    }

    public static void main(String[] args) {
        // smooth (anti-aliased) text everywhere, like a modern app
        System.setProperty("awt.useSystemAAFontSettings", "lcd");
        System.setProperty("swing.aatext", "true");
        sabaylakbay_1.ui.Look.install();   // app-wide styling for drop-downs, scrollbars, check boxes...
        SwingUtilities.invokeLater(() -> AppWindow.start(new LoginForm(), "SabayLakbay - Log In"));
    }
}
