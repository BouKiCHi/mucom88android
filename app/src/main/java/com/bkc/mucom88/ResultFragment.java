package com.bkc.mucom88;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class ResultFragment extends Fragment {
    private View rootView = null;

    private static final String RESULT_TITLE = "resultTitle";
    private static final String RESULT_TEXT = "resultText";
    private String resultTitle;
    private String resultText;

    // 必須
    public ResultFragment() {}

    public static ResultFragment newInstance(String title, String text) {
        ResultFragment fragment = new ResultFragment();
        Bundle args = new Bundle();
        args.putString(RESULT_TITLE, title);
        args.putString(RESULT_TEXT, text);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            resultTitle = getArguments().getString(RESULT_TITLE);
            resultText = getArguments().getString(RESULT_TEXT);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_result, container, false);
        TextView tv = (TextView) rootView.findViewById(R.id.resultTitle);
        tv.setText(resultTitle);
        tv = (TextView) rootView.findViewById(R.id.resultText);
        tv.setText(resultText);
        return rootView;
    }
}
