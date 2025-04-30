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
import java.util.Date;
import java.util.Locale;

public class VirtualControllerConfigManager {
    private static final String CONFIG_FILE_PREFIX = "rgpclient_gamepad_config_";
    private static final String CONFIG_FILE_EXTENSION = ".json";

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
     * @return true nếu nhập thành công, false nếu thất bại
     */
    public static boolean importConfig(VirtualController controller, Context context, String filePath) {
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
            
            // Lấy dữ liệu các phần tử
            JSONArray elementsArray = configJson.getJSONArray("elements");
            
            // Xóa cấu hình hiện tại
            SharedPreferences.Editor prefEditor = context.getSharedPreferences(VirtualControllerConfigurationLoader.OSC_PREFERENCE, Activity.MODE_PRIVATE).edit();
            prefEditor.clear();
            
            // Lưu cấu hình mới
            for (int i = 0; i < elementsArray.length(); i++) {
                JSONObject elementConfig = elementsArray.getJSONObject(i);
                int elementId = elementConfig.getInt("elementId");
                prefEditor.putString(String.valueOf(elementId), elementConfig.toString());
            }
            
            prefEditor.apply();
            
            // Làm mới bố cục
            controller.refreshLayout();
            
            return true;
        } catch (JSONException | IOException e) {
            e.printStackTrace();
            return false;
        }
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