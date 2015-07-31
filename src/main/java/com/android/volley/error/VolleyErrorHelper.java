package com.android.volley.error;

import org.json.JSONException;
import org.json.JSONObject;
import android.content.Context;
import com.android.volley.NetworkResponse;

public class VolleyErrorHelper {

    private static final String NO_INTERNET = "No internet";
	
	/**
	 * Determines whether the error is related to network
	 * 
	 * @param error
	 * @return
	 */
	private static boolean isNetworkProblem(Object error) {
		return (error instanceof NetworkError) || (error instanceof NoConnectionError);
	}

	/**
	 * Determines whether the error is related to server
	 * 
	 * @param error
	 * @return
	 */
	private static boolean isServerProblem(Object error) {
		return (error instanceof ServerError) || (error instanceof AuthFailureError);
	}

	/**
     * Returns appropriate message which is to be displayed to the user against
     * the specified error object.
     * 
     * @param error
     * @param context
     * @return
     */
    public static String getMessage(Object error, Context context) {
        if (error instanceof TimeoutError) {
            return NO_INTERNET;
        } else if (isServerProblem(error)) {
            return handleServerError(error, context);
        } else if (isNetworkProblem(error)) {
            return NO_INTERNET;
        }
        return NO_INTERNET;
    }
    
	/**
	 * Handles the server error, tries to determine whether to show a stock
	 * message or to show a message retrieved from the server.
	 * 
	 * @param err
	 * @param context
	 * @return
	 */
	private static String handleServerError(Object err, Context context) {
		VolleyError error = (VolleyError) err;

		NetworkResponse response = error.networkResponse;

		if (response != null) {
			switch (response.statusCode) {
			case 404:
			case 422:
			case 401:
			    try {
    			    // server might return error like this { "error": "Some error occured" }
    			    JSONObject json = new JSONObject(new String(response.data));
    			    if (json != null && json.has("error")) {
    			        return json.getString("error");
    			    }
    				return error.getMessage();
			    } catch (JSONException e) {
			        e.printStackTrace();
			    }
			default:
				return NO_INTERNET;
			}
		}
		return NO_INTERNET;
	}
}