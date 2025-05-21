package com.bonrix.dynamicqrcode;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class BleActivity extends AppCompatActivity implements View.OnClickListener, BLEControllerCallback,MtuCallback {
    String TAG = "BleActivity";
    Toolbar toolbar;
    ImageView backarrow;
    private Button btnScan, btn_start, btnWelcome, btnGenerateQr, btnSuccess, btnFail, btnPending;
    private TextView receiveText;
    static Activity activity;
    LinearLayout lineScan, line_operation;

    @Override
    public void mtuSize(int mtu) {

    }

    private enum Connected {False, Pending, True}

    private Connected connected = Connected.False;
    BLEController bleController;
    ListView DevicesListM;
    private ProgressDialog progressDialogM;
    boolean bScaleControlActivityIsLaunchedM = false;
    private static final int MSG_PROGRESS = 201;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            BluetoothGattCharacteristic characteristic;
            switch (msg.what) {
                case MSG_PROGRESS:
                    String TraceMsgL = (String) msg.obj;
                    ShowProgressMessageInternal(TraceMsgL);
                    break;

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_ble);
        Log.e(TAG, "===onCreate=====");
        initComponent();
        checkBtConnection();
        checkAndRequestPermissions();
    }


    private void initComponent() {
        activity = this;
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .permitAll().build();
        StrictMode.setThreadPolicy(policy);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        backarrow = findViewById(R.id.backarrow);
        line_operation = findViewById(R.id.line_operation);
        lineScan = findViewById(R.id.lineScan);

        receiveText = findViewById(R.id.tv_bt_status);
        btnWelcome = findViewById(R.id.btnWelcome);
        btnScan = findViewById(R.id.btnScan);

        btnGenerateQr = findViewById(R.id.btnGenerateQr);
        btnWelcome = findViewById(R.id.btnWelcome);
        btnSuccess = findViewById(R.id.btnSuccess);
        btnFail = findViewById(R.id.btnFail);
        btnPending = findViewById(R.id.btnPending);

        bleController = new BLEController(this);
        bleController.SetContext(BleActivity.this);
        bleController.Initialize();
        btnScan = findViewById(R.id.btnScan);
        DevicesListM = findViewById(R.id.deviceslist);
        String[] Devices = {""};

        ArrayAdapter<String> AdapterL = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, Devices);
        DevicesListM.setAdapter(AdapterL);

        DevicesListM.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                String DeviceNameL = ((TextView) view).getText().toString();
                bleController.ConnectToDevice(DeviceNameL, true);
            }
        });

        backarrow.setOnClickListener(this);
        btnWelcome.setOnClickListener(this);
        btnScan.setOnClickListener(this);
        btnGenerateQr.setOnClickListener(this);
        btnFail.setOnClickListener(this);
        btnPending.setOnClickListener(this);
        btnSuccess.setOnClickListener(this);

    }

    private void checkBtConnection() {
        Log.e("TAG", "connected  " + connected);
        if (connected == Connected.False) {
            receiveText.setText("Device not Connected");
            lineScan.setVisibility(View.VISIBLE);
            line_operation.setVisibility(View.GONE);
        } else if (connected == Connected.True) {
            receiveText.setText("Device Connected: " + bleController.ConnectedDeviceNameM + "(" + bleController.ConnectedDeviceAddress + ")");
            lineScan.setVisibility(View.GONE);
            line_operation.setVisibility(View.VISIBLE);
        }
    }

    void ShowProgressMessageInternal(String MessageP) {
        if (progressDialogM == null) {
            return;
        }
        progressDialogM.setMessage(MessageP);
        if (!progressDialogM.isShowing()) {
            progressDialogM.show();
        }
    }

    @Override
    public void OnBleScanComplete(HashMap<String, BluetoothDevice> DevicesP) {
        Log.e("TAG", "OnBleScanComplete     " + DevicesP.size());
        progressDialogM.hide();
        if (DevicesP.size() == 0) {
            String MsgL = "No device Found!";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!isLocationEnabled()) {
                    MsgL += " Your device may require you to turn on GPS for bluetooth scanning.";
                }
            }
            Toast.makeText(activity, MsgL, Toast.LENGTH_SHORT).show();

            String[] Devices = {""};

            ArrayAdapter<String> AdapterL = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, Devices);
            DevicesListM.setAdapter(AdapterL);

            Log.e("TAG", "Devices  " + Devices.length);
        } else {
            Toast.makeText(activity, "Please select one device below to connect", Toast.LENGTH_SHORT).show();
            String[] DeviceNamesL = DevicesP.keySet().toArray(new String[DevicesP.keySet().size()]);
            ArrayAdapter<String> AdapterL = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, DeviceNamesL);
            DevicesListM.setAdapter(AdapterL);

            Bundle bundle = getIntent().getExtras();
            if (bundle != null) {
                for (int i = 0; i < DeviceNamesL.length; i++) {
                    String temp = DeviceNamesL[i];
//                    if (temp.equalsIgnoreCase(SharedPrefrenceManager.getString(DeviceScanActivity.this, SharedPrefrenceManager.DEVICENAME))) {
//                        BLEController TheBleControllerL = SetupBluetoothWeightScale.BLEControllerM;
//                        TheBleControllerL.ConnectToDevice(temp, true);
//                    }
                }
            }
        }
    }

    public boolean isLocationEnabled() {
        int locationMode = 0;
        try {
            locationMode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);
        } catch (Settings.SettingNotFoundException e) {
            //e.printStackTrace();
        }
        return locationMode != Settings.Secure.LOCATION_MODE_OFF;
    }

    @Override
    public void ShowProgressMessage(String MessageP) {
        mHandler.sendMessage(Message.obtain(null, MSG_PROGRESS, MessageP));


    }

    @Override
    public void DeviceIsDisconnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connected = Connected.False;
                Toast.makeText(BleActivity.this, "Opps Bluetooth disconnected", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void DeviceIsconnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connected = Connected.True;
                progressDialogM.hide();
                line_operation.setVisibility(View.VISIBLE);
                lineScan.setVisibility(View.GONE);
                checkBtConnection();

            }
        });

    }

    @Override
    public void ErrorsOccured(String ErrorsP) {
        connected = Connected.False;
    }

    @Override
    public void NotificationReceived(byte[] NotificationDataP) {
        connected = Connected.True;
        if (bScaleControlActivityIsLaunchedM) {
            return;
        }
        progressDialogM.hide();
        bScaleControlActivityIsLaunchedM = true;
//        Intent intent = new Intent(this, BleActivity.class);
//        startActivity(intent);
    }


    @Override
    public void onClick(View view) {
        if (view == backarrow) {
            finish();
        }
        if (view == btnScan) {

            if (!areAllBTPermissionsGranted()) {
                checkAndRequestPermissions();
                return;
            }
            if (!areAllLocationPermissionsGranted()) {
                return;
            }
            connected = Connected.False;
            checkBtConnection();
            progressDialogM = new ProgressDialog(BleActivity.this);
            progressDialogM.setIndeterminate(true);
            progressDialogM.setCancelable(false);
            bScaleControlActivityIsLaunchedM = false;
            if (bleController != null) {
                bleController.Disconnect();
                bleController.SetCallBack(BleActivity.this);
                bleController.StartScan(true, 5);
            }

        }

        if (view == btnWelcome) {

            if (connected == Connected.True) {
                try {
                    bleController.SendData(Constants.WELCOME_SCREEN);
                } catch (Exception e) {
                    Log.e("TAG", "Exception   " + e);
                }
            }

        }
        if (view == btnGenerateQr) {

            if (connected == Connected.True) {
                try {
                    bleController.SendData(Constants.QR_SCREEN);
                } catch (Exception e) {
                    Log.e("TAG", "Exception   " + e);
                }
            }

        }
        if (view == btnSuccess) {

            if (connected == Connected.True) {
                try {
                    bleController.SendData(Constants.SUCCESS_SCREEN
                            .replace("<bankreff>", "31231231")
                            .replace("<orderid>", "ord231231")
                            .replace("<date>", "02-08-2024"));
                } catch (Exception e) {
                    Log.e("TAG", "Exception   " + e);
                }
            }

        }
        if (view == btnFail) {

            if (connected == Connected.True) {
                try {
                    bleController.SendData(Constants.FAIL_SCREEN
                            .replace("<bankreff>", "31231231")
                            .replace("<orderid>", "ord231231")
                            .replace("<date>", "02-08-2024"));
                } catch (Exception e) {
                    Log.e("TAG", "Exception   " + e);
                }
            }

        }
        if (view == btnPending) {

            if (connected == Connected.True) {
                try {
                    bleController.SendData(Constants.CANCEL_SCREEN
                            .replace("<bankreff>", "31231231")
                            .replace("<orderid>", "ord231231")
                            .replace("<date>", "02-08-2024"));
                } catch (Exception e) {
                    Log.e("TAG", "Exception   " + e);
                }
            }

        }
    }


