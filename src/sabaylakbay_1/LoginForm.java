package sabaylakbay_1;

import sabaylakbay_1.backend.Backend;
import sabaylakbay_1.model.User;
import sabaylakbay_1.model.UserRole;
import sabaylakbay_1.ui.AppWindow;
import sabaylakbay_1.ui.AuthScreen;
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

    private final RoundTextField txtUser = new RoundTextField("Enter your email or username");
    private final PasswordField txtPass = new PasswordField("Enter your password");
    private final JCheckBox chkRemember = checkBox("Remember me");

    public LoginForm() {
        super(false);
        int y = 0;
        y = addTitle("Welcome Back!", "Log in to your SabayLakbay account.", y);
        y = addField("Email or Username", txtUser, 0, FORM_W, y);
        y = addField("Password", txtPass, 0, FORM_W, y);

        add(chkRemember, -4, y, 150, 22);
        JLabel forgot = Theme.link("Forgot password?", this::forgotPassword);
        forgot.setHorizontalAlignment(SwingConstants.RIGHT);
        add(forgot, 170, y, 150, 22);
        y += 36;

        RoundButton btnLogin = new RoundButton("Log In");
        btnLogin.addActionListener(e -> login());
        add(btnLogin, 0, y, FORM_W, 42);
        setDefaultButton(btnLogin);   // Enter key = Log In
        y += 58;

        add(footerLink("Don't have an account? ", "Register", this::goToRegister), 0, y, FORM_W, 20);
        finish(y + 20);
    }

    /** Pre-fills the username (used after registering). */
    public LoginForm(String username) {
        this();
        txtUser.setText(username);
    }

    private void login() {
        if (txtUser.getText().isBlank() || txtPass.getValue().isEmpty()) {
            Theme.error(this, "Please enter your email/username and password.");
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
        else AppWindow.show(new MainDashboard(), "SabayLakbay");
    }

    private void forgotPassword() {
        String email = JOptionPane.showInputDialog(this,
                "Enter the email of your account.\nWe'll send you a link to reset your password.",
                "Forgot password", JOptionPane.PLAIN_MESSAGE);
        if (email == null) return;
        if (!email.contains("@")) { Theme.error(this, "Please enter a valid email."); return; }
        // BACKEND: Backend.sendPasswordReset() (button only for now)
        Theme.run(this, () -> Backend.sendPasswordReset(email.trim()));
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }
        SwingUtilities.invokeLater(() -> AppWindow.start(new LoginForm(), "SabayLakbay - Log In"));
    }
}
