package sabaylakbay_1.ui;

import sabaylakbay_1.ui.components.RoundButton;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;

/**
 * Terms of Service and Privacy Policy, shown inside the main window.
 * Written for the Philippines (Data Privacy Act of 2012, RA 10173).
 *
 *   boolean agreed = TermsPage.open(false);   // false = Terms first, true = Privacy first
 */
public class TermsPage extends SubPage {

    public static final String UPDATED = "November 17, 2026";

    private boolean agreed;
    private final JEditorPane text = new JEditorPane();
    private final JScrollPane scroll;
    private final JToggleButton termsTab = tab("Terms of Service");
    private final JToggleButton privacyTab = tab("Privacy Policy");
    private final JToggleButton photosTab = tab("Photo credits");

    private TermsPage(boolean privacyFirst, boolean showAgree) {
        super(null, "Terms and Privacy", null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        // tabs
        ButtonGroup g = new ButtonGroup();
        g.add(termsTab);
        g.add(privacyTab);
        g.add(photosTab);
        termsTab.addActionListener(e -> showDoc(0));
        privacyTab.addActionListener(e -> showDoc(1));
        photosTab.addActionListener(e -> showDoc(2));
        JPanel tabs = Theme.row(8, termsTab, privacyTab, photosTab);
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDER), new EmptyBorder(16, 32, 14, 32)));
        top.add(tabs, BorderLayout.WEST);
        top.add(Theme.label("Last updated " + UPDATED, Font.PLAIN, 12, Theme.TEXT_GRAY), BorderLayout.EAST);
        root.add(top, BorderLayout.NORTH);

        // the text
        text.setEditable(false);
        text.setContentType("text/html");
        text.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        text.setFont(Theme.font(Font.PLAIN, 14));
        text.setForeground(new Color(0x344054));
        text.setBorder(new EmptyBorder(20, 32, 28, 32));
        StyleSheet css = ((HTMLEditorKit) text.getEditorKit()).getStyleSheet();
        css.addRule("body { color:#344054; }");
        css.addRule("h1 { font-size:22px; color:#101828; margin:0 0 6px 0; }");
        css.addRule("h2 { font-size:15px; color:#0B4F3A; margin:18px 0 4px 0; }");
        css.addRule("p { margin:0 0 8px 0; }");
        css.addRule("li { margin-bottom:4px; }");
        css.addRule(".note { color:#667085; }");
        scroll = Theme.scroll(text);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(Color.WHITE);
        root.add(scroll, BorderLayout.CENTER);

        // footer
        JPanel foot = new JPanel(new BorderLayout());
        foot.setBackground(Color.WHITE);
        foot.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER), new EmptyBorder(12, 32, 12, 32)));
        if (showAgree) {
            foot.add(Theme.label("By tapping I agree, you accept both the Terms and the Privacy Policy.",
                    Font.PLAIN, 12, Theme.TEXT_GRAY), BorderLayout.CENTER);
            RoundButton ok = new RoundButton("I agree");
            ok.setPreferredSize(new Dimension(140, 42));
            ok.addActionListener(e -> { agreed = true; dispose(); });
            foot.add(ok, BorderLayout.EAST);
        } else {
            RoundButton close = new RoundButton("Close", RoundButton.Style.OUTLINE);
            close.setPreferredSize(new Dimension(120, 42));
            close.addActionListener(e -> dispose());
            foot.add(close, BorderLayout.EAST);
        }
        root.add(foot, BorderLayout.SOUTH);

        setContentPane(root);
        (privacyFirst ? privacyTab : termsTab).setSelected(true);
        showDoc(privacyFirst ? 1 : 0);
    }

    /** Opens the page with an "I agree" button. Returns true if the user tapped it. */
    public static boolean open(boolean privacyFirst) {
        TermsPage p = new TermsPage(privacyFirst, true);
        p.setVisible(true);          // waits until closed
        return p.agreed;
    }

    /** Opens the page just for reading (Close button only). */
    public static void read(boolean privacyFirst) {
        new TermsPage(privacyFirst, false).setVisible(true);
    }

    private void showDoc(int doc) {
        text.setText("<html><body>" + (doc == 1 ? PRIVACY : doc == 2 ? photoCredits() : TERMS) + "</body></html>");
        text.setCaretPosition(0);
        SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
    }

    private static JToggleButton tab(String name) {
        JToggleButton b = new JToggleButton(name) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isSelected() ? Theme.GREEN_LIGHT : (getModel().isRollover() ? new Color(0xF2F4F7) : Color.WHITE));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                g2.dispose();
                setForeground(isSelected() ? Theme.GREEN : Theme.TEXT_GRAY);
                super.paintComponent(g);
            }
        };
        b.setFont(Theme.font(Font.BOLD, 13));
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setRolloverEnabled(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(b.getPreferredSize().width + 16, 34));
        return b;
    }

    /** Where every landscape photo in the app comes from (they are free to use with credit). */
    private static String photoCredits() {
        StringBuilder b = new StringBuilder("<h1>Photo credits</h1>"
                + "<p class='note'>The landscape photos in SabayLakbay come from Wikimedia Commons and are free to use "
                + "under Creative Commons or public domain licenses. The photographer and exact license of each photo "
                + "are on its Commons page below. The app shows no photos of people; travelers' pictures are only the "
                + "profile photos they upload themselves.</p><ul>");
        for (String[] c : sabaylakbay_1.model.Photos.CREDITS)
            b.append("<li><b>").append(c[0]).append("</b><br>")
             .append(sabaylakbay_1.model.Photos.page(c[1]).replace("&", "&amp;")).append("</li>");
        return b.append("</ul><p class='note'>Maps: &copy; OpenStreetMap contributors.</p>").toString();
    }

    // =====================================================================

    private static final String TERMS =
        "<h1>Terms of Service</h1>"
      + "<p class='note'>Please read these Terms before you create an account. They explain what SabayLakbay is, "
      + "what we expect from you, and what you can expect from us.</p>"

      + "<h2>1. What SabayLakbay is</h2>"
      + "<p>SabayLakbay is an app that helps travelers in the Philippines find and join group trips. "
      + "Trips are created and run by the travelers themselves. SabayLakbay is not a travel agency, tour operator, "
      + "or transport company, and we do not organize, sell, or guide any trip.</p>"

      + "<h2>2. Who can use it</h2>"
      + "<ul><li>You must be at least 18 years old.</li>"
      + "<li>You may have only one account, and it must be in your real name.</li>"
      + "<li>The details you give us (name, birthday, address, ID) must be true and your own.</li></ul>"

      + "<h2>3. ID and face check</h2>"
      + "<p>To keep everyone safe, every traveler is verified. You will upload a valid government ID and take a "
      + "face photo. Our admins compare them by hand to confirm the ID is yours and that your birthday matches. "
      + "Once approved, your profile shows a verified badge. We may reject a "
      + "verification if the photos are unclear or the details do not match. You can always submit again.</p>"

      + "<h2>4. Your account</h2>"
      + "<ul><li>Keep your password private. You are responsible for activity on your account.</li>"
      + "<li>Tell us right away if you think someone else is using your account.</li>"
      + "<li>Your birthday cannot be changed after sign-up, because it is checked against your ID. "
      + "If it is wrong, contact us and we will correct it after checking your ID.</li></ul>"

      + "<h2>5. Joining and creating trips</h2>"
      + "<ul><li>If you create a trip, describe it honestly: place, dates, costs, difficulty, and what is included.</li>"
      + "<li>Shared costs and payments are agreed between travelers. SabayLakbay does not collect or hold payments "
      + "and is not part of those agreements.</li>"
      + "<li>Follow local laws, park rules, and the instructions of local government units and guides.</li>"
      + "<li>Travel has risks (weather, sea and mountain conditions, accidents). Check conditions, bring proper "
      + "gear, and decide for yourself whether a trip is safe for you. Join only what you are fit to do.</li></ul>"

      + "<h2>6. Things you must not do</h2>"
      + "<ul><li>Pretend to be someone else, or use another person's ID or photo.</li>"
      + "<li>Harass, threaten, scam, or discriminate against other travelers.</li>"
      + "<li>Post false, hateful, sexual, or illegal content, or anything that breaks the Cybercrime Prevention "
      + "Act of 2012 (RA 10175) or other Philippine laws.</li>"
      + "<li>Share other travelers' personal information outside the app without their permission.</li>"
      + "<li>Use the app to sell unrelated products or services, or to collect other users' data.</li>"
      + "<li>Try to break, hack, or overload the app.</li></ul>"

      + "<h2>7. Reports and safety</h2>"
      + "<p>You can report a traveler, group, or message. Our admins review every report and may warn, "
      + "suspend, or ban accounts that break these Terms. If you are in danger, contact the police (911) or "
      + "local authorities first. Reporting in the app does not replace emergency help.</p>"

      + "<h2>8. Your content</h2>"
      + "<p>You own what you post (trip details, messages, reviews, photos). You allow SabayLakbay to show it "
      + "inside the app so the service can work. We do not sell your content.</p>"

      + "<h2>9. Suspension and closing your account</h2>"
      + "<p>We may suspend or ban an account that breaks these Terms, and we will tell you why. You can ask us to "
      + "close your account at any time. If you think we made a mistake, you can contact us and ask for a review.</p>"

      + "<h2>10. Limits of our responsibility</h2>"
      + "<p>We work to keep the app safe and accurate, but we cannot control what travelers do during a trip. "
      + "To the extent allowed by Philippine law, SabayLakbay is not responsible for losses, injuries, or "
      + "disputes that come from trips arranged between users. Nothing in these Terms takes away rights you have "
      + "under the Consumer Act of the Philippines (RA 7394), the Data Privacy Act of 2012 (RA 10173), or "
      + "any other law that cannot be waived.</p>"

      + "<h2>11. Changes to these Terms</h2>"
      + "<p>If we change these Terms, we will update the date at the top and tell you in the app. "
      + "If you keep using SabayLakbay after that, the new Terms apply.</p>"

      + "<h2>12. Governing law</h2>"
      + "<p>These Terms follow the laws of the Republic of the Philippines.</p>"

      + "<h2>13. Contact</h2>"
      + "<p>Questions? Email <b>support@sabaylakbay.com</b>.</p>";

    private static final String PRIVACY =
        "<h1>Privacy Policy</h1>"
      + "<p class='note'>We follow the Data Privacy Act of 2012 (Republic Act No. 10173), its Implementing Rules, "
      + "and the issuances of the National Privacy Commission (NPC). This policy explains what we collect, why, "
      + "and the rights you have.</p>"

      + "<h2>1. What we collect</h2>"
      + "<ul><li><b>Account:</b> email address and password (stored as a one-way SHA-256 hash, never as plain text).</li>"
      + "<li><b>Profile:</b> full name, mobile number, birthday, gender, profile photo, and emergency contact.</li>"
      + "<li><b>Address:</b> street, barangay, city or municipality, province, ZIP code, and an optional map pin.</li>"
      + "<li><b>Verification:</b> ID type, a photo of your government ID, and a face photo.</li>"
      + "<li><b>Interests:</b> the activities you pick (like hiking or beaches).</li>"
      + "<li><b>Activity in the app:</b> trips you create or join, messages, reviews, reports, and notifications.</li></ul>"
      + "<p>We do not use your device's GPS. Your location is only what you type or pin on the map yourself.</p>"

      + "<h2>2. Sensitive information</h2>"
      + "<p>Your government ID, birthday, and face photo are sensitive personal information under the law. "
      + "We collect them only with your consent, given when you agree to this policy, and only to verify "
      + "your identity and age. The face photo is compared with your ID by a person (an admin). "
      + "We do not use it for automatic face recognition outside the face check, and we do not share it.</p>"

      + "<h2>3. Why we use your data</h2>"
      + "<ul><li>To create and secure your account.</li>"
      + "<li>To verify that you are a real person, 18 or older.</li>"
      + "<li>To show trips that match your interests and trips near your home.</li>"
      + "<li>To let you join groups, chat, and leave reviews.</li>"
      + "<li>To contact your emergency contact only if there is a real emergency during a trip.</li>"
      + "<li>To review reports and keep the community safe.</li></ul>"
      + "<p>We do not sell your personal data, and we do not use it for advertising.</p>"

      + "<h2>4. Who can see your data</h2>"
      + "<ul><li><b>Other travelers</b> see only your name, profile photo, verified badge, ratings and reviews, "
      + "the date you joined, and the trips and messages you share with them. They do not see your email, "
      + "mobile number, address, birthday, gender, interests, ID, face photo, or emergency contact.</li>"
      + "<li><b>SabayLakbay admins</b> see your ID and face photo only to verify you, and your details only "
      + "when reviewing a report.</li>"
      + "<li><b>Authorities</b>, only when required by law or a lawful order.</li></ul>"

      + "<h2>5. Maps</h2>"
      + "<p>Maps come from OpenStreetMap. When you search or pin a place, the place name or map position is sent "
      + "to OpenStreetMap's services to find it. Your name and account details are not sent.</p>"

      + "<h2>6. How we protect it</h2>"
      + "<p>We use reasonable organizational, physical, and technical security measures: hashed passwords, "
      + "admin-only access to IDs and face photos, and limited access to the database. If a breach puts your "
      + "data at real risk, we will notify you and the NPC as the law requires.</p>"

      + "<h2>7. How long we keep it</h2>"
      + "<p>We keep your data while your account is active. When you close your account, we delete or "
      + "anonymize your personal data, including your ID and face photos, unless the law requires us to keep "
      + "some of it longer (for example, for an open report or a legal claim).</p>"

      + "<h2>8. Your rights</h2>"
      + "<p>Under the Data Privacy Act, you have the right to:</p>"
      + "<ul><li><b>Be informed</b> about how your data is used.</li>"
      + "<li><b>Access</b> a copy of your data.</li>"
      + "<li><b>Correct</b> wrong or outdated data.</li>"
      + "<li><b>Object</b> to processing, and <b>withdraw your consent</b>.</li>"
      + "<li><b>Erasure or blocking</b> of your data.</li>"
      + "<li><b>Data portability</b>, to get your data in a usable format.</li>"
      + "<li><b>Damages</b> if you were harmed by wrong use of your data.</li>"
      + "<li><b>File a complaint</b> with the National Privacy Commission (privacy.gov.ph).</li></ul>"
      + "<p>If you withdraw consent for your ID or face photo, we can no longer keep your account verified.</p>"

      + "<h2>9. Changes to this policy</h2>"
      + "<p>If we change this policy, we will update the date at the top and tell you in the app.</p>"

      + "<h2>10. Contact our Data Protection Officer</h2>"
      + "<p>To use your rights or ask a question, email <b>privacy@sabaylakbay.com</b>. "
      + "We will reply within 15 working days.</p>";
}
