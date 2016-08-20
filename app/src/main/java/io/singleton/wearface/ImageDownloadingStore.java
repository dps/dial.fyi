package io.singleton.wearface;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ImageDownloadingStore {

    private static final String TAG = "ACIDS";
    public static final String ACTION_UPDATE_START = "io.singleton.watchface.UPDATE_START";
    public static final String ACTION_IMAGE_DOWNLOADED = "io.singleton.watchface.IMAGE_DOWNLOADED";
    public static final String ACTION_UPDATE_COMPLETE = "io.singleton.watchface.UPDATE_COMPLETE";
    public static final String THUMBNAILER_IMG_URL_PREFIX = "http://thumbor.us.davidsingleton.org/unsafe/400x400/";
    public static final String GOOGLE_PHOTOS_URL_HOST = "lh3.googleusercontent.com";
    public static final String EXTRA_FILENAME_HASH = "hash";
    public static final String EXTRA_NUM_NEW_IMAGES = "new";
    public static final int INITIAL_TIMEOUT_MS = 120000;
    public static final int MAX_NUM_RETRIES = 2;
    public static final float BACKOFF_MULTIPLIER = 1f;
    public static final String FILE_PREFIX = "ac-";
    private final File mCacheDir;
    private Context mContext;
    private MessageDigest mDigester;
    private Map<String, String> mUrlHashes = new HashMap<String, String>();

    public ImageDownloadingStore(Context context) {
        mContext = context;
        mCacheDir = mContext.getCacheDir();
    }

    public void onUrlsUpdated(String[] urls) {
        mUrlHashes.clear();
        for (String url : urls) {
            mUrlHashes.put(urlHash(url), url);
            Log.d(TAG, "oUU " + url);
        }

        deleteOldFiles();
        fetchMissingFiles();
    }

    private synchronized String urlHash(String url) {
        try {
            if (mDigester == null) {
                mDigester = MessageDigest.getInstance("MD5");
            }
            mDigester.reset();
            mDigester.update(url.getBytes());
            return FILE_PREFIX + (new BigInteger(1, mDigester.digest())).toString(16);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Digest algorithm not found", e);
        }
        return null;
    }

    private void deleteOldFiles() {
        for (File f : mCacheDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return (filename.startsWith(FILE_PREFIX));
            }
        })) {
            if (!mUrlHashes.containsKey(f.getName())) {
                Log.d(TAG, "deleting " + f.getName());
                f.delete();
            }
        }
    }

    private void fetchMissingFiles() {
        int missing = 0;
        List<String> current = Arrays.asList(mCacheDir.list());
        broadcastUpdateStarted();

        for (Map.Entry<String, String> e : mUrlHashes.entrySet()) {
            if (!current.contains(e.getKey())) {
                missing++;
                downloadUrlToFile(e.getValue(), e.getKey());
            }
        }

        broadcastUpdateComplete(missing);
    }

    private String resolutionAdjustUrl(String url) {
        // The thumbor thumbnailing service doesn't work with Google Photos URLs
        if (url.contains(GOOGLE_PHOTOS_URL_HOST)) {
            return url;
        }
        return THUMBNAILER_IMG_URL_PREFIX + url;
    }

    private void downloadUrlToFile(String url, final String filename) {
        ByteArrayRequest request = new ByteArrayRequest(Request.Method.GET,
                resolutionAdjustUrl(url), new ByteArrayRequest.Listener() {
            @Override
            public void onResponse(byte[] data) {
                File out = new File(mCacheDir, filename);
                try {
                    FileOutputStream fos = new FileOutputStream(out);
                    fos.write(data);
                    fos.close();
                    Log.d(TAG, "Wrote " + data.length + " bytes to " + filename);

                    broadcastFileDownloaded(filename);
                } catch (FileNotFoundException e) {
                    Log.e(TAG, "File Not Found ", e);
                } catch (IOException e) {
                    Log.e(TAG, "IOException ", e);
                }
            }
        },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "onErrorResponse (" + filename + ") " + error);
                    }
                });

        request.setRetryPolicy(new DefaultRetryPolicy(INITIAL_TIMEOUT_MS, MAX_NUM_RETRIES, BACKOFF_MULTIPLIER));
        ACApplication.getInstance().getRequestQueue().add(request);
    }

    private void broadcastFileDownloaded(String filename) {
        Intent broadcast = new Intent();
        broadcast.setAction(ACTION_IMAGE_DOWNLOADED);
        broadcast.putExtra(EXTRA_FILENAME_HASH, filename);
        mContext.sendBroadcast(broadcast);
    }

    private void broadcastUpdateStarted() {
        Intent broadcast = new Intent();
        broadcast.setAction(ACTION_UPDATE_START);
        mContext.sendBroadcast(broadcast);
    }

    private void broadcastUpdateComplete(int numNewImages) {
        Intent broadcast = new Intent();
        broadcast.setAction(ACTION_UPDATE_COMPLETE);
        broadcast.putExtra(EXTRA_NUM_NEW_IMAGES, numNewImages);
        mContext.sendBroadcast(broadcast);
    }

    Bitmap getBitmapIfCached(String url) {
        String name = urlHash(url);
        File inf = new File(mCacheDir, name);
        try {
            FileInputStream fis = new FileInputStream(inf);
            byte[] data = toByteArray(fis);
            return parseBitmapFromBytes(data);
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    private Bitmap parseBitmapFromBytes(byte[] data) {
        BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
        Bitmap bitmap = null;
        decodeOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
        return bitmap;
    }

    private static byte[] toByteArray(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int read;
        byte[] data = new byte[8096];
        while ((read = is.read(data, 0, data.length)) >= 0) {
            buffer.write(data, 0, read);
        }
        buffer.flush();
        return buffer.toByteArray();
    }
}
