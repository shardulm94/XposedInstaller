package de.robv.android.xposed.installer;

import android.util.Log;
import android.util.Pair;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Created by Arijeet on 10/30/2017.
 */

public class PermissionManagerUtil {

    public final static String TAG = "XposedPermissionMgrUtil";

    public static final String PERMISSION_INTENT = "de.robv.android.xposed.installer.action.PERMISSION_NOTIFICATION";
    public static final String PERMISSION_ALLOW = "de.robv.android.xposed.installer.action.PERMISSION_ALLOW";
    public static final String PERMISSION_DENY = "de.robv.android.xposed.installer.action.PERMISSION_DENY";
    private static Gson gson = new Gson();

    public static void saveChangesToLog(String mname, String pname, String status) {
        List<LogEntry> logs = XposedApp.getLogList();
        if (logs.size() >= 100) {
            logs.remove(0);
        }
        logs.add(new LogEntry(mname, pname, status, System.currentTimeMillis()));
        saveLogFile(logs);
    }

    public static List<LogEntry> readLogFile() {
        try {
            File logFile = new File(XposedApp.BASE_DIR + "log/permlog.json");
            if (logFile.exists()) {
                FileReader fr = new FileReader(logFile);
                Type token = new TypeToken<List<LogEntry>>() {
                }.getType();
                List<LogEntry> logs = gson.fromJson(fr, token);
                fr.close();
                return logs;
            } else {
                return new ArrayList<>();
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return new ArrayList<>();
    }

    public static void saveLogFile(List<LogEntry> logs) {
        try {
            File logFile = new File(XposedApp.BASE_DIR + "log/permlog.json");
            FileWriter fw = new FileWriter(logFile);
            Type token = new TypeToken<List<LogEntry>>() {
            }.getType();
            gson.toJson(logs, token, fw);
            fw.close();
            logFile.setReadable(true, false);
            logFile.setWritable(true);
            logFile.setExecutable(false);
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public static class LogEntry {
        String moduleName;
        String packageName;
        String status;
        long timestamp;

        public LogEntry(String moduleName, String packageName, String status, long timestamp) {
            this.moduleName = moduleName;
            this.packageName = packageName;
            this.status = status;
            this.timestamp = timestamp;
        }
    }

    public static Map<String, List<Pair<String, Boolean>>> readPermissionsFile() {
        try {
            File permissionsFile = new File(XposedApp.BASE_DIR + "conf/permissions.json");
            if (permissionsFile.exists()) {
                FileReader fr = new FileReader(permissionsFile);
                Type token = new TypeToken<List<PermissionFileModel>>() {
                }.getType();
                List<PermissionFileModel> permissionsList = gson.fromJson(fr, token);
                fr.close();
                return PermissionFileModel.toPermissionMap(permissionsList);
            } else {
                return new LinkedHashMap<>();
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return new LinkedHashMap<>();
    }

    public static void savePermissionsFile(Map<String, List<Pair<String, Boolean>>> permissionsMap) {
        try {
            File permissionsFile = new File(XposedApp.BASE_DIR + "conf/permissions.json");
            FileWriter fw = new FileWriter(permissionsFile);
            Type token = new TypeToken<List<PermissionFileModel>>() {
            }.getType();
            List<PermissionFileModel> permissionsList = PermissionFileModel.fromPermissionMap(permissionsMap);
            gson.toJson(permissionsList, token, fw);
            fw.close();
            permissionsFile.setReadable(true, false);
            permissionsFile.setWritable(false);
            permissionsFile.setExecutable(false);
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    private static class PermissionFileModel {
        String name;
        Map<String, Boolean> packages;

        PermissionFileModel(String name, Map<String, Boolean> packages) {
            this.name = name;
            this.packages = packages;
        }

        public static Map<String, List<Pair<String, Boolean>>> toPermissionMap(List<PermissionFileModel> permList) {
            Map<String, List<Pair<String, Boolean>>> permissionsMap = new LinkedHashMap<>();
            for (PermissionFileModel pfm : permList) {
                List<Pair<String, Boolean>> packages = new ArrayList<>();
                for (Map.Entry<String, Boolean> e : pfm.packages.entrySet()) {
                    packages.add(new Pair<>(e.getKey(), e.getValue()));
                }
                permissionsMap.put(pfm.name, packages);
            }
            return permissionsMap;
        }

        public static List<PermissionFileModel> fromPermissionMap(Map<String, List<Pair<String, Boolean>>> permMap) {
            List<PermissionFileModel> permissionsList = new LinkedList<>();
            for (Map.Entry<String, List<Pair<String, Boolean>>> e : permMap.entrySet()) {
                Map<String, Boolean> packages = new LinkedHashMap<>();
                for (Pair<String, Boolean> p : e.getValue()) {
                    packages.put(p.first, p.second);
                }
                permissionsList.add(new PermissionFileModel(e.getKey(), packages));
            }
            return permissionsList;
        }
    }
}
