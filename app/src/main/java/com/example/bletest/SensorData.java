package com.example.bletest;

public class SensorData {
    // 时间戳，表示数据采集的时间
    public long timestamp;

    // 数据的索引，用于标识不同的传感器数据
    public int index;

    // 传感器的三个轴向数据
    public double x, y, z;

    // 传感器的某个值，可能是传感器的速度、加速度等
    public double v;

    // 构造函数，用于初始化传感器数据
    public SensorData(long timestamp, int index, double x, double y, double z, double v) {
        this.timestamp = timestamp; // 初始化时间戳
        this.index = index;         // 初始化数据索引
        this.x = x;                 // 初始化X轴数据
        this.y = y;                 // 初始化Y轴数据
        this.z = z;                 // 初始化Z轴数据
        this.v = v;                 // 初始化V值（可能代表速度、加速度等）
    }

    // 重写toString方法，用于打印数据的简要信息
    @Override
    public String toString() {
        // 格式化返回：时间戳、索引、X、Y、Z、V值
        return String.format("[%d] #%d: X=%.3f, Y=%.3f, Z=%.3f, V=%.3f",
                timestamp, index, x, y, z, v);
    }

    // 返回详细信息的字符串，适合用于日志输出或者调试
    public String toDetailedString() {
        // 格式化返回：时间戳、索引、X、Y、Z、V值的详细信息
        return String.format("时间戳: %d | 索引: %d\nX轴: %.3f | Y轴: %.3f\nZ轴: %.3f | V值: %.3f",
                timestamp, index, x, y, z, v);
    }
}
