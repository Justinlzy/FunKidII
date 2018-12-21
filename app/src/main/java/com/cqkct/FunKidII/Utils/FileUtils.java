package com.cqkct.FunKidII.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.UUID;

import static com.mob.tools.utils.FileUtils.closeIO;

/**
 * 文件管理类
 */
public class FileUtils {

    private static final String TAG = FileUtils.class.getSimpleName();

    /**
     * APP 目录
     * @return
     */
    public static File getExternalStorageDirectoryFile() {
        File file = new File(Environment.getExternalStorageDirectory(), "FunKidII");
        if (!file.exists()) {
            if (!file.mkdir()) {
                L.w(TAG, "mkdir for getExternalStorageDirectoryFile failure");
            }
        }
        return file;
    }

    /**
     * APP 目录
     * @return
     */
    public static String getExternalStorageDirectory() {
        return getExternalStorageDirectoryFile().getAbsolutePath();
    }

    /**
     * 照片目录
     * @return
     */
    public static File getExternalStoragePhotoDirectoryFile() {
        File parent = getExternalStorageDirectoryFile();
        File file = new File(parent, "Photo");
        if (!file.exists()) {
            if (!file.mkdir()) {
                L.w(TAG, "mkdir for getExternalStoragePhotoDirectoryFile failure");
            }
        }
        return file;
    }

    /**
     * 照片目录
     * @return
     */
    public static String getExternalStoragePhotoDirectory() {
        return getExternalStoragePhotoDirectoryFile().getAbsolutePath();
    }

    /**
     * 缓存目录
     * @return
     */
    public static File getExternalStorageCacheDirectoryFile() {
        File parent = getExternalStorageDirectoryFile();
        File file = new File(parent, "Cache");
        if (!file.exists()) {
            if (!file.mkdir()) {
                L.w(TAG, "mkdir for getExternalStorageCacheDirectoryFile failure");
            }
        }
        File nomedia = new File(file, ".nomedia");
        if (!nomedia.exists()) {
            try {
                if (!nomedia.createNewFile()) {
                    L.w(TAG, "nomedia.createNewFile for getExternalStorageCacheDirectoryFile failure");
                }
            } catch (IOException e) {
                L.e(TAG, "nomedia.createNewFile()", e);
            }
        }
        return file;
    }

    /**
     * 缓存目录
     * @return
     */
    public static String getExternalStorageCacheDirectory() {
        return getExternalStorageCacheDirectoryFile().getAbsolutePath();
    }

    /**
     * 日志缓存目录
     * @return
     */
    public static File getExternalStorageLogDirectoryFile() {
        File parent = getExternalStorageCacheDirectoryFile();
        File file = new File(parent, "logs");
        if (!file.exists()) {
            if (!file.mkdir()) {
                L.w(TAG, "mkdir for getExternalStorageLogsDirectoryFile failure");
            }
        }
        return file;
    }

    /**
     * 日志缓存目录
     * @return
     */
    public static String getExternalStorageLogDirectory() {
        return getExternalStorageLogDirectoryFile().getAbsolutePath();
    }

    /**
     * 图片缓存目录
     * @return
     */
    public static File getExternalStorageImageCacheDirFile() {
        File parent = getExternalStorageCacheDirectoryFile();
        File file = new File(parent, "images");
        if (!file.exists()) {
            if (!file.mkdir()) {
                L.w(TAG, "mkdir for getExternalStorageImagesCacheDirFile failure");
            }
        }
        return file;
    }

    /**
     * 图片缓存目录
     * @return
     */
    public static String getExternalStorageImageCacheDir() {
        return getExternalStorageImageCacheDirFile().getAbsolutePath();
    }


    /**
     * 聊天缓存目录
     * @return
     */
    public static File getExternalStorageChatCacheDirFile() {
        File parent = getExternalStorageCacheDirectoryFile();
        File file = new File(parent, "chat");
        if (!file.exists()) {
            if (!file.mkdir()) {
                L.w(TAG, "mkdir for getExternalStorageChatCacheDirFile failure");
            }
        }
        return file;
    }

    /**
     * 聊天缓存目录
     * @return
     */
    public static String getExternalStorageChatCacheDir() {
        return getExternalStorageChatCacheDirFile().getAbsolutePath();
    }

    /**
     * 语音聊天缓存目录
     * @return
     */
    public static File getExternalStorageVoiceChatCacheDirFile() {
        File parent = getExternalStorageChatCacheDirFile();
        File file = new File(parent, "voice");
        if (!file.exists()) {
            if (!file.mkdir()) {
                L.w(TAG, "mkdir for getExternalStorageVoiceChatCacheDirFile failure");
            }
        }
        return file;
    }

    /**
     * 语音聊天缓存目录
     * @return
     */
    public static File getExternalStorageVoiceChatCacheDir() {
        return getExternalStorageVoiceChatCacheDirFile().getAbsoluteFile();
    }

    /**
     * 根据设备号生成宝贝头像文件名
     * @param idstr 设备号
     * @return 文件名
     */
    public static String genBabyHeadIconFilename(String idstr) {
        UUID uuid = UUID.randomUUID();
        return "H-D-" + idstr + "-" + uuid.toString().replace("-", "") + ".jpg";
    }

    /**
     * 根据设备号生成自定义头像文件名
     * @param idstr 设备号
     * @return 文件名
     */
    public static String genUserHeadIconFilename(String idstr) {
        UUID uuid = UUID.randomUUID();
        return "H-U-" + idstr + "-" + uuid.toString().replace("-", "") + ".jpg";
    }


