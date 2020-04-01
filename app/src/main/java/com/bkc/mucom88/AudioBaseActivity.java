package com.bkc.mucom88;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

public class AudioBaseActivity extends AppCompatActivity implements AudioService.AudioNotifyBinder {
    final static String TAG  = "AudioBaseActivity";

    protected AudioService audioService = new AudioService();
    protected AudioRenderer renderer;

    protected String songFilename = null;

    ////////////////////////////////////////
    // サービス関連

    // サービスの開始
    protected void startService() {
        Intent intent = new Intent(this, AudioRenderer.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(intent);
        else
            startService(intent);
    }
    // サービスの終了
    protected void stopService() {
        stopService(new Intent(this, AudioRenderer.class));
    }

    protected void audioBindService() {
        audioService.doBindService(this, new Intent(this, AudioRenderer.class), this);
    }

    protected void audioUnbindService() {
        audioService.doUnbindService(this);
    }


    // 接続
    @Override
    public void notifyConnected(AudioRenderer rendererInstance) {
        renderer = rendererInstance;
        Log.d(TAG, "notifyConnected");
        Intent MyIntent = new Intent( this,MainActivity.class );

        PendingIntent cbIntent = PendingIntent.getActivity(
                this, 0, MyIntent, 0 );

        renderer.setCallbackIntent( cbIntent );

        // 通知の受け取り
        checkIntent();

        // 接続後にUIをスタートさせる
        startUIThread();
    }

    @Override
    public void notifyDisconnected() {
    }
    ////////////////////////////////////
    // レンダラコントロール
    enum rendererCommand {
        Play,
        Stop
    }

    protected boolean showResultFlag = false;

    protected void controlRenderer(rendererCommand control) {
        if (renderer == null) return;
        switch(control) {
            case Play:
                showResultFlag = true;
                renderer.play(songFilename);
                break;
            case Stop:
                renderer.stop();
                break;
        }
    }

    protected boolean rendererIsPlaying() {
        if (renderer == null) return false;
        return renderer.isPlaying();
    }

    ////////////////////////////////////
    // 他のアプリからの受け渡しを確認する
    void checkIntent() {
        if (renderer == null) return;
        String act = getIntent().getAction();
        if (act == null) return;

        if (act.equals(Intent.ACTION_VIEW)) {
            getIntent().setAction("");
            Uri uri = getIntent().getData();

            Log.d(TAG,"scheme:" + uri.getScheme() + " string:" + uri.toString());
            // openURIsong(uri);
        }
    }


    //////////////////////////////////////
    // 画面関連
    Handler ui_handler = new Handler();

    class UILoop implements Runnable {
        public boolean loopFlag = false;
        public boolean loopEndFlag = false;
        @Override
        public void run() {
            loopEndFlag = false;
            loopFlag = true;
            while(loopFlag) {
                ui_handler.post(new Runnable() {
                    @Override
                    public void run() {
                        intervalUpdate();
                    }
                });
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            loopEndFlag = true;
        }
    }

    private static UILoop uiloop = null;

    // UIスレッドの開始
    private void startUIThread()
    {
        Log.d(TAG,"Start UIThread...");

        if (uiloop != null && uiloop.loopFlag) {
            Log.d(TAG,"Using current thread...");
            return;
        }

        uiloop = new UILoop();
        new Thread(uiloop).start();

        Log.d(TAG,"OK...");
    }

    // UIスレッドの終了
    protected void stopUIThread()
    {
        if (uiloop == null) return;
        uiloop.loopFlag = false;
        while(!uiloop.loopEndFlag) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG,"UIThread is Stopped.");
    }

    // UI更新用に呼ばれる
    protected void intervalUpdate() {
        if (renderer == null) return;

        // 情報更新
        updateInformation();
    }

    // レンダラ動作時に更新する
    protected void updateInformation() {
    }


    final int REQUEST_PERMISSION = 1000;
    //////////////////////////////////////
    // 外部ストレージ書き込み許可
    protected void checkExternalStoragePermission() {
        if(Build.VERSION.SDK_INT < 23) return;

        // 許可済み
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) return;

        // 許可を求める
        requestLocationPermission();
    }

    // 許可を求める
    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);
        } else {
            showToast("SDカードへのアクセス許可が必要です。");
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,},
                    REQUEST_PERMISSION);
        }
    }

    // 結果の受け取り
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION) {
            // 使用が許可された
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) return;
            showToast("アクセスが拒否されました。");
        }
    }

    // トースト表示
    protected void showToast(String text) {
        Toast.makeText(this , text, Toast.LENGTH_LONG).show();
    }

}
