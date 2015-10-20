package com.lhu.samplenetwork;

import android.os.Handler;
import android.os.Looper;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Tacademy on 2015-10-20.
 */
public class NetworkManager {
    private static NetworkManager instance;

    public static NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }

        return instance;
    }

    ThreadPoolExecutor mExecutor;
    Handler mHandler = new Handler(Looper.getMainLooper());

    private NetworkManager() {
        mExecutor = new ThreadPoolExecutor(5, 64, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
    }

    public interface OnResultListener<T> {
        public void onSuccess(NetworkRequest<T> request, T result);
        public void onFail(NetworkRequest<T> request, int code);
    }

    public void getNaverMovies(NetworkRequest<NaverMovies> request, OnResultListener<NaverMovies> listener) {
        mExecutor.execute(new NetworkProcess<NaverMovies>(request, listener));
    }

    class NetworkProcess<T> implements Runnable {
        NetworkRequest<T> request;
        OnResultListener<T> listener;

        public NetworkProcess(NetworkRequest<T> request, OnResultListener<T> listener) {
            this.request = request;
            this.listener = listener;
        }

        @Override
        public void run() {
            try {
                URL url = request.getURL();
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                request.setRequestMethod(conn);
                request.setOutput(conn);
                int code = conn.getResponseCode();
                if (code == HttpURLConnection.HTTP_OK) {
                    InputStream is = conn.getInputStream();
                    final T object = request.parsing(is);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onSuccess(request, object);
                        }
                    });
                    return;
                }
           } catch (MalformedURLException e) {
            e.printStackTrace();
           } catch (IOException e) {
               e.printStackTrace();
            }
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onFail(request, -1);
                }
            });
        }
    }
}