package com.example.bletest;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.core.app.ActivityCompat;

import java.util.UUID;

@SuppressLint("MissingPermission")
public class BLEManager {
    private static final String TAG = "BLEManager";

    // BLE相关常量，UART服务及其特征UUID
    private static final String UART_SERVICE_UUID = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E";
    private static final String UART_TX_CHARACTERISTIC_UUID = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E";
    private static final String UART_RX_CHARACTERISTIC_UUID = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E";
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805F9B34FB");

    private static final int MAX_RECONNECT_ATTEMPTS = 3; // 最大重连次数
    private int reconnectAttempts = 0; // 当前重连尝试次数

    private final Context context;
    private final BluetoothCallback callback;
    private final BluetoothAdapter bluetoothAdapter;
    private final BluetoothLeScanner bleScanner;
    private final Handler handler = new Handler();

    public BluetoothGatt bluetoothGatt;
    private boolean isScanning = false;
    private BluetoothDevice targetDevice;
    public BluetoothGattCharacteristic txCharacteristic;
    private BluetoothGattCharacteristic rxCharacteristic;

    // 定义蓝牙回调接口
    public interface BluetoothCallback {
        void onDataReceived(String data); // 数据接收回调
        void onDeviceConnected(String deviceName); // 设备连接回调
        void onDeviceDisconnected(); // 设备断开回调
        void onServicesDiscovered(String serviceInfo); // 服务发现回调
        void onError(String errorMessage); // 错误回调

        void onScanHasResult(String deviceName);
    }

    // 构造函数，初始化BLE管理器
    public BLEManager(Context context, BluetoothCallback callback) {
        this.context = context;
        this.callback = callback;

        final android.bluetooth.BluetoothManager bluetoothManager =
                (android.bluetooth.BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager.getAdapter();
        this.bleScanner = bluetoothAdapter.getBluetoothLeScanner();
    }

    /**
     * 启动蓝牙扫描
     */
    public void startScanning() {
        // 检查蓝牙是否开启
        if (!isBluetoothEnabled()) {
            callback.onError("蓝牙未开启");
            return;
        }

        // 检查是否有蓝牙权限
        if (!hasPermissions()) {
            callback.onError("缺少蓝牙权限");
            return;
        }

        // 开始扫描
        isScanning = true;
        handler.postDelayed(this::stopScanning, 15000); // 15秒后自动停止扫描
        bleScanner.startScan(scanCallback);
    }

    /**
     * 停止蓝牙扫描
     */
    public void stopScanning() {
        if (!isScanning) return;

        isScanning = false;
        bleScanner.stopScan(scanCallback);
    }

    /**
     * 连接到目标设备
     */
    @SuppressLint("MissingPermission")
    public void connectToDevice() {
        // 1. 检查目标设备是否存在
        if (targetDevice == null) return;

        // 2. 清理现有连接
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
        }

        // 3. 建立新连接（兼容所有Android版本）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            bluetoothGatt = targetDevice.connectGatt(
                    context,
                    false,
                    gattCallback,
                    BluetoothDevice.TRANSPORT_LE
            );
        } else {
            bluetoothGatt = targetDevice.connectGatt(
                    context,
                    false,
                    gattCallback
            );
        }
    }

    /**
     * 断开与设备的连接
     */
    public void disconnectDevice() {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                bluetoothGatt.close();
                bluetoothGatt = null;
                callback.onDeviceDisconnected();
                startReconnect(); // 断开后启动重连
            }, 1000);  // 延时1秒后执行关闭
        }
    }

    /**
     * 读取数据
     */
    public void readData() {
        if (bluetoothGatt == null || txCharacteristic == null) {
            callback.onError("未连接或特征不可用");
            return;
        }

        if (!hasPermissions()) {
            callback.onError("缺少蓝牙权限");
            return;
        }

        bluetoothGatt.readCharacteristic(txCharacteristic);
    }

    /**
     * 获取当前是否正在扫描
     *
     * @return 是否正在扫描
     */
    public boolean isScanning() {
        return isScanning;
    }

    /**
     * 获取当前是否已连接到设备
     *
     * @return 是否已连接
     */
    public boolean isConnected() {
        return bluetoothGatt != null;
    }

    /**
     * 检查蓝牙是否开启
     *
     * @return 蓝牙是否开启
     */
    public boolean isBluetoothEnabled() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    /**
     * 检查应用是否具有蓝牙相关的权限
     *
     * @return 是否具有权限
     */
    private boolean hasPermissions() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * 蓝牙扫描回调，处理扫描结果
     */
    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            String deviceName = device.getName();

            // 判断是否是目标设备
            if (deviceName != null && (deviceName.contains("nRF") || deviceName.contains("UART"))) {
                targetDevice = device;
                stopScanning();
                callback.onScanHasResult(targetDevice.getName());
//                connectToDevice();
//                callback.onDeviceConnected(deviceName);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            isScanning = false;
            callback.onError("扫描失败，错误码: " + errorCode);
        }
    };

    /**
     * 蓝牙Gatt回调，处理连接、服务发现、数据接收等事件
     */
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
                callback.onDeviceConnected(targetDevice.getName());
                reconnectAttempts = 0; // 连接成功后重置重连次数
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "设备已断开连接");
                callback.onDeviceDisconnected();
                startReconnect(); // 设备断开后尝试重连
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // 获取UART服务及其特征
                BluetoothGattService uartService = gatt.getService(UUID.fromString(UART_SERVICE_UUID));
                if (uartService != null) {
                    txCharacteristic = uartService.getCharacteristic(UUID.fromString(UART_TX_CHARACTERISTIC_UUID));
                    rxCharacteristic = uartService.getCharacteristic(UUID.fromString(UART_RX_CHARACTERISTIC_UUID));

                    // 启用通知
                    if (txCharacteristic != null) {
                        gatt.setCharacteristicNotification(txCharacteristic, true);
                        BluetoothGattDescriptor descriptor = txCharacteristic.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
                        if (descriptor != null) {
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            gatt.writeDescriptor(descriptor);
                        }
                    }
                }

                // 报告发现的服务
                StringBuilder servicesInfo = new StringBuilder("发现的服务:\n");
                for (BluetoothGattService service : gatt.getServices()) {
                    servicesInfo.append("• ").append(service.getUuid()).append("\n");
                }
                callback.onServicesDiscovered(servicesInfo.toString());
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            byte[] data = characteristic.getValue();
//            Log.e("rawraw data:" , String.valueOf(data));
            if (data != null && data.length > 0) {
                callback.onDataReceived(new String(data)); // 处理数据接收
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            byte[] data = characteristic.getValue();
            Log.e("ssss",data.toString());// 处理数据读取sss
            if (data != null) {
                callback.onDataReceived(new String(data));

            }
        }
    };

    /**
     * 自动重连逻辑
     * 如果设备断开连接，会尝试重新连接
     */
    private void startReconnect() {
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++;
            Log.d(TAG, "尝试重连，重连次数：" + reconnectAttempts);

            // 延时一段时间后重连
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (targetDevice != null && !isConnected()) {
                    Log.d(TAG, "自动重连...");
                    connectToDevice();
                }
            }, 5000); // 5秒后自动重连
        } else {
            callback.onError("最大重连次数已达到，无法重连。");
        }
    }
}
