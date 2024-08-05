package com.bonrix.dynamicqrcode;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.HashMap;
import java.util.UUID;

/**
 * Created by Aalok on 3/10/2017.
 */
public class BLEController implements BluetoothAdapter.LeScanCallback {
        private static final UUID DATA_SERVICE = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
//        private static final UUID DATA_SERVICE = UUID.fromString("0000FFF0-0000-1000-8000-00805F9B34FB");
    private static final UUID WRITE_CHARACTERSTIC = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    private static final UUID NOTIFICATION_CHARACTERSTIC = UUID.fromString("0000FFF4-0000-1000-8000-00805F9B34FB");
//    private static final UUID WRITE_CHARACTERSTIC = UUID.fromString("0000FFF3-0000-1000-8000-00805F9B34FB");
    private static final UUID CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothGatt ConnectedGattM;

    private HashMap<String, BluetoothDevice> DevicesM;

    BLEControllerCallback BleControllerCallbackM;
    Context ParentContextM;
    boolean IsConnectedM = false;
    String ConnectedDeviceNameM = "";
    String ConnectedDeviceAddress = "";

    public String getConnectedDeviceName() {
        return ConnectedDeviceNameM;
    }

    public String getConnectedDeviceAddress() {
        return ConnectedDeviceAddress;
    }


    public void SetCallBack(BLEControllerCallback BleControllerCallbackP) {
        BleControllerCallbackM = BleControllerCallbackP;
    }

