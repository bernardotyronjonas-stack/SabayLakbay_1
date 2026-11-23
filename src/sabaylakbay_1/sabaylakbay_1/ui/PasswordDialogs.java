package sabaylakbay_1.ui;

import sabaylakbay_1.backend.Backend;
import sabaylakbay_1.ui.components.PasswordField;
import sabaylakbay_1.ui.components.RoundTextField;

import javax.swing.*;
import java.awt.*;

/**
 * The password popups, used by Log In, Register and Profile > Account security:
 *   forgotPassword()      email -> security question + answer + new password
 *   changePassword()      Settings: current password + new password
 *   securityQuestion()    pick a question and answer (first time, or change it with your password)
 * Backend: getSecurityQuestion, resetWithSecurityAnswer, changeOwnPassword, setSecurityQuestion
 */
public final class PasswordDialogs {

    private static final String RULES = "Use at least 8 characters with a number and an uppercase letter.";

    private PasswordDialogs() { }

    /** FORGOT PASSWORD. Returns the email when the password was changed (to fill in the Log In form), else null. */
    public static String forgotPassword(Component parent, String email) {
        // ----- step 1: which account? -----
        RoundTextField mail = new RoundTextField("you@email.com");
        mail.setText(email == null ? "" : email.trim());
        JLabel err1 = error();
        JPanel b1 = body(field("Email", mail), err1);
        String[] question = {null};
        boolean found = Dialogs.form(parent, "lock", "Forgot your password?",
                "Enter your email. We'll ask the security question you picked when you signed up.", b1, "Continue", () -> {
                    if (!mail.getText().contains("@")) { err1.setText("Enter the email of your account."); return false; }
                    try { question[0] = Backend.getSecurityQuestion(mail.getText().trim()); return true; }   // BACKEND
                    catch (RuntimeException ex) { err1.setText(html(ex.getMessage())); return false; }
                });
        if (!found) return null;

        // ----- step 2: answer + new password -----
        JLabel q = Theme.label(question[0], Font.BOLD, 14, Theme.TEXT_DARK);
        RoundTextField answer = new RoundTextField("Your answer");
        PasswordField pass = new PasswordField("At least 8 characters");
        PasswordField again = new PasswordField("Type it again");
        JLabel err2 = error();
        JPanel qBox = Theme.vbox();
        qBox.add(Theme.left(Theme.label("Security question", Font.BOLD, 12, Theme.TEXT_GRAY)));
        qBox.add(Theme.gap(4));
        qBox.add(Theme.left(q));
        qBox.setBorder(Theme.pad(0, 0, 14, 0));
        JPanel b2 = body(Theme.left(qBox), field("Your answer", answer), field("New password", pass),
                field("Confirm new password", again), hint(RULES), err2);
        boolean changed = Dialogs.form(parent, "lock", "Answer your security question",
                "Capital letters don't matter.", b2, "Change password", () -> {
                    if (answer.getText().isBlank()) { err2.setText("Type your answer."); return false; }
                    if (!pass.getValue().equals(again.getValue())) { err2.setText("New passwords don't match."); return false; }
                    try {
                        Backend.resetWithSecurityAnswer(mail.getText().trim(), answer.getText(), pass.getValue());   // BACKEND
                        return true;
                    } catch (RuntimeException ex) { err2.setText(html(ex.getMessage())); return false; }
                });
        return changed ? mail.getText().trim() : null;
    }

    /** SETTINGS: change password with the current one. */
    public static boolean changePassword(Component parent) {
        PasswordField current = new PasswordField("Your current password");
        PasswordField pass = new PasswordField("At least 8 characters");
        PasswordField again = new PasswordField("Type it again");
        JLabel err = error();
        JPanel b = body(field("Current password", current), field("New password", pass),
                field("Confirm new password", again), hint(RULES + " Your last 2 passwords can't be used again."), err);
        return Dialogs.form(parent, "lock", "Change password", "You'll use the new password the next time you log in.",
                b, "Change password", () -> {
                    if (current.getValue().isEmpty()) { err.setText("Enter your current password."); return false; }
                    if (!pass.getValue().equals(again.getValue())) { err.setText("New passwords don't match."); return false; }
                    try { Backend.changeOwnPassword(current.getValue(), pass.getValue()); return true; }   // BACKEND
                    catch (RuntimeException ex) { err.setText(html(ex.getMessage())); return false; }
                });
    }

    /**
     * Pick a security question and answer. When the account already has one (changing it in Settings),
     * the current password is asked too. Returns true when saved.
     */
    public static boolean securityQuestion(Component parent) {
        String now = Backend.mySecurityQuestion();     // BACKEND
        boolean changing = now != null;
        JComboBox<String> q = questionBox();
        if (changing) q.setSelectedItem(now);
        RoundTextField answer = new RoundTextField("Your answer");
        PasswordField current = new PasswordField("Your current password");
        JLabel err = error();
        JPanel b = changing
                ? body(field("Question", q), field("Answer", answer), field("Current password", current),
                       hint("Pick something only you know and won't forget. Capital letters don't matter."), err)
                : body(field("Question", q), field("Answer", answer),
                       hint("Pick something only you know and won't forget. Capital letters don't matter."), err);
        return Dialogs.form(parent, "shield", changing ? "Change security question" : "Set a security question",
                changing ? "It's asked when you use Forgot password." : "If you ever forget your password, you'll answer this to reset it.",
                b, "Save", () -> {
                    if (q.getSelectedIndex() <= 0) { err.setText("Pick a question."); return false; }
                    if (answer.getText().trim().length() < 2) { err.setText("Type your answer."); return false; }
                    try {
                        Backend.setSecurityQuestion((String) q.getSelectedItem(), answer.getText(), current.getValue());   // BACKEND
                        return true;
                    } catch (RuntimeException ex) { err.setText(html(ex.getMessage())); return false; }
                });
    }

    /** The question list with a "Pick a question" first item (also used by Register). */
    public static JComboBox<String> questionBox() {
        JComboBox<String> q = new JComboBox<>();
        q.addItem("Pick a question");
        for (String s : Backend.SECURITY_QUESTIONS) q.addItem(s);
        q.setPreferredSize(new Dimension(400, 40));
        return q;
    }

    // ----- small helpers -----

    private static JPanel body(JComponent... parts) {
        JPanel b = Theme.vbox();
        for (JComponent c : parts) b.add(c);
        b.setPreferredSize(new Dimension(430, b.getPreferredSize().height));
        return b;
    }

    /** Label above an input. */
    public static JComponent field(String label, JComponent input) {
        JPanel f = Theme.clear(new BorderLayout(0, 5));
        f.add(Theme.label(label, Font.BOLD, 12, Theme.TEXT_DARK), BorderLayout.NORTH);
        f.add(input, BorderLayout.CENTER);
        f.setBorder(Theme.pad(0, 0, 12, 0));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, f.getPreferredSize().height));
        return f;
    }

    private static JComponent hint(String text) {
        JComponent h = Theme.left(Theme.wrapLabel(text, 360, Font.PLAIN, 12, Theme.TEXT_GRAY));
        h.setBorder(Theme.pad(0, 0, 6, 0));
        return h;
    }

    private static JLabel error() {
        JLabel l = Theme.label(" ", Font.PLAIN, 12, Theme.RED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setVerticalAlignment(SwingConstants.TOP);
        l.setPreferredSize(new Dimension(420, 34));      // room for a 2-line message
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        return l;
    }

    /** Lets a long message wrap inside the popup. */
    private static String html(String s) { return "<html><div style='width:360px'>" + s + "</div></html>"; }
}
