/**
 * Created by Karim Mreisi.
 */

package com.limelight.binding.input.virtual_controller;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.limelight.LimeLog;
import com.limelight.R;
import com.limelight.binding.input.ControllerHandler;
import com.limelight.preferences.PreferenceConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.text.InputType;
import android.widget.EditText;
import android.content.SharedPreferences;

public class VirtualController {
    public static class ControllerInputContext {
//        public short inputMap = 0x0000;
        public int inputMap = 0;
        public byte leftTrigger = 0x00;
        public byte rightTrigger = 0x00;
        public short rightStickX = 0x0000;
        public short rightStickY = 0x0000;
        public short leftStickX = 0x0000;
        public short leftStickY = 0x0000;
    }

    public enum ControllerMode {
        Active,
        MoveButtons,
        ResizeButtons,
        DisableEnableButtons
    }

    private static final boolean _PRINT_DEBUG_INFORMATION = false;

    private final ControllerHandler controllerHandler;
    private final Context context;
    private final Handler handler;

    private final Runnable delayedRetransmitRunnable = new Runnable() {
        @Override
        public void run() {
            sendControllerInputContextInternal();
        }
    };

    private FrameLayout frame_layout = null;

    ControllerMode currentMode = ControllerMode.Active;
    ControllerInputContext inputContext = new ControllerInputContext();

    private Button buttonConfigure = null;

    private List<VirtualControllerElement> elements = new ArrayList<>();

    private Vibrator vibrator;

    private final VibrationEffect defaultVibrationEffect;

