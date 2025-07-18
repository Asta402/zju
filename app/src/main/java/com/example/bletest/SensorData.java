package com.example.bletest;

public class SensorData {
    // 时间戳，表示数据采集的时间
    public long timestamp;

    // 数据的索引，用于标识不同的传感器数据
    public int index;

    // 传感器的三个轴向数据
    public double x, y, z;

    // 传感器的某个值，可能是传感器的速度、加速度等
    public double t;

    // 构造函数，用于初始化传感器数据
    public SensorData(long timestamp, int index, double x, double y, double z, double t) {
        this.timestamp = timestamp;
        this.index = index;
        this.x = x;
        this.y = y;
        this.z = z;
        this.t = t;
    }

    // 重写toString方法，用于打印数据的简要信息
    @Override
    public String toString() {
        return String.format("[%d] #%d: X=%.3f, Y=%.3f, Z=%.3f, T=%.3f",
                timestamp, index, x, y, z, t);
    }

}
