package com.limelight.binding.input.virtual_controller;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.limelight.Game;
import com.limelight.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GamepadLayoutManager {
    private static final String TAG = "GamepadLayoutManager";
    private static final String LAYOUTS_DIR = "gamepad_layouts";
    public static final String DEFAULT_LAYOUT_NAME = "default_layout";
    private static final String FILE_EXTENSION = ".json";
    
    private Context context;
    private VirtualController virtualController;
    private String currentLayoutName = DEFAULT_LAYOUT_NAME;
    
    public GamepadLayoutManager(Context context, VirtualController virtualController) {
        this.context = context;
        this.virtualController = virtualController;
        
        // Ensure layouts directory exists
        getLayoutsDir().mkdirs();
        
        // If there are no layouts, create the default one
        if (getAvailableLayouts().isEmpty()) {
            createDefaultLayout();
        }
    }
    
    public String getCurrentLayoutName() {
        return currentLayoutName;
    }
    
    public void setCurrentLayoutName(String layoutName) {
        this.currentLayoutName = layoutName;
    }
    
    public List<String> getAvailableLayouts() {
        List<String> layouts = new ArrayList<>();
        File layoutsDir = getLayoutsDir();
        
        if (layoutsDir.exists() && layoutsDir.isDirectory()) {
            File[] files = layoutsDir.listFiles((dir, name) -> name.endsWith(FILE_EXTENSION));
            if (files != null) {
                for (File file : files) {
                    String name = file.getName();
                    // Remove extension from name
                    name = name.substring(0, name.length() - FILE_EXTENSION.length());
                    layouts.add(name);
                }
            }
        }
        
        return layouts;
    }
    
    public boolean saveLayout(String layoutName) {
        if (layoutName == null || layoutName.isEmpty()) {
            layoutName = currentLayoutName;
        } else {
            currentLayoutName = layoutName;
        }
        
        JSONObject layoutJson = new JSONObject();
        
        try {
            // Save elements information
            JSONObject elementsJson = new JSONObject();
            for (VirtualControllerElement element : virtualController.getElements()) {
                try {
                    elementsJson.put(String.valueOf(element.elementId), element.getConfiguration());
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to save element " + element.elementId, e);
                }
            }
            
            layoutJson.put("elements", elementsJson);
            layoutJson.put("name", layoutName);
            
            // Create the layout directory if it doesn't exist
            File layoutsDir = getLayoutsDir();
            if (!layoutsDir.exists()) {
                boolean created = layoutsDir.mkdirs();
                if (!created) {
                    Log.e(TAG, "Failed to create layouts directory: " + layoutsDir.getAbsolutePath());
                    Toast.makeText(context, "Failed to create layouts directory", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
            
            // Save to file
            File layoutFile = new File(layoutsDir, layoutName + FILE_EXTENSION);
            try (FileOutputStream fos = new FileOutputStream(layoutFile)) {
                String jsonString = layoutJson.toString();
                fos.write(jsonString.getBytes(StandardCharsets.UTF_8));
                Log.i(TAG, "Successfully saved layout to " + layoutFile.getAbsolutePath());
                Log.d(TAG, "Layout JSON: " + jsonString);
                return true;
            } catch (IOException e) {
                Log.e(TAG, "Failed to save layout file: " + layoutFile.getAbsolutePath(), e);
                Toast.makeText(context, "Failed to save layout: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return false;
            }
            
        } catch (JSONException e) {
            Log.e(TAG, "Failed to create layout JSON", e);
            Toast.makeText(context, "Failed to create layout JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return false;
        }
    }
    
    public boolean loadLayout(String layoutName) {
        if (layoutName == null || layoutName.isEmpty()) {
            return false;
        }
        
        File layoutFile = new File(getLayoutsDir(), layoutName + FILE_EXTENSION);
        if (!layoutFile.exists() || !layoutFile.isFile()) {
            return false;
        }
        
        try (FileInputStream fis = new FileInputStream(layoutFile)) {
            byte[] data = new byte[(int) layoutFile.length()];
            fis.read(data);
            String jsonString = new String(data, StandardCharsets.UTF_8);
            
            JSONObject layoutJson = new JSONObject(jsonString);
            JSONObject elementsJson = layoutJson.getJSONObject("elements");
            
            // Reset virtual controller with default layout first
            virtualController.removeElements();
            VirtualControllerConfigurationLoader.createDefaultLayout(virtualController, context);
            
            // Apply saved configuration to each element
            for (VirtualControllerElement element : virtualController.getElements()) {
                String elementId = String.valueOf(element.elementId);
                if (elementsJson.has(elementId)) {
                    try {
                        element.loadConfiguration(elementsJson.getJSONObject(elementId));
                    } catch (JSONException e) {
                        Log.e(TAG, "Failed to load configuration for element " + elementId, e);
                    }
                }
            }
            
            currentLayoutName = layoutName;
            return true;
            
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Failed to load layout file: " + layoutFile.getAbsolutePath(), e);
            return false;
        }
    }
    
    public boolean deleteLayout(String layoutName) {
        if (layoutName == null || layoutName.isEmpty() || layoutName.equals(DEFAULT_LAYOUT_NAME)) {
            // Don't allow deleting the default layout
            return false;
        }
        
        File layoutFile = new File(getLayoutsDir(), layoutName + FILE_EXTENSION);
        if (layoutFile.exists() && layoutFile.isFile()) {
            boolean deleted = layoutFile.delete();
            
            // If current layout was deleted, switch to default
            if (deleted && layoutName.equals(currentLayoutName)) {
                currentLayoutName = DEFAULT_LAYOUT_NAME;
                loadLayout(DEFAULT_LAYOUT_NAME);
            }
            
            return deleted;
        }
        
        return false;
    }
    
    public boolean createNewLayout(String layoutName) {
        if (layoutName == null || layoutName.isEmpty()) {
            return false;
        }
        
        // Save current configuration as the new layout
        currentLayoutName = layoutName;
        return saveLayout(layoutName);
    }
    
    public void createDefaultLayout() {
        // Create default layout based on current virtual controller state
        saveLayout(DEFAULT_LAYOUT_NAME);
    }
    
    private File getLayoutsDir() {
        File filesDir = context.getFilesDir();
        Log.d(TAG, "App files directory: " + filesDir.getAbsolutePath());
        File layoutsDir = new File(filesDir, LAYOUTS_DIR);
        Log.d(TAG, "Layouts directory: " + layoutsDir.getAbsolutePath());
        return layoutsDir;
    }
} 