package com.bonrix.dynamicqrcode;

import android.bluetooth.BluetoothDevice;

import java.util.HashMap;

/**
 * Created by Aalok on 3/10/2017.
 */
public interface BLEControllerCallback
{
    void OnBleScanComplete(HashMap<String, BluetoothDevice> DevicesP);
    void ShowProgressMessage(String MessageP);
    void DeviceIsDisconnected();
    void DeviceIsconnected();
    void ErrorsOccured(String ErrorsP);
    void NotificationReceived(byte[] NotificationDataP);
}

