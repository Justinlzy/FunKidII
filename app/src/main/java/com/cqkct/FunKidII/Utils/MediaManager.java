package com.cqkct.FunKidII.Utils;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;

/**
 * 用于播放声音
 */
public class MediaManager {

    private static MediaPlayer mPlayer;
    private static final Object mPlayerLock = new Object();

    public interface PlaySoundListener{
        //播放前 该方法中不要对mPlayer再做操作
        public abstract void playBefore();
        //播完后
        public abstract void playCompletion();
    }

    public static MediaPlayer getMediaPlayerInstance() {
        if (mPlayer == null) {
            synchronized (mPlayerLock) {
                if (mPlayer == null) {
                    mPlayer = new MediaPlayer();
                }
            }
        }
        return mPlayer;
    }

    public static void playSound(String filePathString, final PlaySoundListener playSoundListener) {

        try {
            mPlayer = getMediaPlayerInstance();

            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mPlayer.setDataSource(filePathString);

            mPlayer.setOnCompletionListener(new OnCompletionListener() {//播放完的监听函数
                @Override
                public void onCompletion(MediaPlayer mp) {
                    playSoundListener.playCompletion();//播完后
                    release();//播完后就停止
                }
            });

            mPlayer.prepare();

            playSoundListener.playBefore();//播放之前

            mPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //暂停
    public static void pause() {
        if (mPlayer != null && mPlayer.isPlaying()) {
            mPlayer.pause();
        }
    }

    //停止
    public static void stop() {
        if (mPlayer != null) {
            mPlayer.stop();
        }
    }

    //正在播放
    public static boolean isPlaying() {
        return mPlayer != null && mPlayer.isPlaying();
    }

    public static void release() {
        synchronized (mPlayerLock) {
            if (mPlayer != null) {
                try {
                    mPlayer.stop();
                } catch (Exception ignore) {}
                mPlayer.release();
                mPlayer = null;
            }
        }
    }

}
