package com.limelight.binding.input.virtual_controller;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VirtualControllerConfigManager {
    private static final String CONFIG_FILE_PREFIX = "rgpclient_gamepad_config_";
    private static final String CONFIG_FILE_EXTENSION = ".json";
    private static final String PROFILES_PREFERENCE = "GAMEPAD_PROFILES";
    private static final String CURRENT_PROFILE_PREFERENCE = "CURRENT_GAMEPAD_PROFILE";
    public static final String DEFAULT_PROFILE_NAME = "Default";
    
    /**
     * Lấy tên cấu hình hiện tại
     * @param context Context của ứng dụng
     * @return Tên cấu hình hiện tại
     */
    public static String getCurrentProfileName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(CURRENT_PROFILE_PREFERENCE, Activity.MODE_PRIVATE);
        return prefs.getString("current_profile", DEFAULT_PROFILE_NAME);
    }
    
    /**
     * Thiết lập cấu hình hiện tại
     * @param context Context của ứng dụng
     * @param profileName Tên cấu hình
     */
    public static void setCurrentProfileName(Context context, String profileName) {
        SharedPreferences.Editor editor = context.getSharedPreferences(CURRENT_PROFILE_PREFERENCE, Activity.MODE_PRIVATE).edit();
        editor.putString("current_profile", profileName);
        editor.apply();
    }
    
    /**
     * Lấy danh sách tên các cấu hình đã lưu
     * @param context Context của ứng dụng
     * @return Danh sách tên cấu hình
     */
    public static List<String> getProfileNames(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PROFILES_PREFERENCE, Activity.MODE_PRIVATE);
        List<String> profiles = new ArrayList<>();
        
        // Luôn thêm cấu hình mặc định
        profiles.add(DEFAULT_PROFILE_NAME);
        
        try {
            JSONArray profilesArray = new JSONArray(prefs.getString("profiles", "[]"));
            for (int i = 0; i < profilesArray.length(); i++) {
                String profileName = profilesArray.getString(i);
                if (!DEFAULT_PROFILE_NAME.equals(profileName)) {
                    profiles.add(profileName);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        
        return profiles;
    }
    
    /**
     * Thêm một cấu hình mới
     * @param context Context của ứng dụng
     * @param profileName Tên cấu hình
     * @return true nếu thêm thành công, false nếu thất bại hoặc cấu hình đã tồn tại
     */
    public static boolean addProfile(Context context, String profileName) {
        List<String> existingProfiles = getProfileNames(context);
        if (existingProfiles.contains(profileName)) {
            return false;
        }
        
        SharedPreferences prefs = context.getSharedPreferences(PROFILES_PREFERENCE, Activity.MODE_PRIVATE);
        try {
            JSONArray profilesArray = new JSONArray(prefs.getString("profiles", "[]"));
            profilesArray.put(profileName);
            
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("profiles", profilesArray.toString());
            editor.apply();
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Xóa một cấu hình
     * @param context Context của ứng dụng
     * @param profileName Tên cấu hình cần xóa
     * @return true nếu xóa thành công, false nếu thất bại hoặc cấu hình không tồn tại
     */
    public static boolean deleteProfile(Context context, String profileName) {
        if (DEFAULT_PROFILE_NAME.equals(profileName)) {
            return false; // Không cho phép xóa cấu hình mặc định
        }
        
        List<String> existingProfiles = getProfileNames(context);
        if (!existingProfiles.contains(profileName)) {
            return false;
        }
        
        // Xóa cấu hình từ danh sách
        SharedPreferences prefs = context.getSharedPreferences(PROFILES_PREFERENCE, Activity.MODE_PRIVATE);
        try {
            JSONArray profilesArray = new JSONArray(prefs.getString("profiles", "[]"));
            JSONArray newProfilesArray = new JSONArray();
            
            for (int i = 0; i < profilesArray.length(); i++) {
                String profile = profilesArray.getString(i);
                if (!profileName.equals(profile)) {
                    newProfilesArray.put(profile);
                }
            }
            
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("profiles", newProfilesArray.toString());
            editor.apply();
            
            // Xóa dữ liệu cấu hình
            SharedPreferences.Editor prefEditor = context.getSharedPreferences(getProfilePreferenceName(profileName), Activity.MODE_PRIVATE).edit();
            prefEditor.clear();
            prefEditor.apply();
            
            // Nếu cấu hình hiện tại bị xóa, chuyển về cấu hình mặc định
            if (profileName.equals(getCurrentProfileName(context))) {
                setCurrentProfileName(context, DEFAULT_PROFILE_NAME);
            }
            
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Lấy tên SharedPreferences cho một cấu hình cụ thể
     * @param profileName Tên cấu hình
     * @return Tên SharedPreferences
     */
    public static String getProfilePreferenceName(String profileName) {
        if (DEFAULT_PROFILE_NAME.equals(profileName)) {
            return VirtualControllerConfigurationLoader.OSC_PREFERENCE;
        } else {
            return VirtualControllerConfigurationLoader.OSC_PREFERENCE + "_" + profileName;
        }
    }
    
    /**
     * Xuất cấu hình gamepad ra file JSON trong thư mục Download
     * @param controller Controller chứa cấu hình cần xuất
     * @param context Context của ứng dụng
     * @return Đường dẫn đến file đã xuất hoặc null nếu thất bại
     */
    public static String exportConfig(VirtualController controller, Context context) {
        try {
            JSONObject configJson = new JSONObject();
            JSONArray elementsArray = new JSONArray();
            
            // Thêm metadata
            configJson.put("version", "1.0");
            configJson.put("exportDate", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));
            configJson.put("profileName", getCurrentProfileName(context));
            
            // Lấy cấu hình từ mỗi phần tử
            for (VirtualControllerElement element : controller.getElements()) {
                JSONObject elementConfig = element.getConfiguration();
                elementConfig.put("elementId", element.elementId);
                elementConfig.put("enabled", element.enabled);
                elementsArray.put(elementConfig);
            }
            
            configJson.put("elements", elementsArray);
            
            // Tạo file trong thư mục Download
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = CONFIG_FILE_PREFIX + timeStamp + CONFIG_FILE_EXTENSION;
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File configFile = new File(downloadsDir, fileName);
            
            // Ghi file
            FileOutputStream fos = new FileOutputStream(configFile);
            fos.write(configJson.toString(4).getBytes(StandardCharsets.UTF_8));
            fos.close();
            
            return configFile.getAbsolutePath();
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Nhập cấu hình gamepad từ file JSON
     * @param controller Controller để áp dụng cấu hình
     * @param context Context của ứng dụng
     * @param filePath Đường dẫn đến file cấu hình
     * @param createNewProfile Tạo cấu hình mới hoặc ghi đè lên cấu hình hiện tại
     * @return true nếu nhập thành công, false nếu thất bại
     */
    public static boolean importConfig(VirtualController controller, Context context, String filePath, boolean createNewProfile) {
        try {
            File configFile = new File(filePath);
            FileInputStream fis = new FileInputStream(configFile);
            
            int size = fis.available();
            byte[] buffer = new byte[size];
            fis.read(buffer);
            fis.close();
            
            String jsonContent = new String(buffer, StandardCharsets.UTF_8);
            JSONObject configJson = new JSONObject(jsonContent);
            
            // Kiểm tra phiên bản
            String version = configJson.optString("version", "1.0");
            // Có thể kiểm tra tương thích phiên bản ở đây nếu cần
            
            // Xác định tên cấu hình
            String importedProfileName = configJson.optString("profileName", "Imported");
            String profileName;
            
            if (createNewProfile) {
                // Tạo tên cấu hình mới không trùng lặp
                String baseProfileName = importedProfileName;
                int counter = 1;
                List<String> existingProfiles = getProfileNames(context);
                
                profileName = baseProfileName;
                while (existingProfiles.contains(profileName)) {
                    profileName = baseProfileName + " (" + counter + ")";
                    counter++;
                }
                
                // Thêm cấu hình mới
                addProfile(context, profileName);
            } else {
                // Sử dụng cấu hình hiện tại
                profileName = getCurrentProfileName(context);
            }
            
            // Lấy dữ liệu các phần tử
            JSONArray elementsArray = configJson.getJSONArray("elements");
            
            // Xóa cấu hình hiện tại
            String preferenceName = getProfilePreferenceName(profileName);
            SharedPreferences.Editor prefEditor = context.getSharedPreferences(preferenceName, Activity.MODE_PRIVATE).edit();
            prefEditor.clear();
            
            // Lưu cấu hình mới
            for (int i = 0; i < elementsArray.length(); i++) {
                JSONObject elementConfig = elementsArray.getJSONObject(i);
                int elementId = elementConfig.getInt("elementId");
                prefEditor.putString(String.valueOf(elementId), elementConfig.toString());
            }
            
            prefEditor.apply();
            
            // Thiết lập cấu hình mới làm cấu hình hiện tại
            if (createNewProfile) {
                setCurrentProfileName(context, profileName);
            }
            
            // Làm mới bố cục
            controller.refreshLayout();
            
            return true;
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Nhập cấu hình gamepad từ file JSON (ghi đè lên cấu hình hiện tại)
     * @param controller Controller để áp dụng cấu hình
     * @param context Context của ứng dụng
     * @param filePath Đường dẫn đến file cấu hình
     * @return true nếu nhập thành công, false nếu thất bại
     */
    public static boolean importConfig(VirtualController controller, Context context, String filePath) {
        return importConfig(controller, context, filePath, false);
    }
    
    /**
     * Lấy danh sách các file cấu hình gamepad trong thư mục Download
     * @return Mảng các đường dẫn file cấu hình
     */
    public static File[] getConfigFileList() {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        return downloadsDir.listFiles((dir, name) -> 
                name.startsWith(CONFIG_FILE_PREFIX) && name.endsWith(CONFIG_FILE_EXTENSION));
    }
} 