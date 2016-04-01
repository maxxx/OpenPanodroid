package org.openpanodroid.task;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import junit.framework.Assert;
import org.openpanodroid.GlobalConstants;
import org.openpanodroid.PanoViewerActivity;
import org.openpanodroid.R;
import org.openpanodroid.UIUtilities;
import org.openpanodroid.panoutils.android.CubicPanoNative;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Maxim Smirnov on 31.03.16.
 * Use PanoConversionTask(Context context, Bitmap pano) from where you need to just convertation
 */
public class PanoConversionTask extends AsyncTask<Bitmap, Integer, CubicPanoNative> {

    private Runnable onFinishRun = null;
    private String pathToSave = "";
    private Context context;
    private PanoViewerActivity panoViewerActivity = null;
    private ProgressDialog waitDialog = null;
    private int textureSize = 0;
    private boolean destroyed = false;

    private String dialogMessage = "Converting panorama image...";

    public PanoConversionTask(final PanoViewerActivity panoViewerActivity, int textureSize) {
        this.panoViewerActivity = panoViewerActivity;
        this.textureSize = textureSize;
        dialogMessage = panoViewerActivity.getString(R.string.convertingPanoImage);
    }

    /**
     * @param context
     * @param pano       - original image which should be converted
     * @param pathToSave - where to save converted images
     * @param onFinish   - custom runnable executed on convertation finish
     */
    public PanoConversionTask(@NonNull Context context,
                              @NonNull Bitmap pano,
                              @NonNull String pathToSave,
                              @Nullable Runnable onFinish) {
        this.context = context;
        this.pathToSave = pathToSave;
        this.onFinishRun = onFinish;
        int maxTextureSize = GlobalConstants.DEFAULT_MAX_TEXTURE_SIZE;

        // On the one hand, we don't want to waste memory for textures whose resolution
        // is too large for the device. On the other hand, we want to have a resolution
        // that is high enough to give us good quality on any device. However, we don't
        // know the resolution of the GLView a priori, and it could be resized later.
        // Therefore, we use the display size to calculate the optimal texture size.
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();
        int maxDisplaySize = width > height ? width : height;

        int optimalTextureSize = PanoViewerActivity.getOptimalFaceSize(maxDisplaySize, pano.getWidth(), GlobalConstants
                .DEFAULT_FOV_DEG);
        textureSize = PanoViewerActivity.toPowerOfTwo(optimalTextureSize);
        textureSize = textureSize <= maxTextureSize ? textureSize : maxTextureSize;

        Log.i("PanoConversionTask", "Texture size: " + textureSize + " (optimal size was " + optimalTextureSize + ")");
    }

