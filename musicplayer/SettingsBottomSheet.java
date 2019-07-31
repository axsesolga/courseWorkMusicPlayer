package com.olga_o.course_work.musicplayer;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;


public class SettingsBottomSheet extends BottomSheetDialogFragment {
    private BottomSheetListener mListener;
    RadioGroup radioGroup;
    RecyclerView_Adapter adapter;

    public void setAdapter(RecyclerView_Adapter adapter) {
        this.adapter = adapter;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.settings_bottom_sheet, container, false);


        RadioButton file_nameRadioButton = (RadioButton) v.findViewById(R.id.radioButtonFileName);
        file_nameRadioButton.setOnClickListener(radioButtonClickListener);
        file_nameRadioButton.setChecked(adapter.file_name);

        RadioButton titleRadioButton = (RadioButton) v.findViewById(R.id.radioButtonTitle);
        titleRadioButton.setOnClickListener(radioButtonClickListener);
        titleRadioButton.setChecked(adapter.title);

        RadioButton artistRadioButton = (RadioButton) v.findViewById(R.id.radioButtonArtist);
        artistRadioButton.setOnClickListener(radioButtonClickListener);
        artistRadioButton.setChecked(adapter.artist);

        RadioButton albumRadioButton = (RadioButton) v.findViewById(R.id.radioButtonAlbum);
        albumRadioButton.setOnClickListener(radioButtonClickListener);
        albumRadioButton.setChecked(adapter.album);


        Button button1 = v.findViewById(R.id.accept_settings);
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onButtonClicked("Button 1 clicked");
                dismiss();
            }
        });

        return v;
    }

    public interface BottomSheetListener {
        void onButtonClicked(String text);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mListener = (BottomSheetListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement BottomSheetListener");
        }
    }

    View.OnClickListener radioButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            RadioButton rb = (RadioButton) v;
            switch (rb.getId()) {
                case R.id.radioButtonFileName:
                    adapter.file_name = !adapter.file_name;
                    rb.setChecked(adapter.file_name);
                    break;
                case R.id.radioButtonTitle:
                    adapter.title = !adapter.title;
                    rb.setChecked(adapter.title);
                    break;
                case R.id.radioButtonArtist:
                    adapter.artist = !adapter.artist;
                    rb.setChecked(adapter.artist);
                    break;
                case R.id.radioButtonAlbum:
                    adapter.album = !adapter.album;
                    rb.setChecked(adapter.album);
                    break;

                default:
                    break;
            }
        }
    };

}