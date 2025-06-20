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
import android.os.Looper;
import android.os.Message;
import android.util.Log;


import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

/**
 * Created by Aalok on 3/10/2017.
 */
public class BLEController implements BluetoothAdapter.LeScanCallback {
//        private static final UUID DATA_SERVICE = UUID.fromString("12345678-1234-1234-1234-123456789abc");
//    private static final UUID WRITE_CHARACTERSTIC = UUID.fromString("87654321-4321-4321-4321-cba987654321");
//    private static final UUID NOTIFICATION_CHARACTERSTIC = UUID.fromString("98765432-1234-1234-1234-123456789abc");
//    private static final UUID CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private static final UUID DATA_SERVICE = UUID.fromString("12345678-1234-1234-1234-123456789abc");
    private static final UUID WRITE_CHARACTERSTIC = UUID.fromString("87654321-4321-4321-4321-cba987654321");
    private static final UUID NOTIFICATION_CHARACTERSTIC = UUID.fromString("98765432-1234-1234-1234-123456789abc");
    private static final UUID CONFIG_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private int REQUESTED_MTU = 512;
    private int currentChunkIndex = 0;

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

    private boolean isMtuSet = false;
    private MtuCallback mtuCallback;
    private Queue<byte[]> chunkQueue = new LinkedList<>();
    private boolean isWriting = false;

    public void SetCallBack(BLEControllerCallback BleControllerCallbackP) {
        BleControllerCallbackM = BleControllerCallbackP;
    }

    public BLEController(MtuCallback mtuCallback) {
        this.mtuCallback = mtuCallback;
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
            BleControllerCallbackM.ShowProgressMessage("Trying to Connect " + BluetoothDeviceL.getName() + "-" + BluetoothDeviceL.getAddress());
        }
        ConnectedGattM = BluetoothDeviceL.connectGatt(ParentContextM, true, GattCallbackM);
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

    public void sendDataChunkWise(String dataP) {
        Log.e("TAG", "SendDataChunkWise  " + dataP);

        byte[] imageBytes = dataP.getBytes();
        Log.e("TAG", "packet size  " + imageBytes.length);

        chunkQueue.clear();
        currentChunkIndex = 0;

        if (!IsConnectedM) {
            return;
        }
        if (ConnectedGattM == null) {
            return;
        }

        if (!isMtuSet) {
            if (PrefManager.getIntPref(ParentContextM, PrefManager.PREF_PACKET_SIZE) == 0) {
                REQUESTED_MTU = 20;
            } else {
                REQUESTED_MTU = PrefManager.getIntPref(ParentContextM, PrefManager.PREF_PACKET_SIZE);
            }
        }
        try {
            Log.e("TAG", "REQUESTED_MTU   " + REQUESTED_MTU);
            int totalChunks = (int) Math.ceil((double) imageBytes.length / REQUESTED_MTU);
            Log.e("TAG", "totalChunks   " + totalChunks);
            for (int i = 0; i < totalChunks; i++) {
                int startIndex = i * REQUESTED_MTU;
                int endIndex = Math.min(startIndex + REQUESTED_MTU, imageBytes.length);
                byte[] chunk = Arrays.copyOfRange(imageBytes, startIndex, endIndex);
                chunkQueue.add(chunk);
            }
            Log.e("TAG", " chunkQueue " + chunkQueue.size());
            // Start processing chunks
            sendNextChunk();
        } catch (Exception e) {
            Log.e("TAG", "Exception " + e);
        }
    }

    private void sendNextChunk() {
        if (chunkQueue.isEmpty() || isWriting) {
            return;
        }

        byte[] chunk = chunkQueue.poll();
        if (chunk == null) {
            return;
        }
        Log.e("TAG", "Sending chunk index: " + currentChunkIndex);
        isWriting = true;
        BluetoothGattCharacteristic writeCharacteristic = ConnectedGattM.getService(DATA_SERVICE).getCharacteristic(WRITE_CHARACTERSTIC);
        writeCharacteristic.setValue(chunk);
        boolean writeResult = ConnectedGattM.writeCharacteristic(writeCharacteristic);
        Log.e("TAG", "writeCharacteristic result: " + writeResult);
        currentChunkIndex++;

    }

    private BluetoothGattCallback GattCallbackM = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt BlueToothGattP, int StatusP, int newState) {
            Log.e("TAG", "onConnectionStateChange " + StatusP);
            Log.e("TAG", "onConnectionStateChange " + newState);
            if (StatusP == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                Log.e("TAG", "onConnectionStateChange 1111");

                BlueToothGattP.discoverServices();
                BlueToothGattP.requestMtu(517);

            } else if (StatusP == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.e("TAG", "onConnectionStateChange 2222");
                ConnectedGattM = null;
                IsConnectedM = false;
                BlueToothGattP.close();
                BleControllerCallbackM.DeviceIsDisconnected();
            } else if (StatusP != BluetoothGatt.GATT_SUCCESS) {
                Log.e("TAG", "onConnectionStateChange 3333");

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

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            Log.e("TAG", "MTU : " + mtu);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                isMtuSet = true;
                if (mtu == 517) {
                    REQUESTED_MTU = mtu - 5;
                } else {
                    REQUESTED_MTU = mtu - 3;
                }
                Log.e("TAG", "MTU size changed to: " + mtu);
                try {
                    Handler mainHandler = new Handler(Looper.getMainLooper());
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mtuCallback.mtuSize(mtu);
                        }
                    });
                } catch (Exception e) {
                    Log.e("TAG", "Exception MTU size  " + e);
                }
            } else {
                Log.e("TAG", "Failed to change MTU size");
            }
        }

        private void EnableNotifications(BluetoothGatt bluetoothGatt) {
            BluetoothGattCharacteristic CharacteristicL = bluetoothGatt.getService(DATA_SERVICE).getCharacteristic(NOTIFICATION_CHARACTERSTIC);
            if (CharacteristicL == null) {
                Log.e("TAG", "CharacteristicL is null ");
            } else {
                Log.e("TAG", "CharacteristicL is not null ");
            }
//            if (CharacteristicL != null) {
//                BleControllerCallbackM.ShowProgressMessage("Retrieved Notification Service.");
//            }
            //Enable local notifications
            bluetoothGatt.setCharacteristicNotification(CharacteristicL, true);
            //Enabled remote notifications
            BluetoothGattDescriptor desc = CharacteristicL.getDescriptor(CONFIG_DESCRIPTOR);
//            if (desc != null) {
//                BleControllerCallbackM.ShowProgressMessage("Retrieved Config Descriptor.");
//            }
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bluetoothGatt.writeDescriptor(desc);
//            Log.e("TAG", "====EnableNotifications== khatam== ");
        }


        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.e("TAG", "onCharacteristicWrite" + characteristic.getUuid());
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.e("TAG", "Characteristic write successful  " + characteristic.getValue().length);
//                Log.e("TAG", "PREF_IS_INTERVAL  " + PrefManager.getBoolPref(ParentContextM, PrefManager.PREF_IS_INTERVAL));
//                Log.e("TAG", "isImageSending  " + isImageSending);

                if (!PrefManager.getBoolPref(ParentContextM, PrefManager.PREF_IS_INTERVAL)) {
                    isWriting = false;
                    sendNextChunk();
                }

            }
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