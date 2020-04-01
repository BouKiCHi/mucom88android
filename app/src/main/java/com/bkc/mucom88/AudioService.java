package com.bkc.mucom88;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class AudioService {
    private String TAG = "AudioService";
    private boolean isBindService = false;

    //通知用インターフェース
    public interface AudioNotifyBinder {
        public void notifyConnected(AudioRenderer pcm);
        public void notifyDisconnected();
    }

    private AudioNotifyBinder notify;
    private ServiceConnection audioConnect;

    // バインド
    public void doBindService(Context ctx , Intent intent , AudioNotifyBinder destNotify) {
        if (!isBindService) {
            Log.d(TAG, "BindService");

            audioConnect =  new ServiceConnection() {
                // 接続
                @Override
                public void onServiceConnected(ComponentName className, IBinder service) {
                    Log.d(TAG, "onServiceConnected");
                    notify.notifyConnected( ((AudioRenderer.LocalBinder)service).getService());
                }

                // 切断
                @Override
                public void onServiceDisconnected(ComponentName className) {
                    Log.d(TAG, "onServiceDisconnected");
                    notify.notifyDisconnected();
                    isBindService = false;
                }
            };
            ctx.bindService(intent, audioConnect, Context.BIND_AUTO_CREATE);
            isBindService = true;
            notify = destNotify;
        }
    }

    // アンバインド
    public void doUnbindService(Context ctx) {
        if (isBindService) {
            Log.d(TAG, "UnbindService");
            ctx.unbindService(audioConnect);
            isBindService = false;
        }
    }

}
