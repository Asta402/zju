package com.example.bletest;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Handler;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class MainActivity extends AppCompatActivity implements BLEManager.BluetoothCallback {

    // 定义界面上的按钮和文本视图
    private Button btnScan, btnConnect, btnDisconnect, btnPermissions, btnReadData, btnSendTest, btnClearData, btnSaveData;
    private TextView tvStatus, tvData, tvDeviceName, tvPermissionStatus, tvServiceInfo, tvParsedData;

    public ExecutorService executorService = null;
    // 定义蓝牙管理器、权限管理器和数据解析器
    private BLEManager bleManager;
    private PermissionManager permissionManager;
    private DataParser dataParser;

    // 设备连接状态和数据保存状态标志
    private boolean isConnected = false;


    // 初始化视图组件
    private void initViews() {
        btnScan = findViewById(R.id.btn_scan);
        btnConnect = findViewById(R.id.btn_connect);
        btnDisconnect = findViewById(R.id.btn_disconnect);
        btnPermissions = findViewById(R.id.btn_permissions);
        btnReadData = findViewById(R.id.btn_read_data);
        btnSendTest = findViewById(R.id.btn_send_test);
        btnClearData = findViewById(R.id.btn_clear_data);
        btnSaveData = findViewById(R.id.btn_save_data);
        tvStatus = findViewById(R.id.tv_status);
        tvData = findViewById(R.id.tv_data);
        tvDeviceName = findViewById(R.id.tv_device_name);
        tvPermissionStatus = findViewById(R.id.tv_permission_status);
        tvServiceInfo = findViewById(R.id.tv_service_info);
        tvParsedData = findViewById(R.id.tv_parsed_data);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化视图和组件
        initViews();
        Log.e("sss", "布局初始化成功");
        initComponents();
        Log.e("sss", "组件初始化成功");

        // 设置按钮点击事件监听器
        setupClickListeners();
        Log.e("sss", "事件绑定成功");

        // 更新UI
        updateUI();
        executorService = Executors.newSingleThreadExecutor();
    }

    // 初始化管理器
    private void initComponents() {
        permissionManager = new PermissionManager(this);
        bleManager = new BLEManager(this, this);
        dataParser = new DataParser(this.tvParsedData, new DataParser.DataDisplayCallback() {
            @Override
            public void onDataParsed(SensorData data) {

                if(dataParser.isSavingNoiseData()){
                    dataParser.setSavingNoiseData(true);
                }
            }

            @Override
            public void onParseError(String rawData, String errorMessage) {
//                Log.e("处理错误:", errorMessage);
            }
        });
    }

    // 设置按钮点击事件监听器
    private void setupClickListeners() {
        btnScan.setOnClickListener(v -> {
            if (bleManager.isScanning()) {
                bleManager.stopScanning();
                btnScan.setText("开始扫描");
            } else {
                bleManager.startScanning();
                btnScan.setText("停止扫描");
            }
        });

        btnConnect.setOnClickListener(v -> {
            bleManager.connectToDevice();  // 连接设备

            // 开始保存噪声数据
            dataParser.setSavingNoiseData(true);  // 开始保存噪声数据

            String noiseFileName = "noise_data_" + System.currentTimeMillis() + ".csv";  // 文件名基于当前时间戳
            dataParser.setCurrentFileName(noiseFileName);  // 设置新的文件名

        });


        btnDisconnect.setOnClickListener(v -> bleManager.disconnectDevice());
        btnPermissions.setOnClickListener(v -> permissionManager.requestBluetoothPermissions());
        btnReadData.setOnClickListener(v -> bleManager.readData());
//        btnSendTest.setOnClickListener(v -> bleManager.sendData("TEST"));
        btnClearData.setOnClickListener(v -> clearAllData());
        btnSaveData.setOnClickListener(v -> saveDataForTwoSeconds());
    }

    // 清空数据
    private void clearAllData() {
        dataParser.clearData();
        tvData.setText("原始数据日志: 已清空");
        tvParsedData.setText("解析的传感器数据: 已清空");
        Toast.makeText(this, "数据已清空", Toast.LENGTH_SHORT).show();
    }

    // 保存数据两秒
// 保存数据两秒并保存到新文件
    private void saveDataForTwoSeconds() {
        dataParser.setSavingNoiseData(false);  // 暂时停止保存噪声数据
        // 生成新的文件名，基于当前时间戳
        String currentFileName = "sensor_data_" + System.currentTimeMillis() + ".csv";

        // 设置标志位为 true，开始保存数据
        dataParser.setSavingData(true);
        dataParser.setCurrentFileName(currentFileName);  // 设置新的文件名

        // 两秒后将标志位设为 false，停止保存
        final Handler handler = new Handler();

        // 设置两秒后停止保存
        handler.postDelayed(() -> {
            dataParser.setSavingData(false); // 停止保存正样本数据
            Log.d("SaveData", "正样本数据保存结束");

            // 两秒后继续采集噪声数据
            dataParser.setSavingNoiseData(true);  // 重新开始保存噪声数据
            Log.d("SaveData", "开始保存噪声数据");
        }, 2000);
    }


    // 更新UI界面
    private void updateUI() {
        boolean hasPermissions = permissionManager.hasAllBluetoothPermissions();
        boolean bluetoothEnabled = bleManager.isBluetoothEnabled();

        btnScan.setEnabled(hasPermissions && bluetoothEnabled);
        btnConnect.setEnabled(!isConnected && hasPermissions);
        btnDisconnect.setEnabled(isConnected);
        btnReadData.setEnabled(isConnected);
        btnSendTest.setEnabled(isConnected);
        btnPermissions.setVisibility(hasPermissions ? View.GONE : View.VISIBLE);

        if (!hasPermissions) {
            tvStatus.setText("请先授予蓝牙权限");
        } else if (!bluetoothEnabled) {
            tvStatus.setText("请启用蓝牙");
        } else {
            tvPermissionStatus.setText("已获取权限");
            tvStatus.setText(isConnected ? "已连接" : "准备就绪");
        }
    }

    // BLEManager.BluetoothCallback 实现：接收到数据


    @Override
    public void onScanHasResult(String devicename){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvDeviceName.setText(devicename);
            }
        });
    }
    @Override
    public void onDataReceived(String data) {
        dataParser.processIncomingData(data);
        addLogMessage("接收数据: " + data);
    }

    // 设备连接成功回调
    @Override
    public void onDeviceConnected(String deviceName) {
        isConnected = true;
        runOnUiThread(() -> {
            tvDeviceName.setText("已连接: " + deviceName);
            updateUI();
            addLogMessage("设备已连接: " + deviceName);
        });
    }

    // 设备断开连接回调


    @Override
    public void onDeviceDisconnected() {
        isConnected = false;
        runOnUiThread(() -> {
            tvDeviceName.setText("未连接设备");
            updateUI();
            addLogMessage("设备已断开");
        });
    }
    // 服务发现回调
    @Override
    public void onServicesDiscovered(String serviceInfo) {
        runOnUiThread(() -> {
            tvServiceInfo.setText(serviceInfo);
            addLogMessage("发现服务:\n" + serviceInfo);
        });
    }

    // 错误回调
    @Override
    public void onError(String errorMessage) {
        runOnUiThread(() -> {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
            addLogMessage("错误: " + errorMessage);
        });
    }

    // 添加日志信息
    private void addLogMessage(String message) {
        runOnUiThread(() -> {
            String currentTime = java.text.DateFormat.getTimeInstance().format(new java.util.Date());
            String newData = "[" + currentTime + "] " + message;

            String existingData = tvData.getText().toString();
            if (existingData.startsWith("原始数据日志: 等待连接...") ||
                    existingData.startsWith("原始数据日志: 已清空")) {
                tvData.setText("原始数据日志:\n" + newData);
            } else {
                tvData.setText(existingData + "\n" + newData);
            }
        });
    }

    // Activity销毁时断开蓝牙连接
    @Override
    protected void onDestroy() {
        super.onDestroy();
        bleManager.disconnectDevice();
    }
}