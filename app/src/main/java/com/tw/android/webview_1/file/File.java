package com.tw.android.webview_1.file;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class File {

    public static String getRealPathFromUri(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * 根据路径获取bitmap（压缩后）
     *
     * @param srcPath 图片路径
     * @param width   最大宽（压缩完可能会大于这个，这边只是作为大概限制，避免内存溢出）
     * @param height  最大高（压缩完可能会大于这个，这边只是作为大概限制，避免内存溢出）
     * @param size    图片大小，单位kb
     * @return 返回压缩后的bitmap
     */
    public static Bitmap getCompressBitmap(String srcPath, float width, float height, int size) {
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        // 開始讀入圖片, 此時把options.inJustDecodeBounds 設回true了
        newOpts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(srcPath, newOpts);
        newOpts.inJustDecodeBounds = false;
        int w = newOpts.outWidth;
        int h = newOpts.outHeight;
        int scaleW = (int) (w / width);
        int scaleH = (int) (h / height);
        int scale = scaleW < scaleH ? scaleH : scaleW;
        if (scale <= 1) {
            scale = 1;
        }
        newOpts.inSampleSize = scale; // 設置縮放比例
        // 重新讀入圖片, 注意此時已經把options.inJustDecodeBounds 設回false了

        Bitmap bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
        Log.i("srcPath", srcPath);
        Log.i("newOpts", String.valueOf(newOpts));
        // 壓縮好比例大小後再進行質量壓縮
        Log.i("TAG", "size: " + bitmap.getByteCount() + " width: " + bitmap.getWidth() + " heigth:" + bitmap.getHeight()); // 輸出影象資料
        return compressImage(bitmap, size);
//        return bitmap;
    }

    /**
     * 图片质量压缩
     *
     * @param image 传入的bitmap
     * @param size  压缩到多大，单位kb
     * @return 返回压缩完的bitmap
     */
    public static Bitmap compressImage(Bitmap image, int size) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // 質量壓縮方法, 這裡100表示不壓縮, 把壓縮後的數據存放到baos中
        int options = 100;
        image.compress(Bitmap.CompressFormat.JPEG, options, baos);
        // 循環判斷如果壓縮後圖片是否大於size, 大於繼續壓縮
        while (baos.toByteArray().length / 1024 > size) {
            // 重置baos即清空baos
            baos.reset();
            // 每次都減少10
            options -= 10;
            // 這裡壓縮options%, 把壓縮後的數據存放到baos中
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);
        }
        // 把壓縮後的數據baos存放到ByteArrayInputStream中
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());
        // 把ByteArrayInputStream數據生成圖片
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);
        return bitmap;
    }

    /**
     * bitmap保存为文件
     *
     * @param bm       bitmap
     * @param filePath 文件路径
     * @return 返回保存结果 true：成功，false：失败
     */
    public static boolean saveBitmapToFile(Bitmap bm, String filePath) {
        try {
            java.io.File file = new java.io.File(filePath);
            file.deleteOnExit();
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            boolean b = false;
            if (filePath.toLowerCase().endsWith(".png")) {
                b = bm.compress(Bitmap.CompressFormat.PNG, 100, bos);
            } else {
                b = bm.compress(Bitmap.CompressFormat.JPEG, 100, bos);
            }
            bos.flush();
            bos.close();
            return b;
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
        return false;
    }

}
