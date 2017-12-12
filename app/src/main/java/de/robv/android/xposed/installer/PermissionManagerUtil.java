package de.robv.android.xposed.installer;

import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;
import android.util.Pair;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Arijeet on 10/30/2017.
 */

public class PermissionManagerUtil {

    public final static String TAG = "XposedPermissionMgrUtil";

    public static class permJson {
        String name;
        Set<String> packages;

        permJson(String module, Set<String> packs) {
            name = module;
            packages = packs;
        }
    }

    public static File logFile = new File(XposedApp.BASE_DIR + "log/permlog.json");
    public static File perFile = new File(XposedApp.BASE_DIR + "conf/permissions.json");
    public static final String PERMISSION_INTENT = "de.robv.android.xposed.installer.action.PERMISSION_NOTIFICATION";
    public static final String PERMISSION_ALLOW = "de.robv.android.xposed.installer.action.PERMISSION_ALLOW";
    public static final String PERMISSION_DENY = "de.robv.android.xposed.installer.action.PERMISSION_DENY";
    private static ConcurrentHashMap<String, Set<String>> permissionMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, Map<String, Boolean>> logMap = new ConcurrentHashMap<>(); // always flush
    private static Gson gson = new Gson();

    public static void saveChangesToLog(String mname, String pname, String status) {
        try {
            if(!logFile.exists())
            logFile.createNewFile();
            logFile.setReadable(true, false);
            logFile.setWritable(true, false);
            logFile.setExecutable(true, false);
            JsonWriter jwriter = new JsonWriter(new OutputStreamWriter(new FileOutputStream(logFile)));
            jwriter.setIndent("  ");
            jwriter.beginArray();
            jwriter.beginObject();
            jwriter.name("module_name").value(mname);
            jwriter.name("package_name").value(pname);
            jwriter.name("isAllowed").value(status);
            jwriter.endObject();
            jwriter.endArray();
            jwriter.close();
        } catch (IOException e) {
            Log.e(TAG, "Error occurred while saving changes to Log");
        }
    }

    public static void updatePermissions(String module, String pname, boolean allow) {
        // update the perm map and push to storage
        boolean currAllowed = false;
        if (permissionMap.containsKey(module))
            currAllowed = permissionMap.get(module).contains(pname);
        if (allow && !currAllowed) {
            Set<String> plist = new HashSet<>();
            if (plist == null) plist = new HashSet<>();
            plist.add(pname);
            permissionMap.put(module, plist);
            savePermissions();
        } else if (!allow && currAllowed) {
            permissionMap.get(module).remove(pname);
            savePermissions();
        }
    }

    public static ConcurrentHashMap<String, Map<String, Boolean>> getLogMap() {
        try {
            readPermissions();
            readLogs();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return logMap;
    }

    private static List<permJson> readMapintoArray(Map<String, Set<String>> pMap) {
        List<permJson> res = new ArrayList<>();
        for (String key : pMap.keySet()) {
            res.add(new permJson(key, pMap.get(key)));
        }
        return res;
    }

    public synchronized static void savePermissions() {
        File tmpWrite = new File(XposedApp.BASE_DIR + "conf/permissions_temp.json");
        try {
            Gson gs = new Gson();
            tmpWrite.createNewFile();
            tmpWrite.setReadable(true, false);
            tmpWrite.setWritable(true, false);
            tmpWrite.setExecutable(true, false);
            String jsonString = gs.toJson(readMapintoArray(permissionMap));

            FileWriter fwriter = new FileWriter(tmpWrite);
            fwriter.write(jsonString);
            fwriter.close();
            tmpWrite.renameTo(perFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void readLogModule(JsonReader reader) throws IOException {
        reader.beginObject();
        String mname = "";
        String pname = "";
        while (reader.hasNext()) {
            String key = reader.nextName();
            if (key.equals("module_name")) {
                mname = reader.nextString();
            } else if (key.equals("package_name")) {
                pname = reader.nextString();
            } else {
                reader.skipValue();
            }
        }
        if (!mname.isEmpty() && !pname.isEmpty()) {
            boolean allowed = false;
            if (permissionMap.containsKey(mname))
                allowed = permissionMap.get(mname).contains(pname);
            Map<String, Boolean> pmap = new HashMap<>();
            pmap.put(pname, allowed);
            logMap.put(mname, pmap);
        }
        reader.endObject();
    }

    private static void readLogs() throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(logFile)));
        try {

            //PermissionManagerUtil.saveChangesToLog("TestModule", "TestPackage", true);
            reader.beginArray();
            while (reader.hasNext()) {
                readLogModule(reader);
            }
            reader.endArray();
        } catch (Exception e) {
            Log.e(TAG, "Permission read error " + e.getLocalizedMessage());
        } finally {
            reader.close();
        }
        Log.i(TAG, "Loaded Permission Logs: " + permissionMap.toString());
    }

    private static void readListtoMap(List<permJson> pMap) {
        for (permJson entry : pMap) {
            permissionMap.put(entry.name, entry.packages);
        }
        Log.i(TAG, "Loaded Permissions: " + permissionMap.toString());
    }

    private static void readPermissions() throws IOException {
        if (!perFile.exists()) return;
        String content = new Scanner(perFile).useDelimiter("\\Z").next();
        Type token = new TypeToken<List<permJson>>() {
        }.getType();
        List<permJson> pMap = new Gson().fromJson(content, token);
        readListtoMap(pMap);
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
                return new HashMap<>();
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return new HashMap<>();
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
            Map<String, List<Pair<String, Boolean>>> permissionsMap = new HashMap<>();
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
                Map<String, Boolean> packages = new HashMap<>();
                for (Pair<String, Boolean> p : e.getValue()) {
                    packages.put(p.first, p.second);
                }
                permissionsList.add(new PermissionFileModel(e.getKey(), packages));
            }
            return permissionsList;
        }
    }
}
