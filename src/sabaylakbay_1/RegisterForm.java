package sabaylakbay_1;

import sabaylakbay_1.backend.Backend;
import sabaylakbay_1.model.User;
import sabaylakbay_1.ui.AppWindow;
import sabaylakbay_1.ui.AuthScreen;
import sabaylakbay_1.ui.Theme;
import sabaylakbay_1.ui.components.PasswordField;
import sabaylakbay_1.ui.components.RoundButton;
import sabaylakbay_1.ui.components.RoundTextField;

import javax.swing.*;

/**
 * REGISTER FORM. Create Account -> back to LoginForm.
 */
public class RegisterForm extends AuthScreen {

    private final RoundTextField txtName = new RoundTextField("Juan Dela Cruz");
    private final RoundTextField txtUsername = new RoundTextField("juandc");
    private final RoundTextField txtPhone = new RoundTextField("09XX XXX XXXX");
    private final RoundTextField txtEmail = new RoundTextField("you@email.com");
    private final PasswordField txtPass = new PasswordField("Min. 8 characters");
    private final PasswordField txtConfirm = new PasswordField("Type it again");
    private final JCheckBox chkAgree = checkBox("I agree to the Terms and Privacy Policy");

    public RegisterForm() {
        super(true);
        int y = 0;
        y = addTitle("Create an Account", "Join SabayLakbay and start traveling together.", y);
        y = addField("Full name", txtName, 0, FORM_W, y);
        addField("Username", txtUsername, 0, 152, y);
        y = addField("Phone number", txtPhone, 168, 152, y);
        y = addField("Email", txtEmail, 0, FORM_W, y);
        addField("Password", txtPass, 0, 152, y);
        y = addField("Confirm password", txtConfirm, 168, 152, y);

        add(chkAgree, -4, y - 2, FORM_W, 22);
        y += 32;

        RoundButton btnCreate = new RoundButton("Create Account");
        btnCreate.addActionListener(e -> register());
        add(btnCreate, 0, y, FORM_W, 42);
        setDefaultButton(btnCreate);   // Enter key = Create Account
        y += 58;

        add(footerLink("Already have an account? ", "Log In", this::goToLogin), 0, y, FORM_W, 20);
        finish(y + 20);
    }

    private void register() {
        String error = null;
        if (txtName.getText().isBlank() || txtUsername.getText().isBlank()
                || txtEmail.getText().isBlank() || txtPhone.getText().isBlank()) error = "Please fill in all fields.";
        else if (!txtEmail.getText().contains("@") || !txtEmail.getText().contains(".")) error = "Please enter a valid email.";
        else if (txtPass.getValue().length() < 8) error = "Password must be at least 8 characters.";
        else if (!txtPass.getValue().equals(txtConfirm.getValue())) error = "Passwords don't match.";
        else if (!chkAgree.isSelected()) error = "Please agree to the Terms and Privacy Policy.";
        if (error != null) { Theme.error(this, error); return; }

        User u = new User();
        u.setFullName(txtName.getText().trim());
        u.setUsername(txtUsername.getText().trim());
        u.setEmail(txtEmail.getText().trim());
        u.setPhone(txtPhone.getText().trim());
        if (Theme.run(this, () -> Backend.register(u, txtPass.getValue()))) {   // BACKEND
            AppWindow.show(new LoginForm(u.getUsername()), "SabayLakbay - Log In");
        }
    }
}
