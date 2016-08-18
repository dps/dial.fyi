package io.singleton.wearface;

import android.app.Application;
import android.graphics.Bitmap;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;


public class ACApplication extends Application {

    public static final int INITIAL_TIMEOUT_MS = 120000;
    public static final int MAX_NUM_RETRIES = 1;
    public static final float BACKOFF_MULTIPLIER = 1f;
    public static final int REGISTER_INITIAL_TIMEOUT_MS = 60000;
    private static ACApplication sInstance;
    private RequestQueue mRequestQueue;
    private ImageDownloadingStore mImageStore;

    private String TAG = "ACApp";
    private Settings mSettings;

    @Override
    public void onCreate() {
        super.onCreate();
        mRequestQueue = Volley.newRequestQueue(this);
        sInstance = this;
        mSettings = Settings.getInstance(getApplicationContext());
        mImageStore = new ImageDownloadingStore(this);

        ACJobService.scheduleUpdateJob(this);
    }

    public synchronized static ACApplication getInstance() {
        return sInstance;
    }

    public RequestQueue getRequestQueue() {
        return mRequestQueue;
    }

    private Response.ErrorListener mErrorListener = new Response.ErrorListener(){
        @Override
        public void onErrorResponse(VolleyError error) {
            Log.d(TAG, "onErrorResponse " + error);
        }
    };

    public void register() {

        String when = Long.toString(System.currentTimeMillis());

        JSONObject json = new JSONObject();
        try {
            json.put("id", mSettings.getLocalId());
            json.put("when", when);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                Constants.getRegisterUrl(),
                json,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        String token = null;
                        try {
                            token = response.getString("token");
                            mSettings.setConfigToken(token);
                        } catch (JSONException e) {
                            e.printStackTrace();
                            // TODO: maybe retry
                        }

                    }
                },
                mErrorListener
        );
        request.setRetryPolicy(new DefaultRetryPolicy(REGISTER_INITIAL_TIMEOUT_MS, MAX_NUM_RETRIES, BACKOFF_MULTIPLIER));
        ACApplication.getInstance().getRequestQueue().add(request);

    }

    public void updateUrls() {

        String when = Long.toString(System.currentTimeMillis());

        JSONObject body = new JSONObject();
        try {
            body.put("id", mSettings.getLocalId());
            body.put("token", mSettings.getConfigToken());
            body.put("when", when);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject json = body;

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                Constants.getImageListUrl(mSettings.getConfigToken()),
                json,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        Log.d(TAG, response.toString());
                        String urls = null;
                        try {
                            urls = response.getString("urls");
                            mSettings.setImageUrlList(urls);
                            mImageStore.onUrlsUpdated(urls.split(","));
                        } catch (JSONException e) {
                            e.printStackTrace();
                            // TODO: maybe retry
                        }

                    }
                },
                mErrorListener
        );
        request.setRetryPolicy(new DefaultRetryPolicy(INITIAL_TIMEOUT_MS, MAX_NUM_RETRIES, BACKOFF_MULTIPLIER));
        ACApplication.getInstance().getRequestQueue().add(request);
    }

    public Bitmap getNextBitmap() {
        return mImageStore.getBitmapIfCached(mSettings.getNextUrl());
    }
}
