package com.example.mohitkumar.isgw;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Created by mohitkumar on 23/12/16.
 */

public class MySingleton {

    private static MySingleton mInstance;
    private RequestQueue requestQueue;
    private static Context mctx;

    public MySingleton(Context context)
    {
        mctx = context;
        requestQueue = getRequestQueue();
    }


    public static synchronized MySingleton getInstance(Context context) {

        if(mInstance == null) {
            mInstance = new MySingleton(context);
        }
        return mInstance;
    }

    public RequestQueue getRequestQueue() {

        if(requestQueue == null) {
            requestQueue = Volley.newRequestQueue(mctx.getApplicationContext());
        }
        return requestQueue;
    }

    public <T>void addToRequestQueue(Request<T> request)
    {
        requestQueue.add(request);
    }

}
