package nacho.tfg.blepresencetracker;

/**
 * Created by Nacho on 23/08/2016.
 */
public class NotificationItem {

    private String text;
    private String time;
    private String date;

    public NotificationItem(String text, String date, String time){
        this.text = text;
        this.date = date;
        this.time = time;
    }

    public String getText() {
        return text;
    }

    public String getTime() {
        return time;
    }

    public String getDate() {
        return date;
    }
}
