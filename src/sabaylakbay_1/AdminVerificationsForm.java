package sabaylakbay_1;

import sabaylakbay_1.backend.Backend;
import sabaylakbay_1.model.IdVerification;
import sabaylakbay_1.model.VerificationStatus;
import sabaylakbay_1.ui.AppWindow;
import sabaylakbay_1.ui.Icons;
import sabaylakbay_1.ui.Page;
import sabaylakbay_1.ui.Theme;
import sabaylakbay_1.ui.components.*;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * ADMIN ID VERIFICATIONS
 * Opens from: admin sidebar "ID Verifications"
 * Tabs: Pending, Approved, Rejected, All
 * Each row: traveler, email, date registered, status. "Review" opens the ID photo with Approve / Reject.
 * Travelers need an approved ID before they can join or create trips.
 * Backend: getVerifications, approveVerification, rejectVerification
 */
public class AdminVerificationsForm extends Page {

    private static final VerificationStatus[] TABS = {VerificationStatus.PENDING, VerificationStatus.APPROVED,
            VerificationStatus.REJECTED, null};

    private final AdminDashboard admin;
    private final TabBar tabs = new TabBar("Pending", "Approved", "Rejected", "All");
    private final JPanel list = new Theme.ScrollableBox(null);

    public AdminVerificationsForm(AdminDashboard admin) {
        this.admin = admin;
        JPanel top = Theme.vbox();
        top.add(header("ID Verifications", "Check the IDs travelers uploaded when they registered.", null));
        JPanel tw = Theme.clear(new BorderLayout());
        tw.setBorder(Theme.pad(0, 32, 0, 32));
        tw.add(tabs);
        top.add(tw);
        add(top, BorderLayout.NORTH);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBorder(Theme.pad(18, 32, 32, 32));
        add(Theme.scroll(list));
        tabs.onChange(i -> refresh());
    }

    @Override
    public void refresh() {
        String[] names = {"Pending", "Approved", "Rejected", "All"};
        for (int i = 0; i < TABS.length; i++)
            // BACKEND: Backend.getVerifications() reads from MySQL
            tabs.setTitle(i, names[i] + " (" + Backend.getVerifications(TABS[i]).size() + ")");
        list.removeAll();
        // BACKEND: Backend.getVerifications() reads from MySQL
        List<IdVerification> items = Backend.getVerifications(TABS[tabs.getSelected()]);
        if (items.isEmpty()) list.add(empty("shield", "Nothing to check right now."));
        for (IdVerification v : items) {
            JComponent row = row(v);
            row.setAlignmentX(LEFT_ALIGNMENT);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
            list.add(row);
            list.add(Theme.gap(10));
        }
        list.revalidate();
        list.repaint();
    }

    private JComponent row(IdVerification v) {
        RoundPanel p = RoundPanel.card(new BorderLayout(14, 0));
        p.setBorder(Theme.pad(12, 16, 12, 16));
        ImageBox thumb = new ImageBox(v.getFilePath(), v.getUserName(), 8, true);
        thumb.setPreferredSize(new Dimension(84, 54));
        p.add(thumb, BorderLayout.WEST);
        JPanel t = Theme.vbox();
        t.add(Theme.left(Theme.row(10, Theme.label(v.getUserName(), Font.BOLD, 14, Theme.TEXT_DARK), Theme.verificationChip(v.getStatus()))));
        String meta = (v.getIdType() != null ? v.getIdType() + "  \u2022  " : "") + v.getUserEmail() + "  \u2022  registered " + Theme.date(v.getUserCreatedAt().toLocalDate())
                + (v.getReviewedAt() != null ? "  \u2022  checked " + Theme.ago(v.getReviewedAt()) : "");
        t.add(Theme.left(Theme.label(meta, Font.PLAIN, 12, Theme.TEXT_GRAY)));
        p.add(Theme.middleLeft(t));
        RoundButton review = new RoundButton(v.getStatus() == VerificationStatus.PENDING ? "Review" : "View",
                RoundButton.Style.OUTLINE).small();
        review.addActionListener(e -> openId(v));
        JPanel r = Theme.clear(new GridBagLayout());
        r.add(review);
        p.add(r, BorderLayout.EAST);
        return p;
    }

    /** Popup with the ID photo and Approve / Reject buttons. */
    /** Small box with a label and a value (ID type, birthday, age). */
    private static JComponent fact(String label, String value) {
        RoundPanel p = new RoundPanel(new BorderLayout(0, 4), 12, new Color(0xF9FAFB), Theme.BORDER);
        p.setBorder(Theme.pad(10, 14, 10, 14));
        p.add(Theme.label(label, Font.PLAIN, 12, Theme.TEXT_GRAY), BorderLayout.NORTH);
        p.add(Theme.label(value, Font.BOLD, 14, Theme.TEXT_DARK), BorderLayout.CENTER);
        return p;
    }

    /** One photo with its title; says so when the file is missing. */
    private static JComponent picture(String title, String path, String seed) {
        JPanel p = Theme.clear(new BorderLayout(0, 8));
        p.add(Theme.label(title, Font.BOLD, 13, Theme.TEXT_DARK), BorderLayout.NORTH);
        boolean found = path != null && ImageBox.load(path) != null;
        if (found) {
            ImageBox img = new ImageBox(path, seed, 12, true);
            img.setPreferredSize(new Dimension(420, 300));
            p.add(img, BorderLayout.CENTER);
        } else {
            RoundPanel none = new RoundPanel(new GridBagLayout(), 12, new Color(0xF9FAFB), Theme.BORDER);
            none.setPreferredSize(new Dimension(420, 300));
            none.add(Theme.label(path == null ? "No photo (uploaded before the face check)" : "Photo not found on this computer",
                    Font.PLAIN, 12, path == null ? Theme.TEXT_GRAY : Theme.RED));
            p.add(none, BorderLayout.CENTER);
        }
        return p;
    }

