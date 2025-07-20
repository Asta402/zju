package com.example.bletest;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements BLEManager.BluetoothCallback {

    // 定义界面上的按钮和文本视图
    private Button btnScan, btnConnect, btnDisconnect, btnPermissions, btnReadData, btnSendTest, btnClearData, btnSaveData;
    private TextView tvStatus, tvData, tvDeviceName, tvPermissionStatus, tvServiceInfo, tvParsedData;

    // 创建ExecutorService来管理后台线程
    private ExecutorService executorService;

    // 定义蓝牙管理器、权限管理器和数据解析器
    private BLEManager bleManager;
    private PermissionManager permissionManager;
    private DataParser dataParser;

    // 设备连接状态和数据保存状态标志
    private boolean isConnected = false;
    private long saveStartTime; // 记录开始保存的时间戳
    private boolean isSavingData = false; // 标记是否正在保存数据

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

        // 初始化线程池
        executorService = Executors.newSingleThreadExecutor();
    }

    // 初始化管理器
    private void initComponents() {
        permissionManager = new PermissionManager(this);
        bleManager = new BLEManager(this, this);
        dataParser = new DataParser(this.tvParsedData, new DataParser.DataDisplayCallback() {
            @Override
            public void onDataParsed(SensorData data) {
                if (dataParser.isSavingNoiseData()) {
                    dataParser.setSavingNoiseData(true);
                }
            }

            @Override
            public void onParseError(String rawData, String errorMessage) {
                // 处理解析错误
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
            dataParser.setSavingNoiseData(true);  // 开始保存噪声数据
        });

        btnDisconnect.setOnClickListener(v -> bleManager.disconnectDevice());
        btnPermissions.setOnClickListener(v -> permissionManager.requestBluetoothPermissions());
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

    // 将保存数据的操作移到后台线程
    private void saveDataForTwoSeconds() {
        // 停止噪声数据，开始正样本数据保存
        dataParser.setSavingNoiseData(false);
        dataParser.setSavingData(true);
        isSavingData = true; // 标记为正在保存
        saveStartTime = System.currentTimeMillis(); // 记录开始时间

        Log.d("SaveData", "开始保存正样本数据，持续2秒");
        tvStatus.setText("正在保存正样本数据（2秒）...");

        // 不需要显式延迟，数据回调中会自动检查时间
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
    private void updateDisplay(SensorData data) {
        if(data!=null){
            String displayText = String.format(
                    "时间戳: %d\n索引: %d\nX: %.3f\nY: %.3f\nZ: %.3f\nT: %.3f",
                    data.timestamp, data.index, data.x, data.y, data.z, data.t
            );
            tvParsedData.setText(displayText); // 更新 TextView
        }

    }
    @Override
    public void onDataReceived(String data) {
        long currentTime = System.currentTimeMillis();

        // 检查是否正在保存数据，且已超过2秒
        if (isSavingData && (currentTime - saveStartTime >= 2000)) {
            dataParser.setSavingData(false); // 停止正样本
            dataParser.setSavingNoiseData(true); // 恢复噪声数据
            isSavingData = false; // 重置标记

            runOnUiThread(() -> {
                tvStatus.setText("噪声数据继续采集");
                Toast.makeText(this, "正样本数据已保存2秒", Toast.LENGTH_SHORT).show();
            });
            Log.d("SaveData", "正样本数据保存结束，恢复噪声数据");
        }

        // 正常处理数据
        dataParser.processIncomingData(data);

        runOnUiThread(()->{
            updateDisplay(dataParser.data);
        });
//        addLogMessage("接收数据: " + data);
    }
    @Override
    public void onScanHasResult(String devicename) {
        runOnUiThread(() -> tvDeviceName.setText(devicename));
    }

    @Override
    public void onDeviceConnected(String deviceName) {
        isConnected = true;
        runOnUiThread(() -> {
            tvDeviceName.setText("已连接: " + deviceName);
            updateUI();
            addLogMessage("设备已连接: " + deviceName);
        });
    }

    @Override
    public void onDeviceDisconnected() {
        isConnected = false;
        runOnUiThread(() -> {
            tvDeviceName.setText("未连接设备");
            updateUI();
            addLogMessage("设备已断开");
        });
    }

    @Override
    public void onServicesDiscovered(String serviceInfo) {
        runOnUiThread(() -> {
            tvServiceInfo.setText(serviceInfo);
            addLogMessage("发现服务:\n" + serviceInfo);
        });
    }

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
            if (existingData.startsWith("原始数据日志: 等待连接...") || existingData.startsWith("原始数据日志: 已清空")) {
                tvData.setText("原始数据日志:\n" + newData);
            } else {
                tvData.setText(existingData + "\n" + newData);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bleManager.disconnectDevice();
        if (executorService != null) {
            executorService.shutdown();  // 关闭线程池
        }
    }
}
