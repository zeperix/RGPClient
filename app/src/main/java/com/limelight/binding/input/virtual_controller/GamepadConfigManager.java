package com.limelight.binding.input.virtual_controller;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

public class GamepadConfigManager {
    private static final String PREFS_NAME = "GamepadConfigs";
    private static final String CONFIG_KEY_PREFIX = "gamepad_config_";
    private static final String VISIBILITY_KEY_PREFIX = "gamepad_visibility_";
    private static final int MAX_CONFIGS = 3;

    public static void saveConfig(Context context, VirtualController controller, int configNum) {
        if (configNum < 1 || configNum > MAX_CONFIGS) {
            return;
        }

        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();

            // Save controller layout
            JSONArray configData = new JSONArray();
            for (VirtualControllerElement element : controller.getElements()) {
                JSONObject elementData = element.getConfiguration();
                configData.put(elementData);
            }

            editor.putString(CONFIG_KEY_PREFIX + configNum, configData.toString());
            editor.putBoolean(VISIBILITY_KEY_PREFIX + configNum, controller.isVisible());
            editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void loadConfig(Context context, VirtualController controller, int configNum) {
        if (configNum < 1 || configNum > MAX_CONFIGS) {
            return;
        }

        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String configStr = prefs.getString(CONFIG_KEY_PREFIX + configNum, null);
            boolean isVisible = prefs.getBoolean(VISIBILITY_KEY_PREFIX + configNum, false);

            if (configStr != null) {
                JSONArray configData = new JSONArray(configStr);
                controller.removeElements();
                
                for (int i = 0; i < configData.length(); i++) {
                    JSONObject elementData = configData.getJSONObject(i);
                    // Create and configure element based on saved data
                    VirtualControllerElement element = createElementFromConfig(elementData, controller, context);
                    if (element != null) {
                        element.loadConfiguration(elementData);
                        controller.addElement(element, 
                            elementData.getInt("x"),
                            elementData.getInt("y"),
                            elementData.getInt("width"),
                            elementData.getInt("height"));
                    }
                }

                if (isVisible) {
                    controller.show();
                } else {
                    controller.hide();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static VirtualControllerElement createElementFromConfig(JSONObject config, VirtualController controller, Context context) throws JSONException {
        String elementType = config.getString("type");
        int elementId = config.getInt("id");

        switch (elementType) {
            case "analog_stick":
                return new AnalogStickFree(controller, context, elementId);
            // Add other element types as needed
            default:
                return null;
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
