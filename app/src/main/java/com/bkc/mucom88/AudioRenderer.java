package com.bkc.mucom88;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import android.util.Log;

import java.io.File;
import java.io.UnsupportedEncodingException;

public class AudioRenderer extends Service {
    public static final int NOTIFY_ID = 1;
    public static final String CHARSET_SJIS = "Windows-31j";
    final private String TAG = "AudioRenderer";

    static {
        System.loadLibrary("native-lib");
    }

    public native byte[] getMucomInstance();

    public native byte[] getResultFromMucom(byte[] mucomInstance);
    public native int playSong(byte[] mucomInstance, String workDirectory, String filename);
    public native void closeSong(byte[] mucomInstance);
    public native void renderingSong(byte[] mucomInstance, short[] buf, int samples);

    public void stop() {
        if (AudioRunner != null) AudioRunner.stopSong();
    }

    public void play(String filepath) {
        stop();
        startAudioThread(filepath);
    }

    // 結果
    public String getResult() {
        if (AudioRunner == null) return "";
        byte[] result = AudioRunner.getResult();
        try {
            return new String(result, CHARSET_SJIS);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    public boolean isPlaying() {
        return AudioRunner != null && AudioRunner.isPlaying();
    }

    public boolean hasMessage() {
        if (AudioRunner == null) return false;
        if (AudioRunner.isPlaying() || AudioRunner.isFailed()) return true;
        return false;
    }

    // 開始処理
    @Override
    public void onCreate()
    {
        NotificationCompat.Builder builder = prepareNotificationBuilder("起動しました", getString(R.string.app_title_long));
        startForeground(NOTIFY_ID, builder.build());

        super.onCreate();
        Log.d(TAG, "onCreate");
        notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    }

    // 終了処理
    @Override
    public void onDestroy()
    {
        Log.d(TAG,"onDestroy");
        stopNotification();
        if (AudioRunner != null) AudioRunner.stopSong();
        super.onDestroy();
    }

    public String getTime() {
        return AudioRunner != null ? AudioRunner.getTime() : "--:--";
    }


    public class AudioThread extends Thread {
        private boolean Playing = false;
        private boolean Stopping = false;
        private boolean Loading = false;
        private boolean Failed = false;
        public String FilePath;

        private byte[] mucomInstance = null;

        public boolean isPlaying() {
            return Playing;
        }

        public boolean isFailed() {
            return Failed;
        }

        public byte[] getResult() {
            return getResultFromMucom(mucomInstance);
        }

        // 曲再生
        public void startSong(String filepath) {
            FilePath = filepath;
            Loading = true;
            start();
        }
        // 曲停止
        public void stopSong() {
            Stopping = true;
        }

        // 秒数
        public String getTime() {
            if (!Playing) return "--:--";
            long sec = atCurrentPosition / atAudioRate;
            return String.format("%02d:%02d", sec / 60, sec % 60 );
        }

        //////////////////////////////
        // オーディオハードウェア関連
        AudioTrack at = null;
        private int    atBufSize = 0;
        private int    atBufPos = 0;
        private int    atMinBuf = 0;
        private int    atUpdateFrame = 0;
        private long   atCurrentPosition = 0;

        int  atAudioRate = 44100;

        private boolean atPlay = false;


        // オーディオ初期化
        private boolean audioInit()
        {
            int rate = atAudioRate;
            int blocks = 4;

            int ch_bit = AudioFormat.ENCODING_PCM_16BIT;
            int ch_out = AudioFormat.CHANNEL_OUT_STEREO; // later 2.0

            if (at != null) return false;

            atMinBuf = AudioTrack.getMinBufferSize(rate, ch_out, ch_bit);
            atBufSize = atMinBuf * blocks;

            at = new AudioTrack(AudioManager.STREAM_MUSIC,
                    rate, ch_out, ch_bit,
                    atBufSize, AudioTrack.MODE_STREAM);

            if (at == null) return false;

            atUpdateFrame = rate / 4;
            atBufPos = 0;
            atPlay = false;

            return true;
        }

        // オーディオ終了処理
        private void audioFree() {
            if (at == null) return;

            at.setStereoVolume( 0.0f , 0.0f );
            at.flush();
            at.stop();
            try {
                while(at.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                    Thread.sleep(10);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            at.release();
            at = null;
        }


        @Override
        public void run() {
            audioInit();

            short atPCM[] = new short[atBufSize * 2];

            // 優先度の変更
            android.os.Process.setThreadPriority(
                    android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            Log.d(TAG,"Start audio thread");

            int updateFrameCount = 0;
            // サイズが大きいとUIフリーズバグが発生する
            int atPackSize = 2048;

            long oldPlaybackPosition = 0;

            while(true) {
                // 曲の読み出し
                if (Loading) {
                    Loading = false;
                    mucomInstance = getMucomInstance();
                    File a = new File(FilePath);
                    String directory = a.getParent();
                    String filename = a.getName();

                    if (playSong(mucomInstance, directory, filename) < 0) {
                        Failed = true;
                        continue;
                    }
                    updateNotification(filename, getString(R.string.app_title_long));
                    Playing = true;
                    atCurrentPosition = 0;
                }

                // 曲の停止
                if (Stopping) {
                    Stopping = false;
                    closeSong(mucomInstance);
                    mucomInstance = null;
                    Playing = false;
                    break;
                }

                if (Playing && at != null) {
                    // renderData(atPCM, atPackSize/2);
                    renderingSong(mucomInstance, atPCM, atPackSize/2);
                    int bufferSize = at.write( atPCM , 0 , atPackSize );

                    // バッファ位置への加算
                    atBufPos += bufferSize;

                    // バッファが満たされたら再生開始
                    if (!atPlay && atBufPos >= atMinBuf) {
                        atPlay = true;
                        at.play();
                    }

                    // 現在の再生ポジション
                    long current = at.getPlaybackHeadPosition();
                    long diff = current - oldPlaybackPosition;
                    updateFrameCount += diff;
                    atCurrentPosition += diff;
                    oldPlaybackPosition = current;
                }

                // 再生ポジションがある程度進んだら情報更新
                while(updateFrameCount >= atUpdateFrame) {
                    updateFrameCount -= atUpdateFrame;
                }
            }

            audioFree();
        }
    }

    private AudioThread AudioRunner = null;

    // オーディオ再生スレッドの作成
    private void startAudioThread(String filepath) {
        AudioRunner = new AudioThread();
        AudioRunner.startSong(filepath);
    }



    /////////////////////////////
    // サービス用
    class LocalBinder extends Binder {
        AudioRenderer getService() {
            return AudioRenderer.this;
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return new LocalBinder();
    }

    ///////////////////////////////
    // 通知

    private static NotificationManager notificationManager = null;
    public static final String NOTIFICATION_CHANNEL_ID = "audioMainNotification";

    private PendingIntent cbIntent;

    private void addNotificationChannelId() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationManager manager = (NotificationManager)getSystemService(this.NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel(
                AudioRenderer.NOTIFICATION_CHANNEL_ID,
                "通知",
                NotificationManager.IMPORTANCE_LOW
        );

        channel.setShowBadge(false);

        channel.setSound(null,null);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        manager.createNotificationChannel(channel);
    }

    private NotificationCompat.Builder prepareNotificationBuilder(String title, String text) {
        addNotificationChannelId();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        builder.setContentIntent(cbIntent);
        builder.setSmallIcon(android.R.drawable.ic_media_play);
        builder.setOnlyAlertOnce(true);
        builder.setOngoing(true);
        builder.setAutoCancel(false);
        // builder.setSound(null, AudioManager.STREAM_SYSTEM);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);
        builder.setTicker(title);
        builder.setContentTitle(title);
        builder.setContentText(text);
        builder.setWhen(System.currentTimeMillis());
        builder.setDefaults(0);
        return builder;
    }

    // 通知表示
    public void updateNotification(String title, String text) {
        NotificationCompat.Builder builder = prepareNotificationBuilder(title, text);
        notificationManager.notify(NOTIFY_ID, builder.build());
    }

    // 通知をキャンセルする
    private void stopNotification() {
        notificationManager.cancel(NOTIFY_ID);
    }

    // 呼び出しインテントの設定
    public void setCallbackIntent(PendingIntent intent) {
        cbIntent = intent;
    }


    // PCMテスト用
    int sampleCount = 0;
    short sampleValue = 0;
    private void renderData(short[] atPCM, int samples) {
        for(int i = 0; i < samples*2;) {
            atPCM[i++] = sampleValue;
            atPCM[i++] = sampleValue;
            if (sampleCount < 100) sampleCount++; else {
                sampleCount = 0;
                sampleValue = (short)((int)sampleValue > 0 ? 0 : 8192);
            }
            sampleCount++;
        }
    }
}
