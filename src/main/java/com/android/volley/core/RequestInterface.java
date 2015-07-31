package com.android.volley.core;

import android.os.Handler;
import android.os.Looper;
import com.android.volley.Listener;
import com.android.volley.Request;
import com.android.volley.error.VolleyError;

public abstract class RequestInterface<ResponseType, ResultType> {

    private Handler mHandler;
    private Listener<ResponseType> mResponseListener;
    private RequestCallback<ResponseType, ResultType> mRequestCallback;

    public RequestInterface() {
        mHandler = new Handler(Looper.getMainLooper());
    }

    public abstract Request<ResponseType> create();


    private Listener<ResponseType> mInterfaceListener = new Listener<ResponseType>() {
        @Override
        public void onSuccess(ResponseType response) {
            if (mResponseListener != null) {
                mResponseListener.onSuccess(response);
            } else if (mRequestCallback != null) {
                final ResultType resultType = mRequestCallback.doInBackground(response);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mRequestCallback.onPostExecute(resultType);
                    }
                });
            }
        }

        @Override
        public void onError(final VolleyError error) {
            if (mResponseListener != null) {
                mResponseListener.onError(error);
            } else if (mRequestCallback != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mRequestCallback.onError(error);
                    }
                });
            }
        }
    };

    public final Listener<ResponseType> useInterfaceListener() {
        return mInterfaceListener;
    }

    final void setRequestCallback(RequestCallback<ResponseType, ResultType> requestCallback) {
        mRequestCallback = requestCallback;
    }

    final void setResponseListener(Listener<ResponseType> responseListener) {
        mResponseListener = responseListener;
    }
}
