package com.shanodh.seeforme.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.shanodh.seeforme.R;
import com.shanodh.seeforme.models.WearableDevice;

import java.util.List;

/**
 * Adapter for the RecyclerView that displays wearable devices for pairing
 */
public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {
    
    private final List<WearableDevice> devices;
    private final OnDeviceSelectedListener listener;
    
    public interface OnDeviceSelectedListener {
        void onDeviceSelected(WearableDevice device);
    }
    
    public DeviceAdapter(List<WearableDevice> devices, OnDeviceSelectedListener listener) {
        this.devices = devices;
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_device, parent, false);
        return new DeviceViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        WearableDevice device = devices.get(position);
        holder.tvDeviceName.setText(device.getName());
        holder.tvDeviceType.setText(device.getType());
        holder.tvDeviceMac.setText(device.getMacAddress());
        
        // Update selected state
        if (device.isSelected()) {
            holder.cardView.setCardBackgroundColor(
                    holder.itemView.getContext().getResources().getColor(R.color.primary_light));
        } else {
            holder.cardView.setCardBackgroundColor(
                    holder.itemView.getContext().getResources().getColor(R.color.card_background));
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeviceSelected(device);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return devices.size();
    }
    
    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvDeviceName;
        TextView tvDeviceType;
        TextView tvDeviceMac;
        
        DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardDevice);
            tvDeviceName = itemView.findViewById(R.id.tvDeviceName);
            tvDeviceType = itemView.findViewById(R.id.tvDeviceType);
            tvDeviceMac = itemView.findViewById(R.id.tvDeviceMac);
        }
    }
} 