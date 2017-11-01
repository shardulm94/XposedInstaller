package de.robv.android.xposed.installer;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import static android.content.ContentValues.TAG;

/**
 * Created by Arijeet on 10/30/2017.
 */

public class PermissionManagerUtil {

    public static File logFile = new File(XposedApp.BASE_DIR + "log/permlog.json");
    public static File perFile = new File(XposedApp.BASE_DIR + "conf/permissions.json");
    public static String PERMISSION_INTENT = "de.robv.android.xposed.installer.PERMISSION_ACCESS";

    private static ConcurrentHashMap<String, Set<String>> permissionMap= new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, Map<String, Boolean>> logMap = new ConcurrentHashMap<>(); // always flush

    public static File saveChangesToLog(String mname, String pname, boolean isAllowed){
        try {
            //logFile.createNewFile();
            JsonWriter jwriter =  new JsonWriter(new OutputStreamWriter(new FileOutputStream(logFile)));
            jwriter.setIndent("  ");
            jwriter.beginArray();
            jwriter.beginObject();
            jwriter.name("module_name").value(mname);
            jwriter.name("package_name").value(pname);
            jwriter.name("isAllowed").value(isAllowed);
            jwriter.endObject();
            jwriter.endArray();
            jwriter.close();
            return logFile;
        } catch (IOException e) {
            return null;
        }
    }

    public static void updatePermissions(String module, String pname, boolean allow){
        // update the perm map and push to storage
        boolean currAllowed = permissionMap.get(module).contains(pname);
        if(allow && !currAllowed){
            Set<String> plist= new HashSet<>();
            if(plist== null) plist= new HashSet<>();
            plist.add(pname);
            permissionMap.put(module, plist);
            savePermissions();
        } else if(!allow && currAllowed) {
            permissionMap.get(module).remove(pname);
            savePermissions();
        }
    }

    public static boolean isAllowed(String module, String pname){
        try {
            readLogs();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return logMap.containsKey(module) && logMap.get(module).containsKey(pname)
                ? logMap.get(module).get(pname) : false;
    }

    public synchronized static void savePermissions(){
        File tmpWrite = new File(XposedApp.BASE_DIR + "conf/permissions_temp.json");
        try {
            tmpWrite.createNewFile();
            JsonWriter jwriter =  new JsonWriter(new OutputStreamWriter(
                    new FileOutputStream(tmpWrite)));
            jwriter.setIndent("  ");
            jwriter.beginArray();
            for(String mname : permissionMap.keySet()){
                for(String pname : permissionMap.get(mname)){
                    jwriter.beginObject();
                    jwriter.name("name").value(mname);
                    jwriter.name("packages").value(pname);
                    jwriter.endObject();
                }
            }
            jwriter.endArray();
            tmpWrite.renameTo(perFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void readLogModule(JsonReader reader) throws IOException{
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
        if(!mname.isEmpty() && !pname.isEmpty()){
            boolean allowed= permissionMap.get(mname).contains(pname);
            Map<String, Boolean> pmap = new HashMap<>();
            pmap.put(pname, allowed);
            logMap.put(mname, pmap);
        }
        reader.endObject();
    }

    private static void readLogs() throws IOException{
        JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(perFile)));
        try {
            reader.beginArray();
            while (reader.hasNext()) {
                readLogModule(reader);
            }
            reader.endArray();
        } finally {
            reader.close();
        }
        Log.i(TAG, "Loaded Permission Logs: " + permissionMap.toString());
    }

    private static class Module {
        String name;
        Set<String> packages;
    }

    private static void readPermissions() throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(perFile)));
        try {
            readModulesArray(reader);
        } finally {
            reader.close();
        }
        Log.i(TAG, "Loaded Permissions: " + permissionMap.toString());
    }

    private static void readModulesArray(JsonReader reader) throws IOException {
        reader.beginArray();
        while (reader.hasNext()) {
            Module m = readModule(reader);
            permissionMap.put(m.name, m.packages);
        }
        reader.endArray();
    }

    private static Module readModule(JsonReader reader) throws IOException {
        Module m = new Module();

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("name")) {
                m.name = reader.nextString();
            } else if (name.equals("packages")) {
                m.packages = readPackagesSet(reader);
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return m;
    }

    private static Set<String> readPackagesSet(JsonReader reader) throws IOException {
        Set<String> packages = new HashSet<String>();
        reader.beginArray();
        while (reader.hasNext()) {
            packages.add(reader.nextString());
        }
        reader.endArray();
        return packages;
    }
}