//    private void displayTxnQr() {
//        String orderid = Apputils.getCurrnetDateTime2();
//        String upistring = Apputils.getUpiString(PrefManager.getPref(activity, PrefManager.PREF_UPIID).trim(), PrefManager.getPref(activity, PrefManager.PREF_PAYEENAME).trim().replace(" ", "%20"), etAmount.getText().toString(), orderid);
//        if (TextUtils.isEmpty(upistring)) {
//            Toast.makeText(activity, "Invalid UPI Data", Toast.LENGTH_SHORT).show();
//        } else {
//            try {
//                WindowManager manager = (WindowManager) activity.getSystemService(WINDOW_SERVICE);
//                Display display = manager.getDefaultDisplay();
//                Point point = new Point();
//                display.getSize(point);
//                int width = 280;
//                int height = 280;
//                int smallerDimension = width < height ? width : height;
//                smallerDimension = smallerDimension * 3 / 4;
//                Log.e("TAG", "smallerDimension  " + smallerDimension);
//
//                qrgEncoder = new QRGEncoder(upistring, null, QRGContents.Type.TEXT, 280);
//                Bitmap bitmap_qr = qrgEncoder.encodeAsBitmap();
//
//                if (connected == Connected.True) {
//                    try {
//                        bleController.SendData(Apputils.getUpiString2(PrefManager.getPref(this, PrefManager.PREF_UPIID), PrefManager.getPref(this, PrefManager.PREF_PAYEENAME), etAmount.getText().toString(), ""));
//                    } catch (Exception e) {
//                        Log.e("TAG", "Exception   " + e);
//                    }
//                }
//                try {
//                    if (gcmMessageDataSource == null) {
//                        gcmMessageDataSource = new GcmMessageDataSource(activity);
//                        gcmMessageDataSource.open();
//                    }
//                    Log.e("TAG", "getCurrnetDateTime   " + Apputils.getCurrnetDateTime());
//                    gcmMessageDataSource.saveTransaction(upistring, Apputils.getCurrnetDateTime(), "pending", etAmount.getText().toString(), orderid);
//                } catch (Exception e) {
//                    Log.e("TAG", "DB Exception   " + e);
//                }
//                dialogQr(bitmap_qr, orderid);
//            } catch (Exception e) {
//                Log.e("TAG", "Exception  " + e);
//                e.printStackTrace();
//            }
//        }
//    }

