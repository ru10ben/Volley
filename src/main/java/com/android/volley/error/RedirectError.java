package com.android.volley.error;

import com.android.volley.NetworkResponse;

/**
 * Indicates that there was a redirection.
 */
public class RedirectError extends VolleyError {

    public RedirectError() {
    }

    public RedirectError(final Throwable cause) {
        super(cause);
    }

    public RedirectError(final NetworkResponse response) {
        super(response);
    }
}
