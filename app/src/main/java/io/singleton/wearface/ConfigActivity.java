package io.singleton.wearface;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.ProgressSpinner;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class ConfigActivity extends WearableActivity implements Settings.Listener {

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);
    private static final String TAG = "ACConfig";
    public static final int IMAGE_DOWNLOAD_TIMEOUT_MS = 10000;

    private BoxInsetLayout mContainerView;
    private TextView mTextView;
    private TextView mClockView;
    private ProgressSpinner mProgressView;
    private Settings mSettings;

    BroadcastReceiver mReceiver = new BroadcastReceiver() {

        private int mDownloadsRemaining = 0;

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "onReceive (" + mDownloadsRemaining + ") " + intent.toString());

            if (intent.getAction().equals(ImageDownloadingStore.ACTION_UPDATE_START)) {
                mDownloadsRemaining = 0;
            } else {
                if (intent.getAction().equals(ImageDownloadingStore.ACTION_UPDATE_COMPLETE)) {
                    mDownloadsRemaining += intent.getIntExtra(ImageDownloadingStore.EXTRA_NUM_NEW_IMAGES, 0);
                }
                if (intent.getAction().equals(ImageDownloadingStore.ACTION_IMAGE_DOWNLOADED)) {
                    mDownloadsRemaining -= 1;
                }

                if (mDownloadsRemaining == 0) {
                    Log.d(TAG, "mDownloadsRemaining == 0 -> finishing");
                    if (mReceiver != null) {
                        unregisterReceiver(mReceiver);
                        mReceiver = null;
                    }
                    finish();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mTextView = (TextView) findViewById(R.id.text);
        mClockView = (TextView) findViewById(R.id.clock);
        mProgressView = (ProgressSpinner) findViewById(R.id.progress);

        findViewById(R.id.buttonProceed).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                IntentFilter intf = new IntentFilter();
                intf.addAction(ImageDownloadingStore.ACTION_UPDATE_START);
                intf.addAction(ImageDownloadingStore.ACTION_IMAGE_DOWNLOADED);
                intf.addAction(ImageDownloadingStore.ACTION_UPDATE_COMPLETE);
                registerReceiver(mReceiver, intf);

                ((ACApplication)getApplication()).updateUrls();
                mProgressView.setVisibility(View.VISIBLE);
                mProgressView.showWithAnimation();
                findViewById(R.id.buttonProceed).setVisibility(View.GONE);

                setResult(RESULT_OK);
                timeoutToFinish(IMAGE_DOWNLOAD_TIMEOUT_MS);
            }
        });
    }

    private void timeoutToFinish(int timeoutMs) {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mReceiver != null) {
                    unregisterReceiver(mReceiver);
                    mReceiver = null;
                }
                finish();
            }
        }, timeoutMs);
    }

    @Override
    public void onResume() {
        super.onResume();
        mSettings = Settings.getInstance(this);
        mSettings.addListener(this);
        updateUi();
    }

    @Override
    public void onPause() {
        super.onPause();
        mSettings.removeListener(this);
    }

    private void updateUi() {
        if (mSettings.getConfigToken() == null) {
            mTextView.setText(getString(R.string.registering));
            ACApplication.getInstance().register();
        } else {
            String confUrl = Constants.USER_FRIENDLY_BASE_URL +
                    String.format(Constants.USER_CONFIG_PATH, mSettings.getConfigToken());
            mTextView.setText(getString(R.string.config_instructions, confUrl));
        }
    }


    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
        ((ACApplication)getApplication()).updateUrls();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
            mTextView.setTextColor(getResources().getColor(android.R.color.white));
            mClockView.setVisibility(View.VISIBLE);
            mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
        } else {
            mContainerView.setBackground(null);
            mTextView.setTextColor(getResources().getColor(android.R.color.black));
            mClockView.setVisibility(View.GONE);
        }
    }


    @Override
    public void onSettingsChanged() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateUi();
            }
        });
    }
}
