package com.bkc.mucom88;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class GeneralUtils {

    // 許可がある場合はtrue
    public static boolean checkPermission(Context context) {
        if(Build.VERSION.SDK_INT < 23) return true;

        // 許可済み
        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) return true;

        return false;
    }

    // 許可の説明が必要な場合はtrue
    public static boolean shouldShowRationale(Activity activity) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    // 許可を求める
    public static void requestPermission(Activity activity, int RequestCode) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                RequestCode);
    }

    public static byte[] readAllBytes(File file) {

        try {
            long Length = file.length();
            byte[] result = new byte[(int) Length];
            InputStream in = new FileInputStream(file);
            long pos = 0;

            byte[] buf = new byte[1024];

            int len;
            while ((len = in.read(buf)) > 0) {
                System.arraycopy(buf,0, result, (int) pos, len);
                pos += len;
            }

            in.close();
            return result;

        } catch (IOException ignored) {
            return null;
        }
    }

    // ファイルコピー
    public static boolean copyFile(File srcFile, File destFile) {
        try {
            InputStream in = new FileInputStream(srcFile);
            try {
                OutputStream out = new FileOutputStream(destFile);
                try {
                    // Transfer bytes from in to out
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                } finally {
                    out.close();
                }
            } finally {
                in.close();
            }
        } catch (IOException ignored) {
            return false;
        }
        return true;
    }

}
