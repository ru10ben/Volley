
package com.android.volley.core;

import com.android.volley.error.VolleyError;

public abstract class RequestCallback<ResponseType, ResultType> {

    public abstract ResultType doInBackground(ResponseType response);

    
    public abstract void onPostExecute(ResultType result);

    
    public void onError(VolleyError error) {
    }

}
