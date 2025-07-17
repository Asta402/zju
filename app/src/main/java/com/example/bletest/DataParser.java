package com.example.bletest;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;
import android.os.Environment;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import android.content.Context;
import android.util.Log;
public class DataParser {
    // TAG 用于日志输出
    private static final String TAG = "DataParser";

    // 正则表达式，用于解析数据格式
    private static final Pattern DATA_PATTERN = Pattern.compile(
            "\\[(\\d+)\\]index:\\s*(\\d+)\\s*-\\s*x:\\s*([\\d.-]+)\\s*y:\\s*([\\d.-]+)\\s*z:\\s*([\\d.-]+)\\s*t:\\s*([\\d.-]+)"
    );

    // 用于显示解析后数据的 TextView
    private final TextView tvParsedData;

    // 数据解析的回调接口
    public final DataDisplayCallback callback;

    // 存储解析后的传感器数据
    public final List<SensorData> sensorDataList = new ArrayList<>();

    // 用于缓存接收到的数据
    private StringBuilder databuffer = new StringBuilder();

    // 控制是否保存数据的标志位
    private boolean isSavingData = false;
    private String currentFileName;  // 用于存储当前的文件名

    public boolean isSavingNoiseData = true;
    private int noiseFileCounter = -1;  // 初始计数器
    private int FileCounter =0 ;
    private FileOutputStream currentFileOutputStream = null;  // 当前文件输出流
    private BufferedWriter writer = null;  // 当前文件的 BufferedWriter
    // 用于检查是否正在保存噪声数据
    public boolean isSavingNoiseData() {
        return this.isSavingNoiseData;  // 返回当前的保存状态
    }



    // 定义数据解析后的回调接口
    public interface DataDisplayCallback {
        void onDataParsed(SensorData data); // 数据解析成功的回调
        void onParseError(String rawData, String errorMessage); // 数据解析错误的回调
    }
    public void setCurrentFileName(String fileName) {
        this.currentFileName = fileName;
    }
    // 构造函数，初始化 TextView 和回调接口
    public DataParser(TextView tvParsedData, DataDisplayCallback callback) {
        this.tvParsedData = tvParsedData;
        this.callback = callback;
    }

    // 允许外部控制是否保存数据


    /**
     * 处理接收到的原始数据
     * 将数据分行并逐行解析
     *
     * @param rawData 原始接收到的数据
     */
    public void processIncomingData(String rawData) {
        try {
            databuffer.append(rawData); // 缓存接收到的数据
            Log.e("processed_data:",databuffer.toString());
            String buffercontent = databuffer.toString();
            buffercontent = buffercontent.replace("\r", "").replace("\t", " "); // 清除换行符和制表符
            String[] lines = buffercontent.split("\\n"); // 按行分割数据

            // 遍历每一行数据并进行解析
            for (int i = 0; i < lines.length - 1; i++) {
                String line = lines[i].trim().replaceAll("\\s+", " ").trim();
                parseData(line); // 解析每一行数据
            }
        } catch (Exception e) {
            Log.e(TAG, "Data parsing failed", e);
            callback.onParseError(rawData, "解析错误: " + e.getMessage()); // 数据解析失败回调
        }
    }

    /**
     * 解析一行数据
     * 使用正则表达式匹配数据格式并提取传感器信息
     *
     * @param cleanData 清理过的行数据
     */
    private void parseData(String cleanData) {
        Matcher matcher = DATA_PATTERN.matcher(cleanData);
        if (!matcher.find()) {
            return; // 如果数据不匹配，则跳过
        }

        try {
            // 从正则匹配结果中提取传感器数据
            SensorData data = extractSensorData(matcher);
            sensorDataList.add(data); // 将解析的数据添加到数据列表中

            // 如果开启了保存数据的功能，则保存数据到文件
            if (isSavingData) {
                saveDataToCSVFile(data); // 保存数据到 CSV 文件
            }
            if (isSavingNoiseData) {
                saveNoiseDataToCSVFile(data);
            }

            updateDisplay(data); // 更新 UI 显示
            callback.onDataParsed(data); // 数据解析成功回调
        } catch (NumberFormatException e) {
            callback.onParseError(cleanData, "数值格式错误: " + e.getMessage()); // 处理解析过程中数值格式错误
        }
    }

    /**
     * 从正则表达式的匹配结果中提取传感器数据
     *
     * @param matcher 正则表达式匹配器
     * @return 返回一个 SensorData 对象
     */
    private SensorData extractSensorData(Matcher matcher) {
        return new SensorData(
                Long.parseLong(matcher.group(1)),  // timestamp
                Integer.parseInt(matcher.group(2)), // index
                parseDoubleSafely(matcher.group(3)), // x
                parseDoubleSafely(matcher.group(4)), // y
                parseDoubleSafely(matcher.group(5)), // z
                parseDoubleSafely(matcher.group(6))  // t
        );
    }

