package com.example.bletest;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionManager {

    // 请求权限的标识符
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 2;

    // 当前 Activity
    private Activity activity;

    // 构造函数，初始化 Activity
    public PermissionManager(Activity activity) {
        this.activity = activity;
    }

    /**
     * 检查是否已获取所有蓝牙相关权限
     *
     * @return 返回是否缺少权限，如果没有缺少权限，则返回 true。
     */
    public boolean hasAllBluetoothPermissions() {
        return getMissingPermissions().isEmpty(); // 如果没有缺少权限，则返回 true
    }

    /**
     * 获取缺少的权限列表
     *
     * @return 返回缺少的权限列表
     */
    public List<String> getMissingPermissions() {
        List<String> missingPermissions = new ArrayList<>();

        // 根据 SDK 版本判断需要的权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 及以上需要检查蓝牙相关权限
            checkPermission(Manifest.permission.BLUETOOTH_SCAN, missingPermissions);
            checkPermission(Manifest.permission.BLUETOOTH_CONNECT, missingPermissions);
        } else {
            // Android 12 以下需要检查 Bluetooth 和 Bluetooth_ADMIN 权限
            checkPermission(Manifest.permission.BLUETOOTH, missingPermissions);
            checkPermission(Manifest.permission.BLUETOOTH_ADMIN, missingPermissions);
        }

        // 需要定位权限来扫描蓝牙设备
        checkPermission(Manifest.permission.ACCESS_FINE_LOCATION, missingPermissions);

        return missingPermissions;
    }

    /**
     * 检查单个权限是否已经授予
     *
     * @param permission 权限名称
     * @param missingPermissions 缺少权限的列表，若缺少该权限则添加至列表
     */
    private void checkPermission(String permission, List<String> missingPermissions) {
        if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(permission); // 如果没有授权该权限，添加到缺少权限列表
        }
    }

    /**
     * 请求缺少的蓝牙权限
     */
    public void requestBluetoothPermissions() {
        List<String> missingPermissions = getMissingPermissions(); // 获取缺少的权限列表
        if (missingPermissions.isEmpty()) return; // 如果没有缺少权限，直接返回

        // 弹出权限请求对话框
        new AlertDialog.Builder(activity)
                .setTitle("需要蓝牙权限") // 对话框标题
                .setMessage("此应用需要蓝牙权限来连接设备") // 对话框提示信息
                .setPositiveButton("授权", (dialog, which) -> {
                    // 用户点击授权按钮后，申请缺少的权限
                    ActivityCompat.requestPermissions(activity,
                            missingPermissions.toArray(new String[0]), // 将 List 转换为数组
                            REQUEST_BLUETOOTH_PERMISSIONS); // 请求权限的标识符
                })
                .setNegativeButton("取消", null) // 用户点击取消时，不做任何操作
                .show();
    }
}