//    private void dialogQr(Bitmap rotate_bitmap, String orderid) {
//        Dialog viewDialog112 = new Dialog(BleActivity.this);
//        viewDialog112.getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND, WindowManager.LayoutParams.FLAG_DIM_BEHIND);
//        viewDialog112.requestWindowFeature(Window.FEATURE_NO_TITLE);
//        LayoutInflater lin1 = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        View dialogView112 = lin1.inflate(R.layout.dialog_qr, null);
//        viewDialog112.setContentView(dialogView112);
//        viewDialog112.setCancelable(false);
//        viewDialog112.getWindow().setLayout(android.view.ViewGroup.LayoutParams.FILL_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
//        viewDialog112.show();
//        ImageView iv_close = dialogView112.findViewById(R.id.iv_close);
//        ImageView imageView = dialogView112.findViewById(R.id.imageview);
//        Button btnSuccess = dialogView112.findViewById(R.id.btnSuccess);
//        Button btnpending = dialogView112.findViewById(R.id.btnpending);
//        Button btnFail = dialogView112.findViewById(R.id.btnFail);
//        TextView tv_amount = dialogView112.findViewById(R.id.tv_amount);
//        tv_amount.setText("Amount: \u20B9" + etAmount.getText());
//        imageView.setImageBitmap(rotate_bitmap);
//        iv_close.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                viewDialog112.dismiss();
//                etAmount.setText("");
//            }
//        });
//        btnSuccess.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                viewDialog112.dismiss();
//                etAmount.setText("");
//                try {
//                    if (gcmMessageDataSource == null) {
//                        gcmMessageDataSource = new GcmMessageDataSource(BleActivity.this);
//                        gcmMessageDataSource.open();
//                    }
//                    String id = gcmMessageDataSource.getSingleColumnId(orderid);
//                    gcmMessageDataSource.updateTransactionValue(Integer.parseInt(id), "success");
//                } catch (Exception e) {
//                    Log.e("TAG", "DB Exception update   " + e);
//                }
//                Toast.makeText(BleActivity.this, "Transaction SuccessFull.", Toast.LENGTH_SHORT).show();
//                try {
//
//                    if (connected == Connected.True) {
//                        try {
//                            bleController.SendData(Constants.SUCCESS_SCREEN
//                                    .replace("<bankreff>", orderid)
//                                    .replace("<orderid>", orderid)
//                                    .replace("<date>", Apputils.getCurrnetDate()));
//                        } catch (Exception e) {
//                            Log.e("TAG", "Exception   " + e);
//                        }
//                    }
//                } catch (Exception e) {
//                    Log.e("TAG", "Exception   " + e);
//                }
//            }
//        });
//        btnFail.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                viewDialog112.dismiss();
//                etAmount.setText("");
//                try {
//                    if (gcmMessageDataSource == null) {
//                        gcmMessageDataSource = new GcmMessageDataSource(activity);
//                        gcmMessageDataSource.open();
//                    }
//                    String id = gcmMessageDataSource.getSingleColumnId(orderid);
//                    gcmMessageDataSource.updateTransactionValue(Integer.parseInt(id), "fail");
//                } catch (Exception e) {
//                    Log.e("TAG", "DB Exception update   " + e);
//                }
//                Toast.makeText(activity, "Transaction Failed.", Toast.LENGTH_SHORT).show();
//                try {
//                    if (connected == Connected.True) {
//                        try {
//                            bleController.SendData(Constants.FAIL_SCREEN
//                                    .replace("<bankreff>", orderid)
//                                    .replace("<orderid>", orderid)
//                                    .replace("<date>", Apputils.getCurrnetDate()));
//                        } catch (Exception e) {
//                            Log.e("TAG", "Exception   " + e);
//                        }
//                    }
//                } catch (Exception e) {
//                    Log.e("TAG", "Exception   " + e);
//                }
//
//            }
//        });
//        btnpending.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                viewDialog112.dismiss();
//                etAmount.setText("");
//                try {
//                    if (gcmMessageDataSource == null) {
//                        gcmMessageDataSource = new GcmMessageDataSource(activity);
//                        gcmMessageDataSource.open();
//                    }
//                    String id = gcmMessageDataSource.getSingleColumnId(orderid);
//                    gcmMessageDataSource.updateTransactionValue(Integer.parseInt(id), "pending");
//                } catch (Exception e) {
//                    Log.e("TAG", "DB Exception update   " + e);
//                }
//                Toast.makeText(activity, "Transaction Pending.", Toast.LENGTH_SHORT).show();
//                try {
//
//                    if (connected == Connected.True) {
//                        try {
//                            bleController.SendData(Constants.CANCEL_SCREEN
//                                    .replace("<bankreff>", orderid)
//                                    .replace("<orderid>", orderid)
//                                    .replace("<date>", Apputils.getCurrnetDate()));
//                        } catch (Exception e) {
//                            Log.e("TAG", "Exception   " + e);
//                        }
//                    }
//                } catch (Exception e) {
//                    Log.e("TAG", "Exception   " + e);
//                }
//            }
//        });
//    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void onStart() {
        super.onStart();


    }

    @Override
    public void onResume() {
        super.onResume();
//        checkBtConnection();
    }

    @Override
    protected void onPause() {
        super.onPause();
        progressDialogM.dismiss();
        if (!bScaleControlActivityIsLaunchedM) {
            bleController.Disconnect();
            bleController.StopScan();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (progressDialogM != null) {
            progressDialogM.dismiss();
            progressDialogM = null;
        }
    }

    private void checkAndRequestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        String[] bluetoothPermissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothPermissions = new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
            };
        } else {
            bluetoothPermissions = new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
            };
        }

        for (String permission : bluetoothPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        String[] locationPermissions = new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        for (String permission : locationPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    // Handle the case where the permission was denied
                    // You can show a dialog to inform the user why the permission is important
                }
            }
        }
    }

    private boolean areAllBTPermissionsGranted() {
        for (String permission : getBluetoothPermissions()) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
    }

    private boolean areAllLocationPermissionsGranted() {

        for (String permission : locationPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
    }

    private String[] getBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
            };
        } else {
            return new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN
            };
        }
    }

    private String[] locationPermissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };
}



