package com.shanodh.seeforme.ui;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.shanodh.seeforme.R;
import com.shanodh.seeforme.adapters.DeviceAdapter;
import com.shanodh.seeforme.models.WearableDevice;

import java.util.ArrayList;
import java.util.List;

public class ConnectDeviceActivity extends AppCompatActivity {

    private TextView tvStatus;
    private RecyclerView rvDevices;
    private MaterialButton btnSearch, btnPair;
    private DeviceAdapter deviceAdapter;
    private List<WearableDevice> deviceList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_connect_device);
        
        // Setup toolbar with back button
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setTitle("Connect Device");
        }
        
        initializeViews();
        setupRecyclerView();
        setupClickListeners();
    }
    
    private void initializeViews() {
        tvStatus = findViewById(R.id.tvStatus);
        rvDevices = findViewById(R.id.rvDevices);
        btnSearch = findViewById(R.id.btnSearch);
        btnPair = findViewById(R.id.btnPair);
    }
    
    private void setupRecyclerView() {
        deviceAdapter = new DeviceAdapter(deviceList, this::onDeviceSelected);
        rvDevices.setLayoutManager(new LinearLayoutManager(this));
        rvDevices.setAdapter(deviceAdapter);
    }
    
    private void setupClickListeners() {
        btnSearch.setOnClickListener(v -> {
            performHapticFeedback();
            tvStatus.setText("Searching for nearby devices...");
            // Simulated search
            simulateDeviceSearch();
        });
        
        btnPair.setOnClickListener(v -> {
            performHapticFeedback();
            tvStatus.setText("Pairing with selected device...");
            // Simulated pairing
            simulateDevicePairing();
        });
    }
    
    private void onDeviceSelected(WearableDevice device) {
        performHapticFeedback();
        for (WearableDevice d : deviceList) {
            d.setSelected(d.getId() == device.getId());
        }
        deviceAdapter.notifyDataSetChanged();
        
        btnPair.setEnabled(true);
        tvStatus.setText("Selected: " + device.getName());
    }
    
    private void simulateDeviceSearch() {
        // Clear existing devices
        deviceList.clear();
        deviceAdapter.notifyDataSetChanged();
        
        // For demo purposes, simulate a delay as if searching for devices
        btnSearch.setEnabled(false);
        btnPair.setEnabled(false);
        
        tvStatus.postDelayed(() -> {
            // Add simulated device findings
            deviceList.add(new WearableDevice(1, "SeeForMe Watch", "Smart Watch", "00:11:22:33:44:55"));
            deviceList.add(new WearableDevice(2, "SeeForMe Glass", "Smart Glasses", "66:77:88:99:AA:BB"));
            deviceAdapter.notifyDataSetChanged();
            
            tvStatus.setText("Found 2 devices. Select one to pair.");
            btnSearch.setEnabled(true);
            performHapticFeedback();
        }, 2000);
    }
    
    private void simulateDevicePairing() {
        btnPair.setEnabled(false);
        btnSearch.setEnabled(false);
        
        // For demo purposes, simulate a delay as if pairing with device
        tvStatus.postDelayed(() -> {
            tvStatus.setText("Successfully paired with device!");
            btnSearch.setEnabled(true);
            performHapticFeedback();
            
            // In a real app, we would save the paired device info
            tvStatus.postDelayed(this::finish, 1500);
        }, 3000);
    }
    
    private void performHapticFeedback() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(50);
            }
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            performHapticFeedback();
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 