package sabaylakbay_1;

import sabaylakbay_1.model.Review;
import sabaylakbay_1.model.TravelGroup;
import sabaylakbay_1.model.User;
import sabaylakbay_1.backend.Backend;
import sabaylakbay_1.ui.Dialogs;
import sabaylakbay_1.ui.Theme;
import sabaylakbay_1.ui.components.Avatar;
import sabaylakbay_1.ui.components.RoundTextField;
import sabaylakbay_1.ui.components.StarRating;
import sabaylakbay_1.ui.components.TextAreaBox;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * REVIEW FORM (popups after a completed trip)
 * rateCompanions(): give each companion 1 to 5 stars + comment
 * reviewTrip():     rate and review the trip itself
 * Opens from: My Trips (completed trip) or Profile > Trips to review
 * Backend: Backend.addReview()
 */
public final class ReviewForm {

    private ReviewForm() { }

    /** Rate each companion 1 to 5 stars with an optional comment. Returns true if saved. */
    public static boolean rateCompanions(Component parent, TravelGroup g) {
        int me = Backend.currentUserId();
        List<User> others = new ArrayList<>();
        // BACKEND: Backend.getMembers() reads from MySQL
        for (User u : Backend.getMembers(g.getId())) if (u.getId() != me) others.add(u);
        if (others.isEmpty()) { Theme.info(parent, "There's no one else from this trip to rate."); return false; }

        JPanel p = Theme.vbox();
        List<StarRating> stars = new ArrayList<>();
        List<RoundTextField> comments = new ArrayList<>();
        for (User u : others) {
            JPanel row = Theme.clear(new BorderLayout(10, 0));
            row.add(new Avatar(u, 34), BorderLayout.WEST);
            JPanel c = Theme.vbox();
            StarRating s = new StarRating(0, 18, true);
            RoundTextField note = new RoundTextField("Short comment (optional)");
            note.setPreferredSize(new Dimension(320, 36));
            c.add(Theme.left(Theme.row(10, Theme.label(u.getFullName(), Font.BOLD, 12, Theme.TEXT_DARK), s)));
            c.add(Theme.gap(4));
            c.add(Theme.left(note));
            row.add(c);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            p.add(row);
            p.add(Theme.gap(12));
            stars.add(s);
            comments.add(note);
        }
        if (!Dialogs.form(parent, "star", "Rate your companions",
                "How was traveling with them on \"" + g.getTitle() + "\"? Skip anyone you'd rather not rate.", p, "Save ratings")) return false;

        int saved = 0;
        for (int i = 0; i < others.size(); i++) {
            if (stars.get(i).getValue() == 0) continue;
            Review r = new Review();
            r.setGroupId(g.getId());
            r.setReviewerId(me);
            r.setRevieweeId(others.get(i).getId());
            r.setRating(stars.get(i).getValue());
            r.setComment(comments.get(i).getText().trim());
            final Review rr = r;
            // BACKEND: Backend.addReview() saves to MySQL
            if (Theme.run(parent, () -> Backend.addReview(rr))) saved++;
        }
        if (saved == 0) { Theme.error(parent, "Pick at least one star rating."); return false; }
        Backend.showConnected("Thanks! Your review was saved.");
        return true;
    }

    /** Write a review of the trip itself. Returns true if saved. */
    public static boolean reviewTrip(Component parent, TravelGroup g) {
        StarRating stars = new StarRating(0, 24, true);
        TextAreaBox text = new TextAreaBox("What did you like? Anything to improve?", 5);
        text.setPreferredSize(new Dimension(400, 120));
        JPanel p = Theme.vbox();
        p.add(Theme.left(stars));
        p.add(Theme.gap(10));
        p.add(Theme.left(text));
        if (!Dialogs.form(parent, "star", "Review \"" + g.getTitle() + "\"",
                "Your review helps other travelers pick their next trip.", p, "Post review")) return false;
        Review r = new Review();
        r.setGroupId(g.getId());
        r.setReviewerId(Backend.currentUserId());
        r.setRevieweeId(0);
        r.setRating(stars.getValue());
        r.setComment(text.getText().trim());
        if (r.getRating() == 0) { Theme.error(parent, "Please give 1 to 5 stars."); return false; }
        // BACKEND: Backend.addReview() saves to MySQL
        if (Theme.run(parent, () -> Backend.addReview(r))) {
            Backend.showConnected("Thanks! Your review was saved.");
            return true;
        }
        return false;
    }
}
