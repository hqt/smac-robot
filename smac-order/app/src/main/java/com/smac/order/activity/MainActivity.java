package com.smac.order.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import com.smac.order.config.APIUtils;
import com.smac.order.config.Config;
import com.smac.order.config.NetworkUtils;
import com.smac.order.config.Pair;
import com.smac.order.config.ProcessUtils;
import com.smac.order.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Huynh Quang Thao on 10/27/15.
 */
public class MainActivity extends AppCompatActivity {

    private final int REQ_CODE_SPEECH_INPUT = 100;

    ImageView voiceRecordButton;
    TextView speechTextView;
    ListView listView;
    ItemAdapter adapter;
    Button okBtn;
    CheckBox checkbox;

    List<Pair<Integer, String>> res;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        voiceRecordButton = (ImageView) findViewById(R.id.btnSpeak);
        speechTextView = (TextView) findViewById(R.id.speech_text);
        listView = (ListView) findViewById(R.id.list_view);
        okBtn = (Button) findViewById(R.id.ok_btn);
        checkbox = (CheckBox) findViewById(R.id.test_checkbox);

        voiceRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                promptSpeechInput();
            }
        });

        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new SummitTask("aaa").execute();
            }
        });

    }

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Mời bạn yêu cầu địa điểm");
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),
                    "nhận dạng giọng nói chưa được hỗ trợ trên thiết bị hiện tại.",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if ((resultCode == RESULT_OK) && (null != data)) {


                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String speech = result.get(0);
                    speechTextView.setText(speech);

                    res = ProcessUtils.solve(speech);
                    adapter = new ItemAdapter(
                            getApplicationContext(), res);
                    listView.setAdapter(adapter);
                }
                break;
            }
        }
    }

    class SummitTask extends AsyncTask<Void, Void, Void> {

        String url;
        boolean isCheck;
        String content;

        public SummitTask(String url) {
            this.url = url;
            isCheck = checkbox.isChecked();
        }

        @Override
        protected Void doInBackground(Void... params) {
            String body = APIUtils.generateContent(res);
            Log.e("hqthao", body);

            if (isCheck) {
                content = NetworkUtils.download(Config.OFFICIAL_API, body);
            } else {
                content = NetworkUtils.download(Config.TEST_API, body);
            }

            Log.e("hqthao", content);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if ((content != null) && (content.contains("success"))) {
                Toast.makeText(getApplicationContext(), "Success", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), "Fail", Toast.LENGTH_LONG).show();
            }
        }
    }
}