    /** Pick why the ID was rejected, so the traveler knows what to fix. null = cancelled. */
    private static String rejectReason(Component parent) {
        JComboBox<String> pick = new JComboBox<>(Backend.REJECT_REASONS);
        pick.addItem("Other (type below)");
        pick.setFont(Theme.font(Font.PLAIN, 13));
        pick.setPreferredSize(new Dimension(420, 40));
        RoundTextField other = new RoundTextField("Type the reason");
        other.setEnabled(false);
        pick.addActionListener(e -> other.setEnabled(pick.getSelectedIndex() == pick.getItemCount() - 1));
        JLabel err = Theme.label(" ", Font.PLAIN, 12, Theme.RED);
        JPanel body = Theme.vbox();
        pick.setAlignmentX(LEFT_ALIGNMENT);
        body.add(pick);
        body.add(Theme.gap(10));
        other.setAlignmentX(LEFT_ALIGNMENT);
        body.add(other);
        body.add(Theme.left(err));
        boolean ok = sabaylakbay_1.ui.Dialogs.form(parent, "x", "Reject this ID?",
                "The traveler sees this reason and is asked to upload a new ID and face photo.", body, "Reject ID", () -> {
                    if (pick.getSelectedIndex() == pick.getItemCount() - 1 && other.getText().trim().isEmpty()) {
                        err.setText("Type the reason.");
                        return false;
                    }
                    return true;
                });
        if (!ok) return null;
        return pick.getSelectedIndex() == pick.getItemCount() - 1 ? other.getText().trim() : (String) pick.getSelectedItem();
    }

    private void openId(IdVerification v) {
        // opens inside the main window (Back bar on top), not as a separate window
        sabaylakbay_1.ui.SubPage d = new sabaylakbay_1.ui.SubPage(null, "ID check: " + v.getUserName(), null) { };

        JPanel body = Theme.vbox();
        body.setBorder(Theme.pad(22, 24, 18, 24));
        body.add(Theme.left(Theme.row(10, Theme.label(v.getUserName(), Font.BOLD, 20, Theme.TEXT_DARK), Theme.verificationChip(v.getStatus()))));
        body.add(Theme.left(Theme.label(v.getUserEmail(), Font.PLAIN, 12, Theme.TEXT_GRAY)));
        body.add(Theme.gap(16));

        // details to compare with what's printed on the ID
        JPanel facts = Theme.clear(new GridLayout(1, 3, 12, 0));
        String bday = v.getUserBirthdate() == null ? "Not given" : Theme.date(v.getUserBirthdate());
        String age = v.getUserBirthdate() == null ? "-"
                : java.time.Period.between(v.getUserBirthdate(), java.time.LocalDate.now()).getYears() + " years old";
        facts.add(fact("ID type", v.getIdType() == null ? "Not given" : v.getIdType()));
        facts.add(fact("Birthday on the account", bday));
        facts.add(fact("Age", age));
        facts.setAlignmentX(LEFT_ALIGNMENT);
        facts.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        body.add(facts);
        body.add(Theme.gap(16));

        // ID photo and face photo side by side
        JPanel pics = Theme.clear(new GridLayout(1, 2, 16, 0));
        pics.add(picture("ID photo", v.getFilePath(), v.getUserName()));
        pics.add(picture("Face photo (camera)", v.getSelfiePath(), v.getUserName()));
        pics.setAlignmentX(LEFT_ALIGNMENT);
        pics.setMaximumSize(new Dimension(Integer.MAX_VALUE, 360));
        body.add(pics);
        body.add(Theme.gap(12));
        body.add(Theme.left(Theme.label("Check that the face matches the ID photo, the name matches the account, "
                + "and the birthday matches the ID.", Font.PLAIN, 12, Theme.TEXT_GRAY)));

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bar.setBackground(Color.WHITE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER), Theme.pad(14, 24, 14, 24)));
        RoundButton close = new RoundButton("Close", RoundButton.Style.GHOST);
        close.addActionListener(e -> d.dispose());
        bar.add(close);
        if (v.getStatus() != VerificationStatus.REJECTED) {
            RoundButton reject = new RoundButton("Reject", RoundButton.Style.DANGER).icon("x");
            reject.addActionListener(e -> {
                // BACKEND: Backend.rejectVerification() saves to MySQL
                String why = rejectReason(d);
                if (why != null && Theme.run(d, () -> Backend.rejectVerification(v.getId(), why))) d.dispose();
            });
            bar.add(reject);
        }
        if (v.getStatus() != VerificationStatus.APPROVED) {
            RoundButton approve = new RoundButton("Approve").icon("check");
            approve.addActionListener(e -> {
                // BACKEND: Backend.approveVerification() saves to MySQL
                if (Theme.run(d, () -> Backend.approveVerification(v.getId()))) d.dispose();
            });
            bar.add(approve);
        }
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(body, BorderLayout.CENTER);
        root.add(bar, BorderLayout.SOUTH);
        d.setContentPane(root);
        d.setVisible(true);
        admin.refreshCurrent();
    }
}
