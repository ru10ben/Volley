package com.android.volley.request;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import com.android.volley.Listener;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;

/**
 * A canned request for retrieving the response body at a given URL as a String.
 */
public class DownloadRequest extends Request<String> {
	private final String mDownloadPath;

	/**
	 * Creates a new request with the given method.
	 *
	 * @param url
	 *            URL to fetch the string at
	 * @param listener
	 *            Listener to receive the String response
	 */
	public DownloadRequest(String url, String filePath, Listener<String> listener) {
		super(Method.GET, url, listener);
		mDownloadPath = filePath;
	}

	@Override
	protected Response<String> parseNetworkResponse(NetworkResponse response) {
		String parsed = "";
		try {
			byte[] data = response.data;
			// convert array of bytes into file
			FileOutputStream fileOuputStream = new FileOutputStream(mDownloadPath, true);
			fileOuputStream.write(data);
			fileOuputStream.flush();
			fileOuputStream.close();
			parsed = mDownloadPath;
		} catch (UnsupportedEncodingException e) {
			parsed = new String(response.data);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return Response.success(parsed, response);
	}
}