package com.bkc.mucom88;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.bkc.fileselect.FileActivity;
import com.google.android.material.navigation.NavigationView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class MainActivity extends AudioBaseActivity implements
        View.OnClickListener, NavigationView.OnNavigationItemSelectedListener {

    final static String TAG  = "MainActivity";

    public static final String SUCCESS_SAVE = "保存しました:";
    public static final String SUCCESS_COPY = "コピーに成功しました:";
    public static final String FAIL_COPY = "コピーに失敗しました:";
    public static final String CLEAR_CACHE = "キャッシュをクリアしました";
    public static final String MUSIC_FILE_PATH = "musicFilePath";
    public static final String VOICE_FILE_PATH = "voiceFilePath";
    public static final String PCM_FILE_PATH = "pcmFilePath";
    public static final String SONG_MUC = "song.muc";

    // ファイルパス
    private String musicFilePath;
    private String voiceFilePath;
    private String pcmFilePath;

    enum requestCodeEnum {
        LoadMusic,
        SelectPCM,
        SelectVoice,
        SaveMusic
    }

    /////////////////////////////
    // Activity遷移

    // 起動時
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout);

        addEditorFragment(savedInstanceState);

        // アクションバー
        Toolbar tb = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(tb);

        // アクションバーにドロアを接続
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle actionBarDrawerToggle =
                new ActionBarDrawerToggle(
                        this, drawerLayout, tb, R.string.drawer_open, R.string.drawer_close);
        drawerLayout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();

        // ナビゲーションビュー
        NavigationView nv = (NavigationView) findViewById(R.id.nav_view);
        nv.setNavigationItemSelectedListener(this);

        // 確認
        checkExternalStoragePermission();

        // サービスへの接続
        startService();
    }


    // 再開時 or 開始時
    @Override
    public void onResume() {
        // サービスへの接続
        Log.d(TAG,"onResume");

        // 設定読み出し
        loadSetting();

        // 曲ファイル読み出し
        EditText et = null;
        //EditText et = (EditText) findViewById(R.id.editText);
        editorSetFile(new File(getCacheDir(), SONG_MUC));
        editorLoad();

        audioBindService();
        setViewHandler();
        super.onResume();
    }

    boolean playing = false;

    // 終了時
    @Override
    public void onPause() {
        Log.d(TAG,"onPause");

        // UIの停止
        stopUIThread();

        // 設定保存
        saveSetting();

        // 保存
        editorSave();

        playing = rendererIsPlaying();

        // バインド解除
        audioUnbindService();
        renderer = null;
        super.onPause();
    }

    // 終了処理
    @Override
    public void onDestroy() {
        // 再生していない場合はサービスを停止する
        if (!playing) stopService();
        super.onDestroy();
    }

    // 画面更新　レンダラ動作時
    @Override
    protected void updateInformation() {
        if (showResultFlag && renderer.isPlaying()) {
            showResultFlag = false;
            showResult();
        }
    }


    // 設定
    private void loadSetting() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        musicFilePath = prefs.getString(MUSIC_FILE_PATH, null);
        pcmFilePath = prefs.getString(PCM_FILE_PATH, null);
        voiceFilePath = prefs.getString(VOICE_FILE_PATH, null);
    }

    private void saveSetting() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(MUSIC_FILE_PATH, musicFilePath);
        editor.putString(PCM_FILE_PATH, pcmFilePath);
        editor.putString(VOICE_FILE_PATH, voiceFilePath);
        editor.commit();
    }


    // ビューにセットする
    private void setViewHandler() {
        // ボタン
        findViewById(R.id.play_button).setOnClickListener(this);
        findViewById(R.id.stop_button).setOnClickListener(this);
    }

    // ボタン
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.play_button:
                editorSave();
                songFilename = musicFilePath == null ? "" : new File(musicFilePath).getName();
                controlRenderer(rendererCommand.Play);
                break;
            case R.id.stop_button:
                controlRenderer(rendererCommand.Stop);
                backToEditor();
                break;
        }
    }


    // ドロワからのメニュー選択
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        File file = null;
        File directory = Environment.getExternalStorageDirectory();
        File songDirectory = (musicFilePath != null) ? new File(musicFilePath).getParentFile() : directory;

        switch(menuItem.getItemId()) {
            case R.id.nav_new_file:
                backToEditor();
                musicFilePath = new File(directory,"song.muc").getPath();
                makeNewSong();
                break;
            case R.id.nav_save_file:
                if (musicFilePath != null) file = new File(musicFilePath);
                if (file == null) file = new File(directory,"song.muc");
                callSaveDialog(directory, file, requestCodeEnum.SaveMusic);
                break;
            case R.id.nav_load_music:
                if (musicFilePath != null) file = new File(musicFilePath);
                if (file == null) file = new File(directory,"song.muc");
                callFileDialog(directory, file, requestCodeEnum.LoadMusic);
                break;
            case R.id.nav_select_pcm:
                if (pcmFilePath != null) file = new File(pcmFilePath);
                if (file == null) file = new File(songDirectory, "mucompcm.bin");
                callFileDialog(directory, file, requestCodeEnum.SelectPCM);
                break;
            case R.id.nav_select_voice:
                if (voiceFilePath != null) file = new File(voiceFilePath);
                if (file == null) file = new File(songDirectory, "voice.dat");
                callFileDialog(directory, file, requestCodeEnum.SelectVoice);
                break;
            case R.id.nav_license:
                showLicense();
                break;
            case R.id.nav_help:
                showHelp();
                break;
            case R.id.nav_clear_cache:
                backToEditor();
                clearCache();
                break;

        }

        DrawerLayout mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.closeDrawers();
        return true;
    }

    interface DialogCallback {
        public void ok(DialogInterface dialog, int id);
        public void cancel(DialogInterface dialog, int id);
    }

    private void selectDialog(String title, final DialogCallback dialogCallback) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialogCallback.ok(dialog,id);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialogCallback.cancel(dialog,id);
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // 新規作成
    private void makeNewSong() {
        selectDialog("新規作成しますか？", new DialogCallback() {
            @Override
            public void ok(DialogInterface dialog, int id) {
                makeNewSongFile();
                editorLoad();
            }

            @Override
            public void cancel(DialogInterface dialog, int id) {
            }
        });
    }

    private void makeNewSongFile() {
        File destFile = new File(getCacheDir(), SONG_MUC);
        if (destFile.exists()) destFile.delete();
        try {
            destFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // キャッシュクリア
    private void clearCache() {
        selectDialog("キャッシュをクリアしますか？", new DialogCallback() {
            @Override
            public void ok(DialogInterface dialog, int id) {
                File[] lf = getCacheDir().listFiles();
                for (File f : lf) {
                    f.delete();
                }

                makeNewSongFile();
                editorLoad();
            }

            @Override
            public void cancel(DialogInterface dialog, int id) {
            }
        });
    }

    // ファイルダイアログの表示
    void callFileDialog(File directory, File file, requestCodeEnum requestCode) {
        callFileDialog(directory, file, requestCode,false,false);
    }

    // 保存ダイアログの表示
    void callSaveDialog(File directory, File file, requestCodeEnum requestCode) {
        callFileDialog(directory, file, requestCode,true,false);
    }

    // ファイルダイアログの表示
    void callFileDialog(File directory, File file, requestCodeEnum requestCode,
                        boolean saveMode, boolean directoryMode) {
        backToEditor();
        Intent intent = new Intent(this, FileActivity.class);
        intent.setDataAndType(Uri.fromFile(file), "text/mml");
        intent.putExtra(FileActivity.SAVEMODE, saveMode);
        intent.putExtra(FileActivity.DIRMODE, directoryMode);
        startActivityForResult(intent, requestCode.ordinal());
    }

    // 呼び出したアクティビティが終了した
    protected void onActivityResult(int request, int result, Intent data) {
        if (result != RESULT_OK) return;
        dataCopy(request, data);
        editorLoad();
        saveSetting();
    }

    private void dataCopy(int request, Intent data) {
        File srcFile = new File(data.getData().getPath());
        File destFile = null;
        boolean showResult = true;

        // ファイル保存
        if (request == requestCodeEnum.SaveMusic.ordinal()) {
            destFile = srcFile;
            srcFile = new File(getCacheDir(), SONG_MUC);
            musicFilePath = destFile.getPath();
            showResult = false;
        }

        // 音楽ファイル読み出し
        if (request == requestCodeEnum.LoadMusic.ordinal()) {
            destFile = new File(getCacheDir(), SONG_MUC);
            musicFilePath = srcFile.getPath();
            showResult = false;
        }

        // PCMファイル読み出し
        if (request == requestCodeEnum.SelectPCM.ordinal()) {
            destFile = new File(getCacheDir(), srcFile.getName());
            pcmFilePath = srcFile.getPath();
        }

        // 音色ファイル読み出し
        if (request == requestCodeEnum.SelectVoice.ordinal()) {
            destFile = new File(getCacheDir(),srcFile.getName());
            voiceFilePath = srcFile.getPath();
        }

        if (destFile != null) {
            boolean Result = copyFile(srcFile, destFile);
            if (Result) {
                if (showResult) showToast(SUCCESS_COPY + srcFile.getName());
                if (request == requestCodeEnum.SaveMusic.ordinal()) {
                    showToast(SUCCESS_SAVE + destFile.getName());
                }
            }
        }
    }

    // ファイルコピー
    private boolean copyFile(File srcFile, File destFile) {
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
            showToast(FAIL_COPY + srcFile.getName());
            return false;
        }
        return true;
    }

    // フラグメント
    private void addEditorFragment(Bundle savedInstanceState) {
        if (findViewById(R.id.fragment_container) == null) return;
        if (savedInstanceState != null) return;

        EditorFragment fragment = new EditorFragment();
        fragment.setArguments(getIntent().getExtras());

        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, fragment, fragment.FRAGMENT_ID ).commit();
    }

    private void showResult() {
        String result = renderer.getResult();
        showResult("コンパイル結果",result);
    }

    private void showResult(String title, String text) {
        backToEditor();
        ResultFragment fragment = ResultFragment.newInstance(title, text);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private String readAssetText(String filename) {
        AssetManager as = getResources().getAssets();
        String result = null;
        try {
            InputStream is = as.open(filename);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            result = new String(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }


    private void showLicense() {
        String text = readAssetText("license.txt");
        showResult("license.txt", text == null ? "" : text);
    }

    private void showHelp() {
        String text = readAssetText("help.txt");
        showResult("説明表示", text == null ? "" : text);
    }

    private void backToEditor() {
        getSupportFragmentManager().popBackStack();
    }

    private EditorFragment getEditorFragment() {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(EditorFragment.FRAGMENT_ID);
        if (fragment == null || !(fragment instanceof EditorFragment)) return null;
        return (EditorFragment)fragment;
    }

    private void editorSetFile(File file) {
        EditorFragment fragment = getEditorFragment();
        if (fragment != null) fragment.setFile(file);
    }


    private void editorSave() {
        EditorFragment fragment = getEditorFragment();
        if (fragment != null) fragment.save();
    }

    private void editorLoad() {
        EditorFragment fragment = getEditorFragment();
        if (fragment != null) fragment.load();
    }

}
