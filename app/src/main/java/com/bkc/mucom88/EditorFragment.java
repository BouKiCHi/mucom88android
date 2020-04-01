package com.bkc.mucom88;

import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.File;

public class EditorFragment extends Fragment {

    TextEditor textEditor = null;
    private View rootView = null;

    public static final String FRAGMENT_ID = "editorFragment";

    public static EditorFragment newInstance() {
        return new EditorFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        container.removeAllViews();

        rootView = inflater.inflate(R.layout.editor_fragment, container, false);
        EditText et = (EditText) rootView.findViewById(R.id.editText);
        if (textEditor != null) textEditor.loadFile(et);
        return rootView;
    }

    public void save() {
        if (textEditor == null) return;
        textEditor.saveFile();
    }

    public void load() {
        if (textEditor == null) return;
        textEditor.loadFile();
    }

    public void setFile(File file) {
        EditText et = (EditText) rootView.findViewById(R.id.editText);
        textEditor = new TextEditor(file, et);
    }
}