    public VirtualController(final ControllerHandler controllerHandler, FrameLayout layout, final Context context) {
        this.controllerHandler = controllerHandler;
        this.frame_layout = layout;
        this.context = context;
        this.handler = new Handler(Looper.getMainLooper());

        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            defaultVibrationEffect = VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE);
        } else {
            defaultVibrationEffect = null;
        }

        buttonConfigure = new Button(context);
        buttonConfigure.setAlpha(0.25f);
        buttonConfigure.setFocusable(false);
        buttonConfigure.setBackgroundResource(R.drawable.ic_settings);
        buttonConfigure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message;

                if (currentMode == ControllerMode.Active) {
                    currentMode = ControllerMode.DisableEnableButtons;
                    showElements();
                    message = context.getString(R.string.configuration_mode_disable_enable_buttons);
                } else if (currentMode == ControllerMode.DisableEnableButtons){
                    currentMode = ControllerMode.MoveButtons;
                    showEnabledElements();
                    message = context.getString(R.string.configuration_mode_move_buttons);
                } else if (currentMode == ControllerMode.MoveButtons) {
                    currentMode = ControllerMode.ResizeButtons;
                    message = context.getString(R.string.configuration_mode_resize_buttons);
                } else {
                    currentMode = ControllerMode.Active;
                    VirtualControllerConfigurationLoader.saveProfile(VirtualController.this, context);
                    message = context.getString(R.string.configuration_mode_exiting);
                }

                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();

                buttonConfigure.invalidate();

                for (VirtualControllerElement element : elements) {
                    element.invalidate();
                }
            }
        });

    }

    Handler getHandler() {
        return handler;
    }

    public void hide() {
        for (VirtualControllerElement element : elements) {
            element.setVisibility(View.GONE);
        }

        buttonConfigure.setVisibility(View.GONE);
    }

    public void show() {
        showEnabledElements();
        buttonConfigure.setVisibility(View.VISIBLE);
        checkFirstTimeConfigHelp();
    }

    public int switchShowHide() {
        if (buttonConfigure.getVisibility() == View.VISIBLE) {
            hide();
            return 0;
        } else {
            show();
            return 1;
        }
    }

    public void showElements(){
        for(VirtualControllerElement element : elements){
            element.setVisibility(View.VISIBLE);
        }
    }

    public void showEnabledElements(){
        for(VirtualControllerElement element: elements){
            element.setVisibility( element.enabled ? View.VISIBLE : View.GONE );
        }
    }

    public void removeElements() {
        for (VirtualControllerElement element : elements) {
            frame_layout.removeView(element);
        }
        elements.clear();

        frame_layout.removeView(buttonConfigure);
    }

    public void setOpacity(int opacity) {
        for (VirtualControllerElement element : elements) {
            element.setOpacity(opacity);
        }
    }


    public void addElement(VirtualControllerElement element, int x, int y, int width, int height) {
        elements.add(element);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(width, height);
        layoutParams.setMargins(x, y, 0, 0);

        frame_layout.addView(element, layoutParams);
    }

    public List<VirtualControllerElement> getElements() {
        return elements;
    }

    private static final void _DBG(String text) {
        if (_PRINT_DEBUG_INFORMATION) {
            LimeLog.info("VirtualController: " + text);
        }
    }

    public void refreshLayout() {
        removeElements();

        DisplayMetrics screen = context.getResources().getDisplayMetrics();

        int buttonSize = (int)(screen.heightPixels*0.06f);
        
        // Nút cấu hình chính
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(buttonSize, buttonSize);
        params.leftMargin = 15;
        params.topMargin = 15;
        buttonConfigure.setAlpha(0.6f); // Tăng độ trong suốt
        frame_layout.addView(buttonConfigure, params);

        // Thêm nút tùy chọn xuất/nhập và quản lý cấu hình ở góc phải
        Button buttonConfigOptions = new Button(context);
        buttonConfigOptions.setAlpha(0.6f); // Tăng độ trong suốt
        buttonConfigOptions.setFocusable(false);
        buttonConfigOptions.setBackgroundResource(R.drawable.ic_more_vert);
        buttonConfigOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showConfigOptionsDialog();
            }
        });
        
        FrameLayout.LayoutParams optionsParams = new FrameLayout.LayoutParams(buttonSize, buttonSize);
        optionsParams.rightMargin = 15;
        optionsParams.topMargin = 15;
        optionsParams.gravity = android.view.Gravity.RIGHT;
        frame_layout.addView(buttonConfigOptions, optionsParams);
        
        // Hiển thị tên cấu hình hiện tại
        TextView profileNameView = new TextView(context);
        String currentProfile = VirtualControllerConfigManager.getCurrentProfileName(context);
        profileNameView.setText(currentProfile);
        profileNameView.setTextColor(0xFFFFFFFF);
        profileNameView.setShadowLayer(3.0f, 1.0f, 1.0f, 0xFF000000);
        profileNameView.setTextSize(14);
        profileNameView.setAlpha(0.6f);
        
        FrameLayout.LayoutParams profileNameParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT);
        profileNameParams.gravity = android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.TOP;
        profileNameParams.topMargin = 20;
        frame_layout.addView(profileNameView, profileNameParams);
        
        // Thêm nút quản lý cấu hình ở giữa trên cùng
        Button profileManagerButton = new Button(context);
        profileManagerButton.setAlpha(0.6f);
        profileManagerButton.setFocusable(false);
        profileManagerButton.setBackgroundResource(R.drawable.ic_controller_config);
        profileManagerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showProfilesDialog();
            }
        });
        
        FrameLayout.LayoutParams profileButtonParams = new FrameLayout.LayoutParams(buttonSize, buttonSize);
        profileButtonParams.gravity = android.view.Gravity.CENTER_HORIZONTAL | android.view.Gravity.TOP;
        profileButtonParams.topMargin = 50;
        frame_layout.addView(profileManagerButton, profileButtonParams);

        // Start with the default layout
        VirtualControllerConfigurationLoader.createDefaultLayout(this, context);

        // Apply user preferences onto the default layout
        VirtualControllerConfigurationLoader.loadFromPreferences(this, context);
    }
    
    /**
     * Hiển thị hộp thoại tùy chọn cấu hình gamepad
     */
    private void showConfigOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.dialog_config_title);
        
        String[] options = {
            context.getString(R.string.export_controller_config),
            context.getString(R.string.import_controller_config),
            context.getString(R.string.profiles_controller_config),
        };
        
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: // Xuất cấu hình
                        exportControllerConfig();
                        break;
                    case 1: // Nhập cấu hình
                        importControllerConfig();
                        break;
                    case 2: // Quản lý cấu hình
                        showProfilesDialog();
                        break;
                }
            }
        });
        
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }
    
    /**
     * Hiển thị hộp thoại quản lý cấu hình
     */
    private void showProfilesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.profiles_controller_config);
        
        List<String> profileNames = VirtualControllerConfigManager.getProfileNames(context);
        String currentProfile = VirtualControllerConfigManager.getCurrentProfileName(context);
        
        // Tạo danh sách hiển thị với đánh dấu cấu hình hiện tại
        String[] displayNames = new String[profileNames.size()];
        for (int i = 0; i < profileNames.size(); i++) {
            String profileName = profileNames.get(i);
            if (profileName.equals(currentProfile)) {
                displayNames[i] = "✓ " + profileName;
            } else {
                displayNames[i] = profileName;
            }
        }
        
        builder.setItems(displayNames, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showProfileOptionsDialog(profileNames.get(which));
            }
        });
        
        builder.setPositiveButton(R.string.add_profile, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showAddProfileDialog();
            }
        });
        
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }
    
    /**
     * Hiển thị hộp thoại thêm cấu hình mới
     */
    private void showAddProfileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.add_profile);
        
        // Tạo input để nhập tên cấu hình
        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);
        
        builder.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String profileName = input.getText().toString().trim();
                
                if (profileName.isEmpty()) {
                    Toast.makeText(context, R.string.profile_name_empty, Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Lưu cấu hình hiện tại trước khi tạo mới
                VirtualControllerConfigurationLoader.saveProfile(VirtualController.this, context);
                
                // Thêm cấu hình mới
                if (VirtualControllerConfigManager.addProfile(context, profileName)) {
                    // Chuyển sang cấu hình mới
                    VirtualControllerConfigManager.setCurrentProfileName(context, profileName);
                    
                    // Làm mới giao diện để áp dụng cấu hình mới
                    refreshLayout();
                    
                    Toast.makeText(context, 
                        context.getString(R.string.profile_added, profileName), 
                        Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, 
                        context.getString(R.string.profile_exists, profileName), 
                        Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }
    
    /**
     * Hiển thị tùy chọn cho một cấu hình cụ thể
     */
    private void showProfileOptionsDialog(final String profileName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(profileName);
        
        String currentProfile = VirtualControllerConfigManager.getCurrentProfileName(context);
        boolean isCurrentProfile = profileName.equals(currentProfile);
        
        // Tạo các tùy chọn dựa trên trạng thái cấu hình
        List<String> optionsList = new ArrayList<>();
        final List<Integer> actionsList = new ArrayList<>();
        
        if (!isCurrentProfile) {
            optionsList.add(context.getString(R.string.use_profile));
            actionsList.add(0); // Sử dụng cấu hình
        }
        
        // Kiểm tra xem có phải là cấu hình mặc định hay không
        boolean isDefaultProfile = profileName.equals("Default");
        if (!isDefaultProfile) {
            optionsList.add(context.getString(R.string.rename_profile));
            actionsList.add(1); // Đổi tên cấu hình
            
            optionsList.add(context.getString(R.string.delete_profile));
            actionsList.add(2); // Xóa cấu hình
        }
        
        // Chuyển danh sách thành mảng
        String[] options = optionsList.toArray(new String[0]);
        
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int action = actionsList.get(which);
                
                switch (action) {
                    case 0: // Sử dụng cấu hình
                        // Lưu cấu hình hiện tại trước khi chuyển
                        VirtualControllerConfigurationLoader.saveProfile(VirtualController.this, context);
                        
                        // Chuyển cấu hình
                        VirtualControllerConfigManager.setCurrentProfileName(context, profileName);
                        
                        // Làm mới giao diện
                        refreshLayout();
                        
                        Toast.makeText(context, 
                            context.getString(R.string.profile_switched, profileName), 
                            Toast.LENGTH_SHORT).show();
                        break;
                        
                    case 1: // Đổi tên cấu hình
                        showRenameProfileDialog(profileName);
                        break;
                        
                    case 2: // Xóa cấu hình
                        showDeleteProfileConfirmation(profileName);
                        break;
                }
            }
        });
        
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }
    
    /**
     * Hiển thị hộp thoại đổi tên cấu hình
     */
    private void showRenameProfileDialog(final String oldProfileName) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.rename_profile);
        
        // Tạo input để nhập tên mới
        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(oldProfileName);
        builder.setView(input);
        
        builder.setPositiveButton(R.string.rename, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newProfileName = input.getText().toString().trim();
                
                if (newProfileName.isEmpty()) {
                    Toast.makeText(context, R.string.profile_name_empty, Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (newProfileName.equals(oldProfileName)) {
                    // Không có thay đổi
                    return;
                }
                
                // Kiểm tra xem tên mới đã tồn tại chưa
                List<String> profiles = VirtualControllerConfigManager.getProfileNames(context);
                if (profiles.contains(newProfileName)) {
                    Toast.makeText(context,
                        context.getString(R.string.profile_exists, newProfileName),
                        Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Lưu cấu hình hiện tại
                String currentProfile = VirtualControllerConfigManager.getCurrentProfileName(context);
                boolean isCurrentProfile = oldProfileName.equals(currentProfile);
                
                // Lưu cấu hình cũ vào SharedPreferences mới
                String oldPrefName = VirtualControllerConfigManager.getProfilePreferenceName(oldProfileName);
                String newPrefName = VirtualControllerConfigManager.getProfilePreferenceName(newProfileName);
                
                SharedPreferences oldPrefs = context.getSharedPreferences(oldPrefName, Activity.MODE_PRIVATE);
                SharedPreferences.Editor newPrefsEditor = context.getSharedPreferences(newPrefName, Activity.MODE_PRIVATE).edit();
                
                // Sao chép tất cả cấu hình từ cấu hình cũ sang cấu hình mới
                Map<String, ?> allPrefs = oldPrefs.getAll();
                for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
                    Object value = entry.getValue();
                    if (value instanceof String) {
                        newPrefsEditor.putString(entry.getKey(), (String) value);
                    }
                }
                newPrefsEditor.apply();
                
                // Xóa cấu hình cũ
                oldPrefs.edit().clear().apply();
                
                // Cập nhật danh sách cấu hình
                VirtualControllerConfigManager.deleteProfile(context, oldProfileName);
                VirtualControllerConfigManager.addProfile(context, newProfileName);
                
                // Nếu đang sử dụng cấu hình này thì chuyển sang tên mới
                if (isCurrentProfile) {
                    VirtualControllerConfigManager.setCurrentProfileName(context, newProfileName);
                }
                
                Toast.makeText(context,
                    context.getString(R.string.profile_renamed, oldProfileName, newProfileName),
                    Toast.LENGTH_SHORT).show();
                
                // Làm mới giao diện nếu cần
                if (isCurrentProfile) {
                    refreshLayout();
                }
            }
        });
        
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }
    
    /**
     * Hiển thị xác nhận xóa cấu hình
     */
    private void showDeleteProfileConfirmation(final String profileName) {
        // Kiểm tra nếu đây là cấu hình mặc định thì không cho xóa
        if (profileName.equals("Default")) {
            Toast.makeText(context,
                context.getString(R.string.profile_delete_failed, profileName),
                Toast.LENGTH_SHORT).show();
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.delete_profile);
        builder.setMessage(context.getString(R.string.delete_profile_confirm, profileName));
        
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                boolean isCurrentProfile = profileName.equals(VirtualControllerConfigManager.getCurrentProfileName(context));
                
                if (VirtualControllerConfigManager.deleteProfile(context, profileName)) {
                    Toast.makeText(context,
                        context.getString(R.string.profile_deleted, profileName),
                        Toast.LENGTH_SHORT).show();
                    
                    // Làm mới giao diện nếu cấu hình bị xóa là cấu hình đang sử dụng
                    if (isCurrentProfile) {
                        refreshLayout();
                    }
                } else {
                    Toast.makeText(context,
                        context.getString(R.string.profile_delete_failed, profileName),
                        Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }
    
    /**
     * Xuất cấu hình gamepad ra file
     */
    private void exportControllerConfig() {
        // Kiểm tra quyền truy cập bộ nhớ
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, 
                    context.getString(R.string.storage_permission_required), 
                    Toast.LENGTH_SHORT).show();
                
                ActivityCompat.requestPermissions((Activity) context,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    1001);
                return;
            }
        }
        
        // Lưu cấu hình hiện tại trước khi xuất
        VirtualControllerConfigurationLoader.saveProfile(this, context);
        
        String exportPath = VirtualControllerConfigManager.exportConfig(this, context);
        if (exportPath != null) {
            Toast.makeText(context, 
                context.getString(R.string.export_success, exportPath), 
                Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(context, 
                context.getString(R.string.export_failed), 
                Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Nhập cấu hình gamepad từ file
     */
    private void importControllerConfig() {
        // Kiểm tra quyền truy cập bộ nhớ
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) 
                    != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, 
                    context.getString(R.string.storage_permission_required), 
                    Toast.LENGTH_SHORT).show();
                
                ActivityCompat.requestPermissions((Activity) context,
                    new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                    1002);
                return;
            }
        }
        
        final File[] configFiles = VirtualControllerConfigManager.getConfigFileList();
        
        if (configFiles == null || configFiles.length == 0) {
            Toast.makeText(context, 
                context.getString(R.string.no_config_files), 
                Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Tạo danh sách tên file để hiển thị
        String[] fileNames = new String[configFiles.length];
        for (int i = 0; i < configFiles.length; i++) {
            fileNames[i] = configFiles[i].getName();
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.choose_config_file);
        
        builder.setItems(fileNames, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showImportOptionsDialog(configFiles[which]);
            }
        });
        
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }
    
    /**
     * Hiển thị tùy chọn khi nhập cấu hình
     */
    private void showImportOptionsDialog(final File configFile) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.import_options);
        
        String[] options = {
            context.getString(R.string.import_overwrite_current),
            context.getString(R.string.import_as_new_profile)
        };
        
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                boolean createNewProfile = (which == 1);
                boolean success = VirtualControllerConfigManager.importConfig(
                    VirtualController.this, context, configFile.getAbsolutePath(), createNewProfile);
                
                if (success) {
                    Toast.makeText(context, 
                        context.getString(R.string.import_success), 
                        Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, 
                        context.getString(R.string.import_failed), 
                        Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        builder.setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    /**
     * Hiển thị hướng dẫn nhanh về quản lý cấu hình gamepad
     */
    private void showControllerConfigHelp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.controller_config_help_title);
        builder.setMessage(R.string.controller_config_help_content);
        builder.setPositiveButton(R.string.proceed, null);
        builder.show();
    }
    
    /**
     * Kiểm tra và hiển thị hướng dẫn lần đầu sử dụng
     */
    private void checkFirstTimeConfigHelp() {
        SharedPreferences prefs = context.getSharedPreferences("controller_config_help", Activity.MODE_PRIVATE);
        boolean hasShownHelp = prefs.getBoolean("shown_help", false);
        
        if (!hasShownHelp) {
            // Hiển thị hướng dẫn và đánh dấu đã hiển thị
            showControllerConfigHelp();
            prefs.edit().putBoolean("shown_help", true).apply();
        }
    }

    public ControllerMode getControllerMode() {
        return currentMode;
    }

    public ControllerInputContext getControllerInputContext() {
        return inputContext;
    }

    private void sendControllerInputContextInternal() {
        _DBG("INPUT_MAP + " + inputContext.inputMap);
        _DBG("LEFT_TRIGGER " + inputContext.leftTrigger);
        _DBG("RIGHT_TRIGGER " + inputContext.rightTrigger);
        _DBG("LEFT STICK X: " + inputContext.leftStickX + " Y: " + inputContext.leftStickY);
        _DBG("RIGHT STICK X: " + inputContext.rightStickX + " Y: " + inputContext.rightStickY);

        if (controllerHandler != null) {
            controllerHandler.reportOscState(
                    inputContext.inputMap,
                    inputContext.leftStickX,
                    inputContext.leftStickY,
                    inputContext.rightStickX,
                    inputContext.rightStickY,
                    inputContext.leftTrigger,
                    inputContext.rightTrigger
            );
        }
    }

    public void sendControllerInputContext(long vibrationDuration, int vibrationAmplitude) {
        // Cancel retransmissions of prior gamepad inputs
        handler.removeCallbacks(delayedRetransmitRunnable);

        sendControllerInputContextInternal();
        if (frame_layout != null && PreferenceConfiguration.readPreferences(context).enableKeyboardVibrate) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                VibrationEffect effect;
                if (vibrationDuration == 0) {
                    effect = defaultVibrationEffect;
                } else {
                    effect = VibrationEffect.createOneShot(vibrationDuration, vibrationAmplitude);
                }
                vibrator.vibrate(effect);
            } else {
                if (vibrationDuration == 0) {
                    vibrationDuration = 10;
                }
                vibrator.vibrate(vibrationDuration);
            }
        }
        // HACK: GFE sometimes discards gamepad packets when they are received
        // very shortly after another. This can be critical if an axis zeroing packet
        // is lost and causes an analog stick to get stuck. To avoid this, we retransmit
        // the gamepad state a few times unless another input event happens before then.
        handler.postDelayed(delayedRetransmitRunnable, 25);
        handler.postDelayed(delayedRetransmitRunnable, 50);
        handler.postDelayed(delayedRetransmitRunnable, 75);
    }

    public void sendControllerInputContext() {
        sendControllerInputContext(0, 0);
    }
}
