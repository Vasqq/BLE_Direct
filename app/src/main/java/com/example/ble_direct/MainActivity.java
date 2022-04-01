package com.example.ble_direct;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTED = 2;
    private static final int BLUETOOTH_CONNECT_CODE = 112;
    private static final int BLUETOOTH_FINE_LOCATION_CODE = 113;


    private String deviceAddress = "B0:B1:13:76:0D:0B";

    private BluetoothGatt bluetoothGatt;
    public BluetoothGattCharacteristic bluetoothGattCharacteristic;
    public BluetoothGattService bluetoothGattService;


    public BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    public BluetoothDevice mBluetoothDevice = null;


    //private UUID serviceUUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9");
    private UUID serviceUUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");

    //private UUID receiveUUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    private UUID sendAndReceiveUUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");

    private UUID characteristicConfigUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    //public BluetoothGattCharacteristic gattService = new BluetoothGattCharacteristic(receiveUUID,);


    private int connectionState;

    private final BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                //Succesfully connected to GATT server

                Log.d(TAG, "Succesfully connected to GATT server.");
                connectionState = STATE_CONNECTED;

                checkBTconnectPermission(Manifest.permission.BLUETOOTH_CONNECT, BLUETOOTH_CONNECT_CODE);
                bluetoothGatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED)

                //Disconnected to GATT server
                connectionState = STATE_DISCONNECTED;


        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {

                for (BluetoothGattService gattService : gatt.getServices()) {
                    Log.i(TAG, "Service UUID Found: " + gattService.getUuid().toString());
                }

                checkBTconnectPermission(Manifest.permission.BLUETOOTH_CONNECT, BLUETOOTH_CONNECT_CODE);

                System.out.println(bluetoothGatt.getServices());
                bluetoothGattService = gatt.getService(serviceUUID);
                bluetoothGattCharacteristic = bluetoothGattService.getCharacteristic(sendAndReceiveUUID);
                bluetoothGattCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);

                // set characteristic configuration
                BluetoothGattDescriptor descriptor = bluetoothGattCharacteristic.getDescriptor(characteristicConfigUUID);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(descriptor);

            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
            byte[] dataByte;

            checkBTconnectPermission(Manifest.permission.BLUETOOTH_CONNECT, BLUETOOTH_CONNECT_CODE);

            bluetoothGatt.readCharacteristic(bluetoothGattCharacteristic);
            dataByte = bluetoothGattCharacteristic.getValue();

            String data = new String(dataByte);
            System.out.println(data);
        }

    };


    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);

                switch (state) {

                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "onReceive: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "onReceive: STATE ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "onReceive: STATE TURNING ON");
                        break;

                }

            }

        }
    };


    @Override
    protected void onDestroy() {
        Log.d(TAG, "OnDestroy called");
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver1);
        //unregisterReceiver(mBroadcastReceiver3);

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnONOFF = (Button) findViewById(R.id.btnONOFF);
        Button btnConnect = (Button) findViewById(R.id.btnConnect);
        Button btnGetData = (Button) findViewById(R.id.btnGetData);
        Button btnSenData = (Button) findViewById(R.id.btnSendData);


        //uuidReceive =  UUID.fromString("6e400002‑b5a3‑f393‑e0a9‑e50e24dcca9e");


        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {

            Log.d(TAG, "DEVICE DOES NOT SUPPORT BLE");


        }


        btnONOFF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Log.d(TAG, "OnClick: Enabling/disabling bluetooth");
                enableDisableBT();


            }
        });

        btnGetData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                byte[] dataByte;

                checkBTconnectPermission(Manifest.permission.BLUETOOTH_CONNECT, BLUETOOTH_CONNECT_CODE);

                bluetoothGatt.readCharacteristic(bluetoothGattCharacteristic);
                dataByte = bluetoothGattCharacteristic.getValue();

                String data = new String(dataByte);
                System.out.println(data);

            }
        });

        btnSenData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String data = "CLK\n";
                byte[] dataByte = data.getBytes();

                checkBTconnectPermission(Manifest.permission.BLUETOOTH_CONNECT, BLUETOOTH_CONNECT_CODE);

                boolean returned = bluetoothGattCharacteristic.setValue(dataByte);
                bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
                System.out.println(returned);

            }
        });


        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                checkFineLocationPermission(Manifest.permission.ACCESS_FINE_LOCATION, BLUETOOTH_FINE_LOCATION_CODE);

                Log.d(TAG, "OnClick: Connecting to GATT server");
                connect(deviceAddress);


            }
        });

    }


    public void enableDisableBT() {

        //DEVICE HAS NO BT ADAPTER
        if (mBluetoothAdapter == null) {

            Log.d(TAG, "EnableDisableBT: Does not have BT capabilities");


        }

        //ENABLE BT IF BT ADAPTER IS NOT ENABLED
        if (!mBluetoothAdapter.isEnabled()) {

            checkBTconnectPermission(Manifest.permission.BLUETOOTH_CONNECT, BLUETOOTH_CONNECT_CODE);

            Log.d(TAG, "EnableDisableBT: Enabling BT");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBTIntent);

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);


        }


        //DISABLE BT IF BT ADAPTER IS ARLEADY ENABLED
        if (mBluetoothAdapter.isEnabled()) {

            checkBTconnectPermission(Manifest.permission.BLUETOOTH_CONNECT, BLUETOOTH_CONNECT_CODE);

            Log.d(TAG, "EnableDisableBT: Disabling BT");


            mBluetoothAdapter.disable();

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }

    }

    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.d(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        Log.d(TAG, "connect: attempting to connect to GATT server.");

        try {
            final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

            //Connnect to the GATT server on the device.

            checkBTconnectPermission(Manifest.permission.BLUETOOTH_CONNECT, BLUETOOTH_CONNECT_CODE);
            bluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback);

            return true;


        } catch (IllegalArgumentException exception) {
            Log.d(TAG, "Device not found with provided address.");
            return false;
        }

    }



    public void onServicesDiscovered(BluetoothGatt gatt, int status) {

        if (status == BluetoothGatt.GATT_SUCCESS) {

        } else {
            Log.d(TAG, "onServicesDiscovered received " + status);
        }
    }


    //********************************************************************************************
    //CHECK
    //PERMISSIONS
    //FUNCTIONS
    //********************************************************************************************


    public void checkBTconnectPermission(String permission, int requestCode) {

        Log.d(TAG, "checkBTconnectPermission: CheckingPermissions...");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_DENIED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);

            Log.d(TAG, "checkBTconnectPermission: Permission granted");

            return;
        } else {

            Log.d(TAG, "checkBTconnectPermission: Permission already granted");
            //Permission already granted

        }


    }


    public void checkFineLocationPermission(String permission, int requestCode) {

        Log.d(TAG, "checkBTlocationPermission: CheckingPermissions...");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);


            Log.d(TAG, "checkBTlocationPermission: Permission granted");

            return;
        } else {

            Log.d(TAG, "checkBTlocationPermission: Permission already granted");
            //Permission already granted

        }


    }


}



