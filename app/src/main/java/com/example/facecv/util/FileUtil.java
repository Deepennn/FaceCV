package com.example.facecv.util;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtil {


    /**
     * 获取位于 Assets 目录下的文件的绝对路径，如果文件已存在于应用的内部存储中则直接返回该路径。
     *
     * @param context   上下文对象
     * @param assetName 要获取的文件名
     * @return 文件的绝对路径
     * @throws IOException 读取或写入文件时可能抛出的异常
     */
    public static String assetFilePath(Context context, String assetName) throws IOException {
        // 创建一个File对象，表示存储在应用内部存储中的目标文件
        File file = new File(context.getFilesDir(), assetName);

        // 如果文件已存在且大小大于0，则表示文件已经被复制到应用的内部存储中，直接返回该文件的绝对路径
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        // 如果文件不存在或大小为0，则从Assets中读取文件，并将其复制到应用的内部存储中
        try (InputStream is = context.getAssets().open(assetName);
             FileOutputStream os = new FileOutputStream(file)) {
            byte[] buffer = new byte[4 * 1024];
            int read;
            // 循环读取输入流中的数据，并写入输出流，直到读取完整个文件
            while ((read = is.read(buffer)) != -1) {
                os.write(buffer, 0, read);
            }
            os.flush();
            // 返回复制后的文件的绝对路径
            return file.getAbsolutePath();
        }
    }


}
