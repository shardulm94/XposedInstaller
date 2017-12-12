package de.robv.android.xposed.installer.receivers;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.Pair;
import android.widget.Toast;
import de.robv.android.xposed.installer.PermissionManagerUtil;
import de.robv.android.xposed.installer.R;
import de.robv.android.xposed.installer.XposedApp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static de.robv.android.xposed.installer.XposedApp.getPackageLabel;

/**
 * Created by Arijeet on 10/30/2017.
 */

public class PermissionsReceiver extends BroadcastReceiver {

    private final static String TAG = "XposedPermissionLogRecv";
    private static PackageManager mPm;

    public PermissionsReceiver() {
        mPm = XposedApp.getInstance().getPackageManager();
    }

    private void pushNotification(Context context, String module_name, String package_name) {
        CharSequence smallTickerText = String.format("Configure %s (Restart Required)", getPackageLabel(module_name));
        CharSequence tickerText = String.format("%s (%s) wants to hook to %s (%s)", getPackageLabel(module_name),
                module_name, getPackageLabel(package_name), package_name);
        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        Intent allow = new Intent(context, PermissionsReceiver.class);
        allow.putExtra("moduleName", module_name);
        allow.putExtra("packageName", package_name);
        allow.putExtra("moduleNameL", getPackageLabel(module_name));
        allow.putExtra("packageNameL", getPackageLabel(package_name));
        allow.setAction(PermissionManagerUtil.PERMISSION_ALLOW);
        PendingIntent allowpIntent = PendingIntent.getBroadcast(context, 0,
                allow, PendingIntent.FLAG_UPDATE_CURRENT);
        Intent deny = new Intent(context, PermissionsReceiver.class);
        deny.putExtra("moduleName", module_name);
        deny.putExtra("packageName", package_name);
        deny.putExtra("moduleNameL", getPackageLabel(module_name));
        deny.putExtra("packageNameL", getPackageLabel(package_name));
        deny.setAction(PermissionManagerUtil.PERMISSION_DENY);
        PendingIntent denypIntent = PendingIntent.getBroadcast(context, 0,
                deny, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context)
                // Set Icon
                .setSmallIcon(R.mipmap.ic_launcher)
                // Set Ticker Message
                .setTicker(tickerText)
                // Set Title
                .setContentTitle(smallTickerText)
//                // Set Text
                .setContentText(tickerText)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(tickerText))
                // Add an Action Button below Notification
                .addAction(android.R.drawable.ic_menu_add, "Allow", allowpIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Deny", denypIntent)
                // Dismiss Notification
                .setAutoCancel(true);
        allow.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        String pkg = module_name + ":" + package_name;
        notificationManager.notify(pkg.hashCode(), builder.build());
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Log.i(TAG, "perm received......." + intent.getAction());
        String action = intent.getAction();
        String module_name;
        String package_name;
        switch (action) {
            case PermissionManagerUtil.PERMISSION_INTENT:
                module_name = intent.getStringExtra("moduleName");
                package_name = intent.getStringExtra("packageName");
                String status = intent.getStringExtra("status");
                if (module_name != null && package_name != null) {
                    PermissionManagerUtil.saveChangesToLog(module_name, package_name, status);
                    if (status.equals("unknown") && !findPackage(module_name, package_name)) {
                        pushNotification(context, module_name, package_name);
                    }
                }
                break;
            case PermissionManagerUtil.PERMISSION_ALLOW:
                updatePermission(context, intent, true);
                break;
            case PermissionManagerUtil.PERMISSION_DENY:
                updatePermission(context, intent, false);
                break;
            default:
                break;
        }
    }

    private void updatePermission(Context context, Intent intent, boolean granted) {
        String module_name = intent.getStringExtra("moduleName");
        String package_name = intent.getStringExtra("packageName");
        String module_namel = intent.getStringExtra("moduleNameL");
        String package_namel = intent.getStringExtra("packageNameL");
        if (module_name != null && package_name != null) {
            final Map<String, List<Pair<String, Boolean>>> permissionsMap = XposedApp.getInstance().getPermissionsMap();
            if (!permissionsMap.containsKey(module_name)) {
                permissionsMap.put(module_name, new ArrayList<Pair<String, Boolean>>());
            }
            List<Pair<String, Boolean>> packages = permissionsMap.get(module_name);
            packages.add(new Pair(package_name, granted));
            PermissionManagerUtil.savePermissionsFile(permissionsMap);
            Toast.makeText(context, module_namel + " " + (granted ? "allowed" : "denied") + " to hook to " +
                    package_namel, Toast.LENGTH_LONG).show();
        }
        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel((module_name + ":" + package_name).hashCode());
    }

    private boolean findPackage(String module_name, String package_name) {
        final Map<String, List<Pair<String, Boolean>>> permissionsMap = XposedApp.getInstance().getPermissionsMap();
        if (permissionsMap.containsKey(module_name)) {
            List<Pair<String, Boolean>> packages = permissionsMap.get(module_name);
            for (Pair<String, Boolean> p : packages) {
                if (p.first.equals(package_name))
                    return true;
            }
        }
        return false;
    }

}
