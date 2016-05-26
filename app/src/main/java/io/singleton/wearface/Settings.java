package io.singleton.wearface;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class Settings implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "ACApp";

    public static final String PREF_ID = "id";
    public static final String PREF_TOKEN = "token";
    public static final String PREF_URLS = "urls";
    public static final String PREF_INDEX = "index";
    public static final String PREF_SET_MAIN = "main";


    private static Settings mInstance;
    static synchronized Settings getInstance(Context ctx) {
        if (mInstance == null) {
            mInstance = new Settings(ctx);
        }
        return mInstance;
    }

    private final Context mContext;
    private String mMyId;
    private String mConfigToken;
    private String mImageUrlList;
    private Integer mIndexInCollection;

    interface Listener {
        void onSettingsChanged();
    }

    private List<Listener> mListeners = new ArrayList<Listener>();
    void addListener(Listener listener) {
        mListeners.add(listener);
    }
    void removeListener(Listener listener) {
        mListeners.remove(listener);
    }

    private SharedPreferences mPrefs;

    private Settings(Context ctx) {
        mContext = ctx;
        updateFromSharedPrefs();
    }

    private void updateFromSharedPrefs() {
        if (mPrefs == null) {
            mPrefs = mContext.getSharedPreferences(PREF_SET_MAIN,
                    Context.MODE_MULTI_PROCESS);
        }

        mMyId = mPrefs.getString(PREF_ID, null);
        if (mMyId == null) {
            generateNewId();
        }
        mConfigToken = mPrefs.getString(PREF_TOKEN, null);
        mImageUrlList = mPrefs.getString(PREF_URLS, null);
        mIndexInCollection = mPrefs.getInt(PREF_INDEX, 0);


        mPrefs.registerOnSharedPreferenceChangeListener(this);
    }

    private void generateNewId() {
        SecureRandom rnd = new SecureRandom();
        mMyId = Long.toHexString(rnd.nextLong()) + Long.toHexString(rnd.nextLong());
        mPrefs.edit().putString(PREF_ID, mMyId).commit();
    }

    public String getLocalId() {
        return mMyId;
    }

    public String getConfigToken() {
        return mConfigToken;
    }

    public void setConfigToken(String token) {
        mConfigToken = token;
        writeToSharedPreferences();
    }

    public void setImageUrlList(String urlList) {
        Log.d(TAG, "setImageUrlList " + urlList);
        mImageUrlList = urlList;
        writeToSharedPreferences();
    }

    private void writeToSharedPreferences() {
        mPrefs.edit()
                .putString(PREF_URLS, mImageUrlList)
                .putString(PREF_TOKEN, mConfigToken)
                .putString(PREF_ID, mMyId)
                .putInt(PREF_INDEX, mIndexInCollection).apply();
        notifyListeners();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        Log.d(TAG, "onSharedPreferenceChanged " + key);
        updateFromSharedPrefs();
        notifyListeners();
    }

    private void notifyListeners() {
        for (Listener l : mListeners) {
            l.onSettingsChanged();
        }
    }

    public String getNextUrl() {
        mIndexInCollection = (mIndexInCollection + 1) % mImageUrlList.split(",").length;
        writeToSharedPreferences();
        return mImageUrlList.split(",")[mIndexInCollection];
    }

}
