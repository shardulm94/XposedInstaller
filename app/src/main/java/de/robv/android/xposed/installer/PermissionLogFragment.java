package de.robv.android.xposed.installer;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static android.content.ContentValues.TAG;
import static de.robv.android.xposed.installer.XposedApp.getPackageLabel;

public class PermissionLogFragment extends Fragment {

    View rootView;
    ListView lv;
    SimpleDateFormat dateFormatter = new SimpleDateFormat("hh:mm:ss");

    public PermissionLogFragment() {

    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.tab_permission_log, container, false);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final List<PermissionManagerUtil.LogEntry> logs = XposedApp.getLogList();

        try {
            lv = (ListView) view.findViewById(R.id.listLogs);
            lv.setAdapter(new CustomListAdapter(this.getActivity(), new ArrayList(logs)));
        } catch (Exception e) {
            Log.e(TAG, "Error onCreate");
        }
    }

    public class CustomListAdapter extends BaseAdapter {

        private Context context;
        private List<PermissionManagerUtil.LogEntry> logs;

        public CustomListAdapter(Context context, List<PermissionManagerUtil.LogEntry> logs) {
            this.context = context;
            this.logs = logs;
        }


        @Override
        public int getCount() {
            return logs.size();
        }

        @Override
        public Object getItem(int i) {
            return logs.get(logs.size() - 1 - i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.list_permission_log_item, viewGroup, false);
            TextView mName = (TextView) rowView.findViewById(R.id.listLogModuleName);
            TextView pName = (TextView) rowView.findViewById(R.id.listLogPackageName);
            TextView status = (TextView) rowView.findViewById(R.id.listLogStatus);
            TextView time = (TextView) rowView.findViewById(R.id.listLogTime);
            PermissionManagerUtil.LogEntry e = (PermissionManagerUtil.LogEntry) getItem(i);
            mName.setText("M:" + getPackageLabel(e.moduleName) + " (" + e.moduleName + ")");
            pName.setText("P:" + getPackageLabel(e.packageName) + " (" + e.packageName + ")");
            status.setText("S:" + e.status);
            time.setText(dateFormatter.format(new Date(e.timestamp)));
            return rowView;
        }

    }
}