    /**
     * 安全地解析字符串为双精度浮点数
     * 如果解析失败，返回 0.0
     *
     * @param value 要解析的字符串
     * @return 解析后的双精度浮点数值
     */
    private double parseDoubleSafely(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            Log.w(TAG, "Invalid number format: " + value); // 解析失败时的警告日志
            return 0.0; // 返回默认值
        }
    }

    /**
     * 更新 UI 显示传感器数据
     * 将解析后的数据更新到 TextView 上
     *
     * @param data 传感器数据
     */
    private void updateDisplay(SensorData data) {
        String displayText = String.format(
                "时间戳: %d\n索引: %d\nX: %.3f\nY: %.3f\nZ: %.3f\nT: %.3f",
                data.timestamp, data.index, data.x, data.y, data.z, data.t
        );
        tvParsedData.setText(displayText); // 更新 TextView
    }

    /**
     * 获取当前的世界时间（UTC）
     *
     * @return 返回格式化的世界时间字符串
     */
    private String getWorldTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));  // 设置为世界时（UTC）
        Date now = new Date();
        return sdf.format(now); // 格式化并返回世界时间
    }

    public void setSavingData(boolean isSaving) {
        if (isSaving == this.isSavingData) {
            return;  // 如果状态未变化时，直接返回
        }

        this.isSavingData = isSaving;

        if (isSaving) {
            // 每次开始保存时，创建一个新的文件
            FileCounter++;  // 更新计数器，确保文件名唯一

            // 生成新的文件名
            String dataFileName = "px_c_a_" + FileCounter + ".txt";  // 可以根据需要修改扩展名

            // 获取Download目录
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File outputFile = new File(downloadDir, dataFileName);  // 新文件路径

            try {
                // 以追加模式打开文件
                currentFileOutputStream = new FileOutputStream(outputFile, true);
                writer = new BufferedWriter(new OutputStreamWriter(currentFileOutputStream));

                // 如果文件为空时，写入表头
                if (outputFile.length() == 0) {
                    writer.write("世界时间, 时间戳, 索引, X, Y, Z, T\n");
                }

                Log.d(TAG, "正样本数据开始保存到文件: " + outputFile.getAbsolutePath());
            } catch (IOException e) {
                Log.e(TAG, "无法创建正样本数据文件", e);
            }
        } else {
            // 停止保存正样本数据
            try {
                if (writer != null) {
                    writer.close();
                }
                if (currentFileOutputStream != null) {
                    currentFileOutputStream.close();
                }
                Log.d(TAG, "正样本数据保存已停止");
            } catch (IOException e) {
                Log.e(TAG, "关闭正样本数据文件时出错", e);
            }
        }
    }

    /**
     * 将传感器数据保存到 CSV 文件，并加上世界时间
     *
     * @param data 要保存的传感器数据
     */
// 保存正样本数据到txt文件
    public void saveDataToCSVFile(SensorData data) {
        if (!isSavingData || writer == null) {  // 只有在开启保存时才会执行
            return;  // 如果未开启保存或文件未初始化时跳过
        }
        try {
            // 获取当前世界时间
            String worldTime = getWorldTime();

            // 格式化数据并写入文件
            String dataString = String.format(
                    "%s, %d, %d, %.3f, %.3f, %.3f, %.3f\n",
                    worldTime, data.timestamp, data.index, data.x, data.y, data.z, data.t
            );
            writer.write(dataString); // 将数据写入文件
            writer.flush(); // 确保数据立即写入文件
            Log.d(TAG, "正样本数据保存到文件");

        } catch (IOException e) {
            Log.e(TAG, "保存正样本数据失败", e);  // 错误日志
        }
    }


    /**
     * 清空数据
     * 清除所有传感器数据并更新 UI
     */
    public void clearData() {
        sensorDataList.clear(); // 清空传感器数据列表
        tvParsedData.setText("无数据"); // 更新 UI 显示
        Log.i(TAG, "All data cleared"); // 记录日志
    }

    // 保存噪声数据到txt文件
    public void saveNoiseDataToCSVFile(SensorData data) {
        if (!isSavingNoiseData || writer == null) {
            return; // 未开启保存或文件未初始化时跳过
        }
        try {
            String worldTime = getWorldTime();
            String dataString = String.format(
                    "%s, %d, %d, %.3f, %.3f, %.3f, %.3f\n",
                    worldTime, data.timestamp, data.index, data.x, data.y, data.z, data.t
            );
            writer.write(dataString); // 写入数据
            writer.flush(); // 实时写入文件，避免缓冲延迟
        } catch (IOException e) {
            Log.e(TAG, "保存噪声数据失败", e);
        }
    }

    // 修改启动保存逻辑，保存为txt文件
    public void setSavingNoiseData(boolean isSaving) {
        if (isSaving == this.isSavingNoiseData) {
            return; // 状态未变化时直接返回
        }

        this.isSavingNoiseData = isSaving;

        if (isSaving) {
            // 生成新文件名（如 px_1_x.txt, px_2_x.txt...）
            String noiseFileName = "px_n_" + noiseFileCounter + ".txt";  // 更改为.txt扩展名
            noiseFileCounter--; // 每次保存后递增计数器，确保文件名唯一
            try {
                // 获取Download目录
                File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File outputFile = new File(downloadDir, noiseFileName); // 文件路径

                // 以追加模式打开文件
                currentFileOutputStream = new FileOutputStream(outputFile, true);
                writer = new BufferedWriter(new OutputStreamWriter(currentFileOutputStream));

                // 如果是新文件，写入表头
                if (outputFile.length() == 0) {  // 文件为空时写入表头
                    writer.write("世界时间, 时间戳, 索引, X, Y, Z, T\n");
                }
                Log.d(TAG, "开始保存噪声数据到文件: " + outputFile.getAbsolutePath());
            } catch (IOException e) {
                Log.e(TAG, "无法创建噪声数据文件", e);
            }
        }
    }



}