    @Override
    protected void onPreExecute() {
        waitDialog = new ProgressDialog(panoViewerActivity != null ? panoViewerActivity : context);
        waitDialog.setMessage(dialogMessage);
        waitDialog.setCancelable(true);
        waitDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(final DialogInterface dialogInterface) {
                cancel(true);
            }
        });
        waitDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        waitDialog.setMax(6);
        waitDialog.show();
    }

    @Override
    protected CubicPanoNative doInBackground(Bitmap... params) {
        Bitmap bmp;
        final Bitmap pano = params[0];

        if (isCancelled()) {
            return null;
        }

        bmp = CubicPanoNative.getCubeSide(pano, CubicPanoNative.TextureFaces.front, textureSize);
        if (bmp == null) {
            Log.e("PanoConversionTask", "doInBackground: bmp null for front");
            return null;
        }
        Bitmap front = createPurgableBitmap(bmp);
        bmp.recycle();
        publishProgress(1);
        if (!pathToSave.isEmpty()) {
            saveBitmap(front, pathToSave + "_f.jpg");
        }

        if (isCancelled()) {
            return null;
        }

        bmp = CubicPanoNative.getCubeSide(pano, CubicPanoNative.TextureFaces.back, textureSize);
        if (bmp == null) {
            Log.e("PanoConversionTask", "doInBackground: bmp null for back");
            return null;
        }
        Bitmap back = createPurgableBitmap(bmp);
        bmp.recycle();
        publishProgress(2);
        if (!pathToSave.isEmpty()) {
            saveBitmap(back, pathToSave + "_b.jpg"); // _back
        }

        if (isCancelled()) {
            return null;
        }

        bmp = CubicPanoNative.getCubeSide(pano, CubicPanoNative.TextureFaces.top, textureSize);
        if (bmp == null) {
            Log.e("PanoConversionTask", "doInBackground: bmp null for top");
            return null;
        }
        Bitmap top = createPurgableBitmap(bmp);
        bmp.recycle();
        publishProgress(3);
        if (!pathToSave.isEmpty()) {
            saveBitmap(top, pathToSave + "_t.jpg");
        }

        if (isCancelled()) {
            return null;
        }

        bmp = CubicPanoNative.getCubeSide(pano, CubicPanoNative.TextureFaces.bottom, textureSize);
        if (bmp == null) {
            Log.e("PanoConversionTask", "doInBackground: bmp null for bottom");
            return null;
        }
        Bitmap bottom = createPurgableBitmap(bmp);
        bmp.recycle();
        publishProgress(4);
        if (!pathToSave.isEmpty()) {
            saveBitmap(bottom, pathToSave + "_d.jpg"); // _down
        }

        if (isCancelled()) {
            return null;
        }

        bmp = CubicPanoNative.getCubeSide(pano, CubicPanoNative.TextureFaces.right, textureSize);
        if (bmp == null) {
            Log.e("PanoConversionTask", "doInBackground: bmp null for right");
            return null;
        }
        Bitmap right = createPurgableBitmap(bmp);
        bmp.recycle();
        publishProgress(5);
        if (!pathToSave.isEmpty()) {
            saveBitmap(right, pathToSave + "_r.jpg");
        }

        if (isCancelled()) {
            return null;
        }

        bmp = CubicPanoNative.getCubeSide(pano, CubicPanoNative.TextureFaces.left, textureSize);
        if (bmp == null) {
            Log.e("PanoConversionTask", "doInBackground: bmp null for left");
            return null;
        }
        Bitmap left = createPurgableBitmap(bmp);
        bmp.recycle();
        publishProgress(6);
        if (!pathToSave.isEmpty()) {
            saveBitmap(left, pathToSave + "_l.jpg");
        }

        CubicPanoNative cubic = new CubicPanoNative(front, back, top, bottom, left, right);

        return cubic;
    }

    private Bitmap createPurgableBitmap(Bitmap original) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        original.compress(Bitmap.CompressFormat.JPEG, PanoViewerActivity.IMG_QUALITY, os);
        byte[] imgDataCompressed = os.toByteArray();

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inDither = false;
        // When we run out of memory, the decompressed bitmap can be purged.
        // If it is re-accessed, the byte array will be decompressed again.
        // This allows us to handle larger or more bitmaps.
        options.inPurgeable = true;
        // Original byte array will not be altered anymore. --> Can make a shallow reference.
        options.inInputShareable = true;
        Bitmap compressedBitmap =
                BitmapFactory.decodeByteArray(imgDataCompressed, 0, imgDataCompressed.length, options);

        return compressedBitmap;
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
        if (panoViewerActivity != null) {
            panoViewerActivity.pano.recycle();
            panoViewerActivity.finish();
        }
    }

    @Override
    protected void onPostExecute(CubicPanoNative result) {
        if (isDestroyed()) {
            return;
        }

        waitDialog.dismiss();

        if (result == null) {
            Log.e("PanoConversionTask", "onPostExecute: result null!");
        }

        if (onFinishRun != null) {
            onFinishRun.run();
        }

        if (panoViewerActivity != null) {
            panoViewerActivity.pano.recycle();
            panoViewerActivity.pano = null;
            if (result == null) {
                UIUtilities.showAlert(panoViewerActivity,
                        null,
                        dialogMessage,
                        new ClickListenerErrorDialog());
            } else {
                panoViewerActivity.cubicPano = result;
                panoViewerActivity.setupOpenGLView();
                panoViewerActivity.panoDisplaySetupFinished();
            }
        }
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        Assert.assertTrue(progress.length > 0);
        int p = progress[0];
        waitDialog.setProgress(p);
    }


    public void setDialogMessage(final String dialogMessage) {
        this.dialogMessage = dialogMessage;
    }

    public class ClickListenerErrorDialog implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            // After an (fatal) error dialog, the activity will be dismissed.
            panoViewerActivity.finish();
        }
    }

    public static void saveBitmap(Bitmap result, String path) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(path);
            result.compress(Bitmap.CompressFormat.JPEG, 85, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
