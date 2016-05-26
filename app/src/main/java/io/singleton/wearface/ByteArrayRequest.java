package io.singleton.wearface;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;

public class ByteArrayRequest extends Request<byte[]> {

    private final Listener mListener;

    interface Listener {
        void onResponse(byte[] data);
    }

    public ByteArrayRequest(int method, String url, Listener listener, Response.ErrorListener errorListener) {
        super(method, url, errorListener);
        mListener = listener;
    }

    @Override
    protected Response parseNetworkResponse(NetworkResponse response) {
        return Response.success(response.data, HttpHeaderParser.parseCacheHeaders(response));
    }

    @Override
    protected void deliverResponse(byte[] response) {
        mListener.onResponse(response);
    }
}