    public static byte[] FileTobyte(String filePath)
    {
        byte[] buffer = null;
        try
        {
            File file = new File(filePath);
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int n;
            while ((n = fis.read(b)) != -1)
            {
                bos.write(b, 0, n);
            }
            fis.close();
            bos.close();
            buffer = bos.toByteArray();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return buffer;
    }

    /**
     * 根据语音文件获取语音时长
     * @param file
     * @return
     * @throws IOException
     */
    public static int getAmrDuration(File file) throws IOException {
        long duration = -1;
        int[] packedSize = { 12, 13, 15, 17, 19, 20, 26, 31, 5, 0, 0, 0, 0, 0,
                0, 0 };
        RandomAccessFile randomAccessFile = null;
        try {
            randomAccessFile = new RandomAccessFile(file, "rw");
            long length = file.length();// 文件的长度
            int pos = 6;// 设置初始位置
            int frameCount = 0;// 初始帧数
            int packedPos = -1;

            byte[] datas = new byte[1];// 初始数据值
            while (pos <= length) {
                randomAccessFile.seek(pos);
                if (randomAccessFile.read(datas, 0, 1) != 1) {
                    duration = length > 0 ? ((length - 6) / 650) : 0;
                    break;
                }
                packedPos = (datas[0] >> 3) & 0x0F;
                pos += packedSize[packedPos] + 1;
                frameCount++;
            }

            duration += frameCount * 20;// 帧数*20
        } finally {
            if (randomAccessFile != null) {
                randomAccessFile.close();
            }
        }
        return (int)((duration/1000)+1);
    }

    public static int getAudioFileDuration(String filepath) {
        try {
            MediaPlayer mp = new MediaPlayer();
            try {
                mp.setDataSource(filepath);
            } catch (Exception e) {
                L.e(TAG, "getAudioFileDuration MediaPlayer setDataSource", e);
                return 0;
            } finally {
                mp.release();
            }
            try {
                mp.prepare();
            } catch (Exception e) {
                L.e(TAG, "getAudioFileDuration MediaPlayer prepare", e);
                return 0;
            } finally {
                mp.release();
            }
            int length = mp.getDuration();
            mp.release();
            return length;
        } catch (Exception e) {
            L.e(TAG, "getAudioFileDuration", e);
        }
        return 0;
    }

    public static void byteToFile(byte[] buf, String filePath, String fileName)
    {
        BufferedOutputStream bos = null;
        FileOutputStream fos = null;
        File file = null;
        try
        {
            File dir = new File(filePath);
            if (!dir.exists() && dir.isDirectory())
            {
                dir.mkdirs();
            }
            file = new File(filePath + File.separator + fileName);
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            bos.write(buf);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (bos != null)
            {
                try
                {
                    bos.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            if (fos != null)
            {
                try
                {
                    fos.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
    /**
     * 根据路径判别文件是否存在
     *
     * @param filePath
     * @return
     */
    public static boolean isFileExist(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return false;
        }
        File file = new File(filePath);
        return file.isFile() && file.exists();
    }

    /**
     * 递归方式删除文件, 如果时目录，会删除目录自身
     * @param file 文件
     */
    public static void deleteFileRecursive(File file) {
        if (file.isDirectory())
            for (File child : file.listFiles())
                deleteFileRecursive(child);
        if (!file.delete()) {
            L.w(TAG, "delete \"" + file.getAbsolutePath() + "\" failure");
        }
    }

    /**
     * 删除目录
     * @param directory 目录
     */
    public static void deleteDirectory(File directory) {
        if (!directory.isDirectory())
            return;
        for (File child : directory.listFiles())
            deleteFileRecursive(child);
    }


    public static void saveBitmap(Bitmap bm, String filepath) {
        FileOutputStream outputStream = null;
        try {
            File f = new File(filepath);
            if (f.exists()) {
                f.delete();
            }
            outputStream = new FileOutputStream(f);
            bm.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    //读取文件到Byte数组
    public static byte[] readFile(String filePath) {
        try {
            FileInputStream in = new FileInputStream(filePath); // 读取文件路径
            byte bs[] = new byte[in.available()];
            in.read(bs);
//            System.out.println("file content=\n" + new String(bs));
            in.close();
            return bs;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void copyFile(@NonNull Context context, @NonNull Uri inputUri, @Nullable Uri outputUri) throws NullPointerException, IOException {
        L.d(TAG, "copyFile");

        if (outputUri == null) {
            throw new NullPointerException("Output Uri is null - cannot copy image");
        }

        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(inputUri);
            outputStream = new FileOutputStream(new File(outputUri.getPath()));
            if (inputStream == null) {
                throw new NullPointerException("InputStream for given input Uri is null");
            }

            byte buffer[] = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception ignored) {}
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception ignored) {}
            }
        }
    }
    public static final byte[] input2byte(InputStream inStream) {
        if (inStream == null) {
            return null;
        } else {
            byte[] in2b = null;
            BufferedInputStream in = new BufferedInputStream(inStream);
            ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
            boolean var4 = false;

            try {
                int rc;
                while((rc = in.read()) != -1) {
                    swapStream.write(rc);
                }

                in2b = swapStream.toByteArray();
            } catch (IOException var9) {
                var9.printStackTrace();
            } finally {
                closeIO(inStream, in, swapStream);
            }

            return in2b;
        }
    }

}
