package de.robv.android.xposed.installer;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckedTextView;
import android.widget.ExpandableListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static android.content.ContentValues.TAG;
import static de.robv.android.xposed.installer.XposedApp.getPackageLabel;

public class PermissionManagerFragment extends Fragment {

    View rootView;
    ExpandableListView lv;

    public PermissionManagerFragment() {

    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.tab_permission_manager, container, false);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Map<String, List<Pair<String, Boolean>>> permissionsMap = XposedApp.getInstance().getPermissionsMap();

        try {
            final List<String> modules = new ArrayList<>(permissionsMap.keySet());
            lv = (ExpandableListView) view.findViewById(R.id.listPermissions);
            lv.setAdapter(new CustomExpandableListAdapter(this.getActivity(), modules, permissionsMap));
            lv.setGroupIndicator(null);
            lv.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
                @Override
                public boolean onChildClick(ExpandableListView parent, View v,
                                            int groupPosition, int childPosition, long id) {
                    CheckedTextView checkbox = (CheckedTextView) v.findViewById(R.id.listPermissionPackageName);
                    checkbox.toggle();

                    Pair<String, Boolean> oldPermission = permissionsMap.get(modules.get(groupPosition)).get
                            (childPosition);
                    permissionsMap.get(modules.get(groupPosition)).set(childPosition, new Pair<>(oldPermission
                            .first, checkbox.isChecked()));

                    PermissionManagerUtil.savePermissionsFile(permissionsMap);
                    return false;
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error onCreate");
        }
    }

    public class CustomExpandableListAdapter extends BaseExpandableListAdapter {

        private Context context;
        private List<String> expandableListTitle;
        private Map<String, List<Pair<String, Boolean>>> expandableListDetail;

        public CustomExpandableListAdapter(Context context, List<String> expandableListTitle,
                                           Map<String, List<Pair<String, Boolean>>> expandableListDetail) {
            this.context = context;
            this.expandableListTitle = expandableListTitle;
            this.expandableListDetail = expandableListDetail;
        }

        @Override
        public Object getChild(int listPosition, int expandedListPosition) {
            return this.expandableListDetail.get(this.expandableListTitle.get(listPosition))
                    .get(expandedListPosition);
        }

        @Override
        public long getChildId(int listPosition, int expandedListPosition) {
            return expandedListPosition;
        }

        @Override
        public View getChildView(int listPosition, final int expandedListPosition,
                                 boolean isLastChild, View convertView, ViewGroup parent) {
            final Pair<String, Boolean> expandedListText = (Pair<String, Boolean>) getChild(listPosition,
                    expandedListPosition);
            if (convertView == null) {
                LayoutInflater layoutInflater = (LayoutInflater) this.context
                        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.list_permission_child, null);
            }
            CheckedTextView expandedListTextView = (CheckedTextView) convertView
                    .findViewById(R.id.listPermissionPackageName);
            expandedListTextView.setText(getPackageLabel(expandedListText.first) + " (" + expandedListText.first + ")");
            expandedListTextView.setChecked(expandedListText.second);
            return convertView;
        }

        @Override
        public int getChildrenCount(int listPosition) {
            return this.expandableListDetail.get(this.expandableListTitle.get(listPosition))
                    .size();
        }

        @Override
        public Object getGroup(int listPosition) {
            return this.expandableListTitle.get(listPosition);
        }

        @Override
        public int getGroupCount() {
            return this.expandableListTitle.size();
        }

        @Override
        public long getGroupId(int listPosition) {
            return listPosition;
        }

        @Override
        public View getGroupView(int listPosition, boolean isExpanded,
                                 View convertView, ViewGroup parent) {
            String listTitle = (String) getGroup(listPosition);
            if (convertView == null) {
                LayoutInflater layoutInflater = (LayoutInflater) this.context.
                        getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.list_permission_group, null);
            }
            TextView listTitleTextView = (TextView) convertView
                    .findViewById(R.id.listPermissionModuleName);
            listTitleTextView.setTypeface(null, Typeface.BOLD);
            listTitleTextView.setText(getPackageLabel(listTitle) + " (" + listTitle + ")");
            return convertView;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isChildSelectable(int listPosition, int expandedListPosition) {
            return true;
        }
    }
}
