package de.robv.android.xposed.installer.receivers;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import de.robv.android.xposed.installer.PermissionManagerUtil;

/**
 * Created by Arijeet on 10/30/2017.
 */

public class PermissionsLogReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(final Context context, final Intent intent) {
        String action = intent.getAction();
        if (action.equalsIgnoreCase(PermissionManagerUtil.PERMISSION_INTENT)) {
            String module_name = intent.getStringExtra("moduleName");
            String package_name = intent.getStringExtra("packageName");
            boolean isAllowed = intent.getBooleanExtra("granted", false);
            PermissionManagerUtil.saveChangesToLog(module_name, package_name, isAllowed);
        }
    }

}
