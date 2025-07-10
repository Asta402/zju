# BLE Test - Bluetooth Low Energy (BLE) Manager

## 项目概述

该项目实现了一个 Bluetooth Low Energy (BLE) 管理器，用于在 Android 设备上扫描、连接、读取和处理蓝牙设备数据。它包括蓝牙设备扫描、设备连接、接收数据、保存数据和将数据返回给前端的功能。

## 功能概述

- **蓝牙检测**: 扫描周围的 BLE 设备，识别目标设备。
- **蓝牙连接**: 连接到目标设备，支持自动重连功能。
- **蓝牙数据处理**: 处理接收到的 BLE 数据并进行解析。
- **蓝牙数据保存**: 将处理后的数据保存到本地文件。
- **蓝牙数据返回**: 将接收到的数据返回到前端以供显示。

## 主要类

### `PermissionManager`

`PermissionManager` 负责处理蓝牙权限的申请和检查。确保应用程序在执行 BLE 操作之前已获得必要的权限。

#### 主要功能

- 检查蓝牙相关权限是否已授予。
- 请求蓝牙权限（如扫描、连接、定位等）。

### `BLEManager`

`BLEManager` 提供了与 BLE 设备交互的核心功能，涵盖了设备扫描、连接、接收数据、数据处理和保存等操作。

#### 核心功能

1. **蓝牙检测**：  
   `BLEManager` 类负责扫描附近的 BLE 设备。扫描操作在后台进行，扫描结果通过回调传递到主界面，用户可以看到目标设备的信息。

   ```java
   // 开始扫描
   bleManager.startScanning();

   // 停止扫描
   bleManager.stopScanning();
扫描回调: 当发现目标设备时，调用 onDeviceConnected() 回调方法。

2. **蓝牙检测**：  
   通过调用 connectToDevice() 方法连接到目标 BLE 设备。如果连接成功，系统会通过回调方法 onDeviceConnected() 通知前端，并开始发现该设备的服务。

   ```java
   // 连接设备
    bleManager.connectToDevice();

自动重连: 如果设备连接断开，BLEManager 会尝试在后台自动重新连接，最多重连 n 次。

3. **蓝牙数据处理**：  
  连接成功后，BLEManager 会读取设备的特征数据，通常是 UART 服务的数据。数据读取通过回调方法 onDataReceived() 返回，并通过 DataParser 进行处理。

   ```java
   // 读取数据
    bleManager.readData();

数据解析: DataParser 会解析传感器数据并转换为结构化数据（如温度、湿度等）。
数据处理回调: 处理后的数据通过回调传递给主界面。

4. **蓝牙数据保存**：  
  将接收到的数据保存到本地文件，通常以 CSV 格式保存。数据保存操作通过 DataParser 实现，并且可以在指定时间段内自动保存数据。

   ```java
    // 保存数据
    dataParser.saveDataToCSVFile(latestData);

5. **蓝牙数据返回**：  
  接收到的数据会实时显示在前端 UI 上，用户可以查看接收到的数据及设备的连接状态。更新 UI: 每当接收到数据时，前端会更新数据显示区域。

   ```java
   // 显示数据
    tvData.setText("接收到的数据: " + data);

<br>结构</br>
<br>├── PermissionManager.java  // 蓝牙权限管理类</br>
<br>├── BLEManager.java        // 蓝牙管理类</br>
<br>├── DataParser.java        // 数据解析和保存类</br>
<br>├── MainActivity.java      // 主界面，用户交互</br>
<br>└── AndroidManifest.xml    // 权限声明</br>


