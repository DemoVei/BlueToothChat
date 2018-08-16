package com.example.administrator.bluetoothchat_2;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by Administrator on 2018/8/4.
 */

public class DeviceListAdapter extends BaseAdapter {
    private List<BluetoothDevice> mDeviceList;
    private Context mContext;

    private String pairInfo;

    public DeviceListAdapter(Context context, List<BluetoothDevice> deviceList) {
        this.mContext = context;
        this.mDeviceList = deviceList;
    }

    @Override
    public int getCount() {
        return mDeviceList.size();
    }

    public void setItems(List<BluetoothDevice> devices) {
        mDeviceList.clear();
        for (BluetoothDevice d : devices) {
            mDeviceList.add(d);
        }
        notifyDataSetChanged();
    }

    @Override
    public Object getItem(int position) {
        return mDeviceList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        BluetoothDevice device = mDeviceList.get(position);
        ViewHolder holder = null;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.device_list_item, null);
            holder.mDeviceNameTV = (TextView) convertView.findViewById(R.id.deviceNameTV);
            holder.mIsPairTV = (TextView) convertView.findViewById(R.id.isPairTV);
            holder.mMacAddressTV = (TextView) convertView.findViewById(R.id.marAddressTV);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        try {
            holder.mDeviceNameTV.setText(device.getName());
        } catch (Exception e) {
            holder.mDeviceNameTV.setText("null");
        }
        if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
            pairInfo = "已配对";
        } else {
            pairInfo = "未配对";
        }
        holder.mIsPairTV.setText(pairInfo);
        holder.mMacAddressTV.setText(device.getAddress());
        return convertView;
    }

}
