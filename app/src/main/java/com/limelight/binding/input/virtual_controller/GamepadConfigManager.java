package com.limelight.binding.input.virtual_controller;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.FrameLayout;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.List;

public class GamepadConfigManager {
    private static final String PREFS_NAME = "GamepadConfigs";
    private static final String CONFIG_KEY_PREFIX = "gamepad_config_";
    private static final String VISIBILITY_KEY_PREFIX = "gamepad_visibility_"; 
    public static final int MAX_CONFIGS = 3;

    public static void saveConfig(Context context, VirtualController controller, int configNum) {
        if (configNum < 1 || configNum > MAX_CONFIGS || controller == null) {
            return;
        }

        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            // Save controller layout
            JSONArray configData = new JSONArray();
            List<VirtualControllerElement> elements = controller.getElements();
            
            if (elements != null) {
                for (VirtualControllerElement element : elements) {
                    if (element != null) {
                        JSONObject elementData = element.getConfiguration();
                        elementData.put("type", element.getClass().getSimpleName());
                        elementData.put("id", element.elementId);
                        
                        // Save position and size
                        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) element.getLayoutParams();
                        if (params != null) {
                            elementData.put("x", params.leftMargin);
                            elementData.put("y", params.topMargin);
                            elementData.put("width", params.width);
                            elementData.put("height", params.height);
                            elementData.put("visible", element.getVisibility() == View.VISIBLE);
                            configData.put(elementData);
                        }
                    }
                }
            }

            editor.putString(CONFIG_KEY_PREFIX + configNum, configData.toString());
            editor.putBoolean(VISIBILITY_KEY_PREFIX + configNum, controller.isVisible());
            editor.apply();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadConfig(Context context, VirtualController controller, int configNum) {
        if (configNum < 1 || configNum > MAX_CONFIGS || controller == null) {
            return;
        }

        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String configStr = prefs.getString(CONFIG_KEY_PREFIX + configNum, null);
            boolean isVisible = prefs.getBoolean(VISIBILITY_KEY_PREFIX + configNum, false);

            // Create default layout first
            VirtualControllerConfigurationLoader.createDefaultLayout(controller, context);

            if (configStr != null) {
                JSONArray configData = new JSONArray(configStr);
                
                // Then apply saved configurations to existing elements
                for (int i = 0; i < configData.length(); i++) {
                    JSONObject elementData = configData.getJSONObject(i);
                    if (elementData != null) {
                        int elementId = elementData.getInt("id");
                        
                        // Find matching element
                        for (VirtualControllerElement element : controller.getElements()) {
                            if (element != null && element.elementId == elementId) {
                                try {
                                    element.loadConfiguration(elementData);
                                    
                                    // Update position and size
                                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) element.getLayoutParams();
                                    params.leftMargin = elementData.getInt("x");
                                    params.topMargin = elementData.getInt("y");
                                    params.width = elementData.getInt("width");
                                    params.height = elementData.getInt("height");
                                    element.setLayoutParams(params);

                                    // Set visibility
                                    element.setVisibility(elementData.optBoolean("visible", true) ? View.VISIBLE : View.GONE);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    // If loading individual element fails, continue with next
                                    continue;
                                }
                                break;
                            }
                        }
                    }
                }

                // Set overall controller visibility
                if (isVisible) {
                    controller.setButtonConfigureVisibility(View.VISIBLE);
                    controller.show();
                } else {
                    controller.setButtonConfigureVisibility(View.GONE);
                    controller.hide();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            // If loading fails, ensure we have at least default layout
            VirtualControllerConfigurationLoader.createDefaultLayout(controller, context);
        }
    }

    public static boolean configExists(Context context, int configNum) {
        if (configNum < 1 || configNum > MAX_CONFIGS) {
            return false;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.contains(CONFIG_KEY_PREFIX + configNum);
    }
}
