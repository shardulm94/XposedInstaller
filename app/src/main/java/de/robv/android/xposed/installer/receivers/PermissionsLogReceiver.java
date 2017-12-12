package de.robv.android.xposed.installer.receivers;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import de.robv.android.xposed.installer.PermissionListActivity;
import de.robv.android.xposed.installer.PermissionManagerUtil;
import de.robv.android.xposed.installer.R;

/**
 * Created by Arijeet on 10/30/2017.
 */

public class PermissionsLogReceiver extends BroadcastReceiver {

    public final static String TAG = "XposedPermissionLogRecv";

    private void pushNotification(Context context){
        int icon = R.drawable.icon_perm;
        CharSequence tickerText = "Configure Xposed Permissions";
        long when = System.currentTimeMillis(); //now
        NotificationManager notificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(context, PermissionListActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context)
                // Set Icon
                .setSmallIcon(icon)
                // Set Ticker Message
                .setTicker(tickerText)
                // Set Title
                .setContentTitle(tickerText)
                // Set Text
                .setContentText(tickerText)
                // Add an Action Button below Notification
                .addAction(R.drawable.icon_perm, "Action Button", pIntent)
                // Set PendingIntent into Notification
                .setContentIntent(pIntent)
                // Dismiss Notification
                .setAutoCancel(true);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        notificationManager.notify(0, builder.build());
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Log.i(TAG, "perm received......."+intent.getStringExtra("moduleName")+"..."+intent.getStringExtra("packageName"));
        String action = intent.getAction();
        if (action.equalsIgnoreCase(PermissionManagerUtil.PERMISSION_INTENT)) {
            String module_name = intent.getStringExtra("moduleName");
            String package_name = intent.getStringExtra("packageName");
            boolean isAllowed = intent.getBooleanExtra("granted", false);
            if(module_name!= null && package_name!= null)
                PermissionManagerUtil.saveChangesToLog(module_name, package_name, isAllowed);
            pushNotification(context);
        }
    }

}
