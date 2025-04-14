package com.limelight;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.limelight.binding.input.virtual_controller.GamepadLayoutManager;

import java.util.List;

public class GamepadLayoutActivity extends Activity {
    
    public static final String EXTRA_SELECTED_LAYOUT = "selected_layout";
    public static final int RESULT_LAYOUT_SELECTED = 100;
    public static final int RESULT_LAYOUT_EDIT = 101;
    
    private GamepadLayoutManager layoutManager;
    private ListView layoutListView;
    private LayoutAdapter layoutAdapter;
    private Button btnNewLayout;
    private Game gameActivity;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gamepad_layout);
        
        // Get reference to GameActivity
        gameActivity = Game.getInstance();
        
        // Initialize layout manager
        if (gameActivity != null && gameActivity.getVirtualController() != null) {
            layoutManager = new GamepadLayoutManager(this, gameActivity.getVirtualController());
        } else {
            Toast.makeText(this, "Error: Virtual controller not available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        layoutListView = findViewById(R.id.layout_list);
        btnNewLayout = findViewById(R.id.btn_new_layout);
        
        // Load available layouts
        List<String> layouts = layoutManager.getAvailableLayouts();
        layoutAdapter = new LayoutAdapter(this, layouts);
        layoutListView.setAdapter(layoutAdapter);
        
        // Set onclick for new layout button
        btnNewLayout.setOnClickListener(v -> showNewLayoutDialog());
    }
    
    private void showNewLayoutDialog() {
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
        layoutAdapter.clear();
        layoutAdapter.addAll(layoutManager.getAvailableLayouts());
        layoutAdapter.notifyDataSetChanged();
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
            TextView tvLayoutName = convertView.findViewById(R.id.tv_layout_name);
            Button btnUse = convertView.findViewById(R.id.btn_use);
            Button btnEdit = convertView.findViewById(R.id.btn_edit);
            Button btnDelete = convertView.findViewById(R.id.btn_delete);
            
            tvLayoutName.setText(layoutName);
            
            // Current layout indicator
            if (layoutName != null && layoutName.equals(layoutManager.getCurrentLayoutName())) {
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
                if (layoutName.equals(GamepadLayoutManager.DEFAULT_LAYOUT_NAME)) {
                    Toast.makeText(GamepadLayoutActivity.this, 
                            R.string.gamepad_layout_delete_default, 
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                
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