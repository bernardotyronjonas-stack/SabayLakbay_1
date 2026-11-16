package sabaylakbay_1.ui.components;

import sabaylakbay_1.ui.Theme;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

/** Date or time picker (spinner) that works with LocalDate / LocalTime. */
public class DateField extends JSpinner {

    private final boolean timeOnly;

    private DateField(boolean timeOnly, String pattern) {
        super(new SpinnerDateModel(new Date(), null, null, timeOnly ? Calendar.MINUTE : Calendar.DAY_OF_MONTH));
        this.timeOnly = timeOnly;
        setEditor(new JSpinner.DateEditor(this, pattern));
        setFont(Theme.font(Font.PLAIN, 13));
        setPreferredSize(new Dimension(150, 40));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xD9DEE3)), BorderFactory.createEmptyBorder(0, 6, 0, 0)));
    }

    public static DateField date() { return new DateField(false, "MMM d, yyyy"); }

    public static DateField time() { return new DateField(true, "h:mm a"); }

    public LocalDate getDate() {
        return ((Date) getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public void setDate(LocalDate d) {
        if (d != null) setValue(Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant()));
    }

    public LocalTime getTime() {
        return ((Date) getValue()).toInstant().atZone(ZoneId.systemDefault()).toLocalTime().withSecond(0).withNano(0);
    }

    public void setTime(LocalTime t) {
        if (t != null) setValue(Date.from(t.atDate(LocalDate.now()).atZone(ZoneId.systemDefault()).toInstant()));
    }
}
