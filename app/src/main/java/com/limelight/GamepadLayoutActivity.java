package com.limelight;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.limelight.binding.input.virtual_controller.GamepadLayoutManager;
import com.limelight.binding.input.virtual_controller.VirtualController;

import java.util.List;

public class GamepadLayoutActivity extends Activity {
    
    private static final String TAG = "GamepadLayoutActivity";
    
    public static final String EXTRA_SELECTED_LAYOUT = "selected_layout";
    public static final int RESULT_LAYOUT_SELECTED = 100;
    public static final int RESULT_LAYOUT_EDIT = 101;
    
    private GamepadLayoutManager layoutManager;
    private FrameLayout layoutListContainer;
    private LayoutAdapter layoutAdapter;
    private Button btnNewLayout;
    private Game gameActivity;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gamepad_layout);
        
        // Get reference to GameActivity
        gameActivity = Game.getInstance();
        
        // Initialize layout manager with VirtualController from Game instance or create a temporary one
        if (gameActivity != null) {
            VirtualController virtualController = gameActivity.getVirtualController();
            
            // If virtual controller doesn't exist yet, we'll still allow viewing layouts
            if (virtualController == null) {
                Log.w(TAG, "Virtual controller not available, will create temporary instance for layout management");
                try {
                    // Create a temporary virtual controller just for layout management
                    FrameLayout tempFrame = new FrameLayout(this);
                    virtualController = new VirtualController(null, tempFrame, this);
                } catch (Exception e) {
                    Log.e(TAG, "Error creating temporary virtual controller", e);
                    Toast.makeText(this, "Error: Failed to initialize layout manager", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
            }
            
            try {
                layoutManager = new GamepadLayoutManager(this, virtualController);
                
                // Check if there's a current layout passed from the intent
                String currentLayout = getIntent().getStringExtra("current_layout");
                if (currentLayout != null && !currentLayout.isEmpty()) {
                    layoutManager.setCurrentLayoutName(currentLayout);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error initializing GamepadLayoutManager", e);
                Toast.makeText(this, "Error: Failed to initialize layout manager", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
        } else {
            Toast.makeText(this, "Error: Game activity not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Check if we should show list or grid based on orientation
        layoutListContainer = findViewById(R.id.layout_list_container);
        btnNewLayout = findViewById(R.id.btn_new_layout);
        
        // Set onclick for new layout button
        btnNewLayout.setOnClickListener(v -> showNewLayoutDialog());
        
        // Load available layouts and setup the appropriate view
        setupLayoutListView();
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
        // Re-setup the layout list view based on new orientation
        setupLayoutListView();
    }
    
    private void setupLayoutListView() {
        // Remove previous view if exists
        if (layoutListContainer.getChildCount() > 0) {
            layoutListContainer.removeAllViews();
        }
        
        // Create appropriate view based on orientation
        boolean isLandscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        
        View layoutView;
        if (isLandscape) {
            // Use GridView for landscape orientation
            GridView gridView = new GridView(this);
            gridView.setNumColumns(2); // Adjust columns as needed
            gridView.setVerticalSpacing(20);
            gridView.setHorizontalSpacing(20);
            gridView.setPadding(20, 20, 20, 20);
            
            // Add specific settings for landscape layout
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            gridView.setLayoutParams(params);
            
            layoutView = gridView;
        } else {
            // Use ListView for portrait orientation
            ListView listView = new ListView(this);
            listView.setDividerHeight(10);
            listView.setPadding(10, 10, 10, 10);
            layoutView = listView;
        }
        
        // Set ID for the layout view
        layoutView.setId(R.id.layout_list);
        
        // Add the new view to container
        layoutListContainer.addView(layoutView);
        
        // Load available layouts and set adapter
        List<String> layouts = layoutManager.getAvailableLayouts();
        layoutAdapter = new LayoutAdapter(this, layouts);
        
        if (isLandscape) {
            ((GridView) layoutView).setAdapter(layoutAdapter);
        } else {
            ((ListView) layoutView).setAdapter(layoutAdapter);
        }
    }
    
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (layoutManager != null) {
            outState.putString("current_layout", layoutManager.getCurrentLayoutName());
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null && layoutManager != null) {
            String currentLayout = savedInstanceState.getString("current_layout");
            if (currentLayout != null && !currentLayout.isEmpty()) {
                layoutManager.setCurrentLayoutName(currentLayout);
            }
        }
    }
    
    private void showNewLayoutDialog() {
        if (isFinishing()) return;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.gamepad_layout_new);
        
        // Set up the input
        final EditText input = new EditText(this);
        builder.setView(input);
        
        // Set up the buttons
        builder.setPositiveButton(R.string.yes, (dialog, which) -> {
            String layoutName = input.getText().toString().trim();
            if (!layoutName.isEmpty()) {
                if (layoutManager.createNewLayout(layoutName)) {
                    refreshLayouts();
                    Toast.makeText(GamepadLayoutActivity.this, 
                            getString(R.string.gamepad_layout_created, layoutName), 
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(GamepadLayoutActivity.this, 
                            R.string.gamepad_layout_create_failed, 
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(GamepadLayoutActivity.this, 
                        R.string.gamepad_layout_name_empty, 
                        Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton(R.string.no, (dialog, which) -> dialog.cancel());
        
        builder.show();
    }
    
    private void refreshLayouts() {
        if (layoutAdapter != null) {
            layoutAdapter.clear();
            layoutAdapter.addAll(layoutManager.getAvailableLayouts());
            layoutAdapter.notifyDataSetChanged();
        }
    }
    
    private class LayoutAdapter extends ArrayAdapter<String> {
        
        public LayoutAdapter(Context context, List<String> layouts) {
            super(context, 0, layouts);
        }
        
        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_gamepad_layout, parent, false);
            }
            
            String layoutName = getItem(position);
            if (layoutName == null) {
                return convertView;
            }
            
            TextView tvLayoutName = convertView.findViewById(R.id.tv_layout_name);
            Button btnUse = convertView.findViewById(R.id.btn_use);
            Button btnEdit = convertView.findViewById(R.id.btn_edit);
            Button btnDelete = convertView.findViewById(R.id.btn_delete);
            
            tvLayoutName.setText(layoutName);
            
            // Current layout indicator
            if (layoutName.equals(layoutManager.getCurrentLayoutName())) {
                tvLayoutName.append(" (Current)");
            }
            
            btnUse.setOnClickListener(v -> {
                if (layoutManager.loadLayout(layoutName)) {
                    // Set selected layout and return to game
                    setResult(RESULT_LAYOUT_SELECTED, getIntent().putExtra(EXTRA_SELECTED_LAYOUT, layoutName));
                    Toast.makeText(GamepadLayoutActivity.this, 
                            getString(R.string.gamepad_layout_selected, layoutName), 
                            Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(GamepadLayoutActivity.this, 
                            R.string.gamepad_layout_load_failed, 
                            Toast.LENGTH_SHORT).show();
                }
            });
            
            btnEdit.setOnClickListener(v -> {
                // Save layout name to edit and return to game for editing
                setResult(RESULT_LAYOUT_EDIT, getIntent().putExtra(EXTRA_SELECTED_LAYOUT, layoutName));
                finish();
            });
            
            btnDelete.setOnClickListener(v -> {
                // Check if this is the default layout - cannot delete it
                if (layoutName.equals(GamepadLayoutManager.DEFAULT_LAYOUT_NAME)) {
                    Toast.makeText(GamepadLayoutActivity.this, 
                            R.string.gamepad_layout_delete_default, 
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                
                if (isFinishing()) return;
                
                new AlertDialog.Builder(getContext())
                        .setTitle(R.string.gamepad_layout_delete_title)
                        .setMessage(getString(R.string.gamepad_layout_delete_message, layoutName))
                        .setPositiveButton(R.string.yes, (dialog, which) -> {
                            if (layoutManager.deleteLayout(layoutName)) {
                                refreshLayouts();
                                Toast.makeText(GamepadLayoutActivity.this, 
                                        getString(R.string.gamepad_layout_deleted, layoutName), 
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(GamepadLayoutActivity.this, 
                                        R.string.gamepad_layout_delete_failed, 
                                        Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();
            });
            
            return convertView;
        }
    }
} 