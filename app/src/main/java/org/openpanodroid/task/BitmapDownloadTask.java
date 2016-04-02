package org.openpanodroid.task;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import junit.framework.Assert;

import org.openpanodroid.PanoViewerActivity;
import org.openpanodroid.R;
import org.openpanodroid.RunnableWithBitmap;
import org.openpanodroid.UIUtilities;
import org.openpanodroid.ioutils.BitmapDecoderThread;
import org.openpanodroid.ioutils.Pipe;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/*
 * Copyright 2012 Frank Dürr
 *
 * This file is part of OpenPanodroid.
 *
 * OpenPanodroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenPanodroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenPanodroid.  If not, see <http://www.gnu.org/licenses/>.
 */
public class BitmapDownloadTask extends AsyncTask<Uri, Integer, Bitmap> {

    private final static int BUFFER_SIZE = 8192;
    private RunnableWithBitmap onFinish = null;
    private Context context = null;
    private PanoViewerActivity panoViewerActivity = null;
    private InputStream downloadStream = null;
    private BitmapDecoderThread bitmapDecoder = null;
    private ProgressDialog waitDialog = null;
    private boolean destroyed = false;
    private String dialogMessage = "Loading panorama image...";

    public BitmapDownloadTask(PanoViewerActivity panoViewerActivity) {
        this.panoViewerActivity = panoViewerActivity;
        context = panoViewerActivity;
    }

    /**
     * @param context
     */
    public BitmapDownloadTask(Context context, @NonNull RunnableWithBitmap onFinish) {
        this.context = context;
        this.onFinish = onFinish;
    }

    @Override
    protected void onPreExecute() {
        waitDialog = new ProgressDialog(panoViewerActivity != null ? panoViewerActivity : context);
        waitDialog.setMessage(dialogMessage);
        waitDialog.setCancelable(true);
        waitDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        waitDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                cancel(true);
                if (downloadStream != null) {
                    // Force download to end.
                    try {
                        downloadStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        waitDialog.show();
    }

    @Override
    protected Bitmap doInBackground(Uri... params) {
        Assert.assertTrue(params.length > 0);
        Uri uri = params[0];
        int contentLength = -1;
        byte[] buffer = new byte[BUFFER_SIZE];
        URLConnection connection;
        URL url;
        System.gc();

        try {
            url = new URL(uri.toString());
        } catch (MalformedURLException ex) {
            url = null;
        }

        Pipe pipe = new Pipe(BUFFER_SIZE);
        OutputStream pipeOutput = pipe.getOutputStream();
        InputStream pipeInput = pipe.getInputStream();

        Bitmap bitmap = null;

        try {
            if (url != null) {
                // We try to open an URL connection since this gives us a content length
                // (in contrast to the generic way of opening an URI).
                connection = url.openConnection();
                downloadStream = new BufferedInputStream(connection.getInputStream());
                contentLength = connection.getContentLength();
            } else {
                // Try generic way to open URI.
                downloadStream = context.getContentResolver().openInputStream(uri);
            }

            if (contentLength > 0) {
                waitDialog.setMax(contentLength);
            } else {
                waitDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            }

            bitmapDecoder = new BitmapDecoderThread(pipeInput);
            bitmapDecoder.start();

            int currentLength = 0;
            int readCnt;

            while (!isCancelled() && (readCnt = downloadStream.read(buffer)) != -1) {
                pipeOutput.write(buffer, 0, readCnt);
                currentLength += readCnt;

                if (contentLength > 0) {
                    publishProgress(currentLength);
                }
            }
        } catch (Exception e) {
            Log.e(PanoViewerActivity.LOG_TAG, "Failed to load image: " + e.getMessage());
        } finally {
            if (pipeOutput != null) {
                try {
                    pipeOutput.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (bitmapDecoder != null) {
            try {
                bitmapDecoder.join();
                bitmap = bitmapDecoder.bitmap;
            } catch (InterruptedException e) {
                Log.e(PanoViewerActivity.LOG_TAG, "Download taks interrupted: " + e.getMessage());
            }
        }

        return bitmap;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        Assert.assertTrue(progress.length > 0);
        int p = progress[0];
        waitDialog.setProgress(p);
    }

    synchronized boolean isDestroyed() {
        return destroyed;
    }

    public synchronized void destroy() {
        destroyed = true;
        cancel(true);
    }

    @Override
    protected void onCancelled() {
        if (isDestroyed()) {
            return;
        }

        waitDialog.dismiss();
        if (panoViewerActivity != null)
            panoViewerActivity.finish();
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        if (isDestroyed()) {
            return;
        }

        waitDialog.dismiss();
        if (panoViewerActivity != null) {
            if (result == null) {
                String msg = panoViewerActivity.getString(R.string.loadingPanoFailed);
                if (bitmapDecoder != null && bitmapDecoder.errorMsg != null) {
                    msg += " (" + bitmapDecoder.errorMsg + ")";
                }
                UIUtilities.showAlert(panoViewerActivity, null, msg, new ClickListenerErrorDialog());
            } else if (result.getWidth() != 2 * result.getHeight()) {
                String msg = panoViewerActivity.getString(R.string.invalidPanoImage);
                UIUtilities.showAlert(panoViewerActivity, null, msg, new ClickListenerErrorDialog());
            } else {
                panoViewerActivity.pano = result;
                panoViewerActivity.convertCubicPano(panoViewerActivity.pano);
            }
        } else {
            if (result != null)
                onFinish.run(result);
            else {
                String msg = "Could not load panorama image.";
                if (bitmapDecoder != null && bitmapDecoder.errorMsg != null) {
                    msg += " (" + bitmapDecoder.errorMsg + ")";
                }
                Log.e("BitmapDownloadTask", msg);
            }
        }
    }

    public class ClickListenerErrorDialog implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            // After an (fatal) error dialog, the activity will be dismissed.
            panoViewerActivity.finish();
        }
    }
}
