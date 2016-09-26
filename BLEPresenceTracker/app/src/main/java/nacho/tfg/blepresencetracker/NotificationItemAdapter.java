package nacho.tfg.blepresencetracker;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Nacho on 23/08/2016.
 */
public class NotificationItemAdapter extends ArrayAdapter<NotificationItem> {

    Context context;
    int resource;
    ArrayList<NotificationItem> data;
    private SparseBooleanArray mSelectedItemsIds;

    public NotificationItemAdapter(Context context, int resource, ArrayList<NotificationItem> data) {
        super(context, resource, data);
        this.context = context;
        this.resource = resource;
        this.data = data;
        mSelectedItemsIds = new SparseBooleanArray();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;

        NotificationItemHolder holder = null;

        if (row == null){
            LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(resource, parent, false);

            holder = new NotificationItemHolder();
            holder.tvText = (TextView)row.findViewById(R.id.item_tv_text);
            holder.tvDate = (TextView)row.findViewById(R.id.item_tv_date);
            holder.tvTime = (TextView)row.findViewById(R.id.item_tv_time);
            holder.container = (RelativeLayout)row.findViewById(R.id.list_item_container);

            row.setTag(holder);
        }
        else{
            holder = (NotificationItemHolder) row.getTag();
        }

        NotificationItem node = data.get(position);
        holder.tvText.setText(node.getText());
        holder.tvDate.setText(node.getDate());
        holder.tvTime.setText(node.getTime());

        if(mSelectedItemsIds.get(position)) {
            holder.container.setBackgroundColor(context.getResources().getColor(android.R.color.holo_blue_light));
        }
        else {
            holder.container.setBackgroundColor(context.getResources().getColor(android.R.color.transparent));
        }

        return row;

    }

    static class NotificationItemHolder{
        TextView tvText;
        TextView tvDate;
        TextView tvTime;
        RelativeLayout container;
    }

    @Override
    public void remove(NotificationItem object) {
        data.remove(object);
        notifyDataSetChanged();
        Log.d("NIA", "dakitu remove, data size: " + data.size());
    }

    public void toggleSelection(int position) {
        selectView(position, !mSelectedItemsIds.get(position));
    }

    public void selectView(int position, boolean value) {
        if (value) {
            mSelectedItemsIds.put(position, value);

        }
        else {
            mSelectedItemsIds.delete(position);
        }
        notifyDataSetChanged();
    }

    public SparseBooleanArray getSelectedIds() {
        return mSelectedItemsIds;
    }

    public int getSelectedCount() {
        return mSelectedItemsIds.size();
    }

    public void removeSelection() {
        mSelectedItemsIds = new SparseBooleanArray();
        notifyDataSetChanged();
    }


}