    public void SetContext(Context ContextP) {
        ParentContextM = ContextP;
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        }
    };

    private Runnable mStopRunnable = new Runnable() {
        @Override
        public void run() {
            StopScan();
        }
    };

    public void Initialize() {
        BluetoothManager manager = (BluetoothManager) ParentContextM.getSystemService(ParentContextM.BLUETOOTH_SERVICE);
        mBluetoothAdapter = manager.getAdapter();
    }

    public void StartScan(boolean ShowUiP, int ScanTimeInSecondsP) {
        DevicesM = new HashMap<String, BluetoothDevice>();
        DevicesM.clear();

        if (ShowUiP) {
            BleControllerCallbackM.ShowProgressMessage("Scanning for Devices...");
        }
        mBluetoothAdapter.startLeScan(this);
        mHandler.postDelayed(mStopRunnable, ScanTimeInSecondsP * 1000);//5000);
    }

    public void StopScan() {
        try {
            mBluetoothAdapter.stopLeScan(this);
            BleControllerCallbackM.OnBleScanComplete(DevicesM);
        } catch (Exception ex) {

        }
    }

    public void ConnectToDevice(String DeviceNameP, boolean ShowUiP) {
        if (IsConnectedM) {
            return;
        }
        BluetoothDevice BluetoothDeviceL = DevicesM.get(DeviceNameP);

        if (BluetoothDeviceL == null) {
            return;
        }

        if (ShowUiP) {
            BleControllerCallbackM.ShowProgressMessage("Trying to Connect " + BluetoothDeviceL.getName()+"-"+BluetoothDeviceL.getAddress());
        }
        ConnectedGattM = BluetoothDeviceL.connectGatt(ParentContextM, false, GattCallbackM);
        IsConnectedM = true;
        ConnectedDeviceNameM = DeviceNameP;
        ConnectedDeviceAddress = BluetoothDeviceL.getAddress();
    }

    public void Disconnect() {
        if (ConnectedGattM != null) {
            ConnectedGattM.disconnect();
            ConnectedGattM = null;
        }
        IsConnectedM = false;
    }

    @Override
    public void onLeScan(BluetoothDevice DeviceP, int rssi, byte[] scanRecord) {
        String DeviceNameL = DeviceP.getName();
        if (DeviceNameL == null) {
            return;
        }
        if (!DevicesM.containsKey(DeviceP.getName())) {
            DevicesM.put(DeviceP.getName(), DeviceP);
        }

    }

    public void ProcessCharacterSticData(byte[] DataP) {
        BleControllerCallbackM.NotificationReceived(DataP);
    }

    public void SendData(String DataP) {

        if (!IsConnectedM) {
            return;
        }
        if (ConnectedGattM == null) {
            return;
        }
        BluetoothGattCharacteristic WriteCharactersticL = ConnectedGattM.getService(DATA_SERVICE).getCharacteristic(WRITE_CHARACTERSTIC);

        if (WriteCharactersticL == null) {
            Log.e("TAG", "WriteCharactersticL null");
        } else {
            Log.e("TAG", "WriteCharactersticL  not null");

        }

        if (WriteCharactersticL == null) {
            return;
        }
//        String DataL = " " + DataP;
//        byte[] DataBytesL = DataP.getBytes();
//        DataBytesL[0] = (byte) DataP.length();
        WriteCharactersticL.setValue(DataP.getBytes());
        ConnectedGattM.writeCharacteristic(WriteCharactersticL);
        Log.e("TAG", "data sent");
    }

    private BluetoothGattCallback GattCallbackM = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt BlueToothGattP, int StatusP, int newState) {
            Log.e("TAG", "onConnectionStateChange");
            if (StatusP == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                BlueToothGattP.discoverServices();
            } else if (StatusP == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
                ConnectedGattM = null;
                IsConnectedM = false;
                BlueToothGattP.close();
                BleControllerCallbackM.DeviceIsDisconnected();
            } else if (StatusP != BluetoothGatt.GATT_SUCCESS) {
                ConnectedGattM = null;
                IsConnectedM = false;
                BlueToothGattP.disconnect();
                BlueToothGattP.close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt BluetoothGattP, int status) {
            Log.e("TAG", "onServicesDiscovered " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BleControllerCallbackM.DeviceIsconnected();
                EnableNotifications(BluetoothGattP);
            } else {
                BleControllerCallbackM.ShowProgressMessage("Errors Occurred while discovering services.");
            }
        }

        private void EnableNotifications(BluetoothGatt bluetoothGatt) {
            Log.e("TAG", "====EnableNotifications==== " + bluetoothGatt.getServices().size());

            for (int i = 0; i < bluetoothGatt.getServices().size(); i++) {
                Log.e("TAG", "====i==== " + i);

                Log.e("TAG", "uuid: " + bluetoothGatt.getServices().get(i).getUuid());

                for (int j = 0; j < bluetoothGatt.getServices().get(i).getCharacteristics().size(); j++) {
                    Log.e("TAG", "j : " + j);
                    Log.e("TAG", "print property : " + bluetoothGatt.getServices().get(i).getCharacteristics().get(j).getUuid());

                }
            }
            BluetoothGattCharacteristic CharacteristicL = bluetoothGatt.getService(DATA_SERVICE).getCharacteristic(NOTIFICATION_CHARACTERSTIC);
            if (CharacteristicL == null) {
                Log.e("TAG", "CharacteristicL is null ");
            } else {
                Log.e("TAG", "CharacteristicL is not null ");
            }
            if (CharacteristicL != null) {
                BleControllerCallbackM.ShowProgressMessage("Retrieved Notification Service.");
            }
            //Enable local notifications
            bluetoothGatt.setCharacteristicNotification(CharacteristicL, true);
            //Enabled remote notifications
            BluetoothGattDescriptor desc = CharacteristicL.getDescriptor(CONFIG_DESCRIPTOR);
            if (desc != null) {
                BleControllerCallbackM.ShowProgressMessage("Retrieved Config Descriptor.");
            }
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bluetoothGatt.writeDescriptor(desc);
            Log.e("TAG", "====EnableNotifications== khatam== ");
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {


        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.e("TAG", "onCharacteristicWrite" + characteristic.getUuid());


        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt BluetoothGattP, BluetoothGattCharacteristic CharacteristicP) {
            Log.e("TAG", "===onCharacteristicChanged=====");

            if (CharacteristicP.getValue() == null) {
                Log.e("TAG", "Error obtaining characteristic value");
                BleControllerCallbackM.ErrorsOccured("Error obtaining characteristic value");
                return;
            }
            byte[] DataL = CharacteristicP.getValue();
            ProcessCharacterSticData(DataL);
        }


        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        }

        private String connectionState(int status) {
            switch (status) {
                case BluetoothProfile.STATE_CONNECTED:
                    return "Connected";
                case BluetoothProfile.STATE_DISCONNECTED:
                    return "Disconnected";
                case BluetoothProfile.STATE_CONNECTING:
                    return "Connecting";
                case BluetoothProfile.STATE_DISCONNECTING:
                    return "Disconnecting";
                default:
                    return String.valueOf(status);
            }
        }
    };
}