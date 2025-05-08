package com.example.myapplication.adapter;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.ui.MainActivity;

import java.util.List;

public class DeviceListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<BluetoothDevice> myBluetoothDeviceList;
    private List<String> myRssiList;
    private OnItemClickListener myItemClickListener;
    private Context context;

    public DeviceListAdapter(List<BluetoothDevice> bluetoothDevices, List<String> rssiList, Context context) {
        this.myBluetoothDeviceList = bluetoothDevices;
        this.myRssiList = rssiList;
        this.context = context;
    }

    public  RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.adapter_device_list, viewGroup, false);
        return new DeviceListViewHolder(view);
    }

    @SuppressLint("MissingPermission")
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, final int i) {
        final DeviceListViewHolder deviceListViewHolder = (DeviceListViewHolder) viewHolder;
        BluetoothDevice bluetoothDevice = myBluetoothDeviceList.get(i);
        String rssi = myRssiList.get(i);

        String deviceName = bluetoothDevice.getName();
        deviceListViewHolder.tvName.setText(bluetoothDevice.getName());
        deviceListViewHolder.tvMac.setText(bluetoothDevice.getAddress());
        deviceListViewHolder.tvRssi.setText("Rssi="+rssi);

        if (myItemClickListener!=null) {
            deviceListViewHolder.rlInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = deviceListViewHolder.getAdapterPosition();
                    if (position!=RecyclerView.NO_POSITION) {
                        // 检查设备名称是否为血压计
                        BluetoothDevice device = myBluetoothDeviceList.get(position);
                        String name = device.getName();

                        myItemClickListener.onItemClick(deviceListViewHolder.rlInfo, position);

                        if (name!=null && name.equals("BM100B") && context instanceof MainActivity) {
                            ((MainActivity) context).showHealthDataUI();
                        }
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return myBluetoothDeviceList.size();
    }

    public class DeviceListViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout rlInfo;
        TextView tvName;
        TextView tvMac;
        TextView tvRssi;

        DeviceListViewHolder(View itemView) {
            super(itemView);
            rlInfo = (RelativeLayout) itemView.findViewById(R.id.rl_info);
            tvName = (TextView) itemView.findViewById(R.id.tv_name);
            tvMac = (TextView) itemView.findViewById(R.id.tv_mac);
            tvRssi = (TextView) itemView.findViewById(R.id.tv_rssi);
        }
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {// 设置Item点击监听
        this.myItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {// 点击回调接口
        void onItemClick(View view, int position);
    }
}