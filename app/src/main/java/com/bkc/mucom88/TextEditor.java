package com.bkc.mucom88;

import android.content.Context;
import android.widget.EditText;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class TextEditor {
    public static final String CHARSET_SJIS = "Windows-31j";

    File textFile = null;
    EditText et = null;
    String Text = null;

    TextEditor(File file,EditText editText) {
        textFile = file;
        et = editText;
    }

    // 編集ファイルの読み込み
    public void loadFile(EditText editText) {
        et = editText;
        loadFile();
    }

    // 編集ファイルの読み込み
    public void loadFile()
    {
        if (et == null) return;
        if (textFile == null || !textFile.exists()) return;

        int len = (int)textFile.length();
        InputStream is = null;

        try {
            is = new FileInputStream(textFile);
            Text = loadDataFromIS(is, len);
            is.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        if (Text == null) return;
        et.setText(Text);
    }

    // 編集ファイルの保存
    public void saveFile() {
        if (et == null) return;
        String text = et.getText().toString();

        // LF -> CRLF, remove NULL
        text = text.replace("\n","\r\n");
        text = text.replace("\0","");

        OutputStreamWriter osw;
        try {
            FileOutputStream os = new FileOutputStream(textFile);
            osw = new OutputStreamWriter(os, CHARSET_SJIS);
            osw.write(text);
            osw.close();
            os.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // データを入力ストリームより読み込む
    private String loadDataFromIS(InputStream is, int len) {
        try {
            InputStreamReader in = new InputStreamReader(is,CHARSET_SJIS);

            int pos = 0;
            char [] data = new char[(int) len];

            while(len > 0) {
                int bl = in.read(data, pos, (int) len);
                if (bl < 0) break;
                len -= bl;
                pos += bl;
            }
            in.close();

            String text = String.valueOf(data);
            // CRLF -> LF, CR -> LF, remove NULL
            text = text.replace("\r\n","\n");
            text = text.replace("\r","\n");
            text = text.replace("\0","");

            return text;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


}
