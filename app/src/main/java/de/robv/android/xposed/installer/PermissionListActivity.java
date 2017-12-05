package de.robv.android.xposed.installer;

import android.app.Activity;
import android.app.ListActivity;
import android.os.Bundle;
import android.util.JsonWriter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.File;

import static android.content.ContentValues.TAG;

/**
 * Created by Arijeet on 10/31/2017.
 */
public class PermissionListActivity extends ListActivity {

    public void onCreate(Bundle icicle) {
        try {

            File logFile = new File(XposedApp.BASE_DIR + "log/permlog.json");
            if (!logFile.exists()) {
                logFile.createNewFile();
                logFile.setReadable(true, false);
                logFile.setWritable(true, false);
                logFile.setExecutable(true, false);
                JsonWriter jwriter = new JsonWriter(new OutputStreamWriter(new FileOutputStream(logFile)));
                jwriter.setIndent("  ");
                jwriter.beginArray();
                jwriter.beginObject();
                jwriter.endObject();
                jwriter.endArray();
                jwriter.close();
            }
            File permissionFile = new File(XposedApp.BASE_DIR + "conf/permissions.json");
            if (!permissionFile.exists()) {
                PermissionManagerUtil.savePermissions();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error occurred while creating Log and permssion json files");
        }

        setTitle("Permission Manager");
        super.onCreate(icicle);
        // create an array of Strings, that will be put to our ListActivity
        try {
            ArrayAdapter<Model> adapter = new InteractiveArrayAdapter(this,getModel());
            setListAdapter(adapter);
        } catch (Exception e) {
            Log.e(TAG, "Error onCreate");
        }
    }

    private List<Model> getModel() {
        Map<String, Map<String, Boolean>> logmap = PermissionManagerUtil.getLogMap();
        List<Model> list = new ArrayList<Model>();
        for (String module : logmap.keySet()) {
            for (String pname : logmap.get(module).keySet()) {
                boolean allowed = logmap.get(module).get(pname);
                list.add(new Model(module, pname, allowed));
            }
        }
        return list;
    }

    public class Model {
        public String module;
        public String packname;
        public boolean isAllowed;

        public Model(String mname, String pname, Boolean allow) {
            module = mname;
            packname = pname;
            isAllowed = allow;
        }
    }

    public class InteractiveArrayAdapter extends ArrayAdapter<Model> {

        private final List<Model> list;
        private final Activity context;

        public InteractiveArrayAdapter(Activity context, List<Model> list) {
            super(context, R.layout.list_permissions, list);
            this.context = context;
            this.list = list;
        }

        class ViewHolder {
            protected TextView module;
            protected TextView packname;
            protected CheckBox checkbox;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = null;
            if (convertView == null) {
                LayoutInflater inflator = context.getLayoutInflater();
                view = inflator.inflate(R.layout.list_permissions, null);
                final ViewHolder viewHolder = new ViewHolder();
                viewHolder.module = (TextView) view.findViewById(R.id.mname);
                viewHolder.packname = (TextView) view.findViewById(R.id.pname);
                viewHolder.checkbox = (CheckBox) view.findViewById(R.id.pbox);
                viewHolder.checkbox
                        .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView,
                                                         boolean isChecked) {
                                Model element = (Model) viewHolder.checkbox
                                        .getTag();
                                boolean changed = element.isAllowed ^ buttonView.isChecked();
                                if (changed) {
                                    element.isAllowed = buttonView.isChecked();
                                    PermissionManagerUtil.updatePermissions(
                                            element.module, element.packname, buttonView.isChecked());
                                }
                            }
                        });
                view.setTag(viewHolder);
                viewHolder.checkbox.setTag(list.get(position));
            } else {
                view = convertView;
                ((ViewHolder) view.getTag()).checkbox.setTag(list.get(position));
            }
            ViewHolder holder = (ViewHolder) view.getTag();
            holder.module.setText(list.get(position).module);
            holder.packname.setText(list.get(position).packname);
            holder.checkbox.setChecked(list.get(position).isAllowed);
            return view;
        }
    }
}
