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

package org.openpanodroid;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;

import junit.framework.Assert;

import org.openpanodroid.panoutils.android.CubicPanoNative;
import org.openpanodroid.panoutils.android.CubicPanoNative.TextureFaces;
import org.openpanodroid.task.BitmapDownloadTask;
import org.openpanodroid.task.PanoConversionTask;

import java.io.File;

public class PanoViewerActivity extends Activity {
    public static final String LOG_TAG = FlickrSearchActivity.class.getSimpleName();
    public static final java.lang.String INTENT_PROVIDE_IMAGES = "images_provided";

    public static int IMG_QUALITY = 85;

    public static final String ORIGINAL_BITMAP_LINK_KEY = "origBitmapLink"; // http...
    public static final String ORIGINAL_BITMAP_KEY = "origBitmap";
    public static final String FRONT_BITMAP_KEY = "frontBitmap";
    public static final String BACK_BITMAP_KEY = "backBitmap";
    public static final String TOP_BITMAP_KEY = "topBitmap";
    public static final String BOTTOM_BITMAP_KEY = "bottomBitmap";
    public static final String LEFT_BITMAP_KEY = "leftBitmap";
    public static final String RIGHT_BITMAP_KEY = "rightBitmap";

    protected Uri panoUri;

    private PanodroidGLView glView = null;

    public Bitmap pano = null;
    public CubicPanoNative cubicPano = null;

    private BitmapDownloadTask panoDownloadTask = null;

    private PanoConversionTask panoConversionTask = null;

    private boolean stateSaved;

    public class ClickListenerErrorDialog implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            // After an (fatal) error dialog, the activity will be dismissed.
            finish();
        }
    }


    private Bitmap createPurgableBitmap(File source) {
//            ByteArrayOutputStream os = new ByteArrayOutputStream();
//            original.compress(Bitmap.CompressFormat.JPEG, IMG_QUALITY, os);
//            byte[] imgDataCompressed = os.toByteArray();

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inDither = false;
        // When we run out of memory, the decompressed bitmap can be purged.
        // If it is re-accessed, the byte array will be decompressed again.
        // This allows us to handle larger or more bitmaps.
        options.inPurgeable = true;
        // Original byte array will not be altered anymore. --> Can make a shallow reference.
        options.inInputShareable = true;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeFile(source.getAbsolutePath(), options);
        return bitmap;
    }

    public void panoDisplaySetupFinished() {
    }

    public PanoViewerActivity() {
    }

    protected void setupImageInfo() {
        Intent intent = getIntent();
        panoUri = intent.getData();
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(LOG_TAG, "Creating");

        super.onCreate(savedInstanceState);

        stateSaved = false;
        boolean withUrl = false;

        if (getIntent().getExtras() != null && getIntent().getExtras().containsKey(INTENT_PROVIDE_IMAGES)) {
            if (getIntent().getExtras().containsKey(ORIGINAL_BITMAP_KEY)) {
                File f = (File) getIntent().getExtras().getSerializable(ORIGINAL_BITMAP_KEY);
                processImageFromIntent(f);
            } else if (getIntent().getExtras().containsKey(ORIGINAL_BITMAP_LINK_KEY)) {
                String link = getIntent().getExtras().getString(ORIGINAL_BITMAP_LINK_KEY);
                ;
                panoUri = Uri.parse(link);
                withUrl = true;
            } else {
                processImagesFromIntent();
            }
        } else {
            setupImageInfo();
            withUrl = true;
        }

        if (!withUrl)
            return;

        Bitmap front, back, top, bottom, left, right;
        front = back = top = bottom = left = right = null;
        if (savedInstanceState != null) {
            Parcelable parcelData;
            parcelData = savedInstanceState.getParcelable(FRONT_BITMAP_KEY);
            if (parcelData != null) {
                Assert.assertTrue(parcelData instanceof Bitmap);
                front = (Bitmap) parcelData;
            }

            parcelData = savedInstanceState.getParcelable(BACK_BITMAP_KEY);
            if (parcelData != null) {
                Assert.assertTrue(parcelData instanceof Bitmap);
                back = (Bitmap) parcelData;
            }

            parcelData = savedInstanceState.getParcelable(TOP_BITMAP_KEY);
            if (parcelData != null) {
                Assert.assertTrue(parcelData instanceof Bitmap);
                top = (Bitmap) parcelData;
            }

            parcelData = savedInstanceState.getParcelable(BOTTOM_BITMAP_KEY);
            if (parcelData != null) {
                Assert.assertTrue(parcelData instanceof Bitmap);
                bottom = (Bitmap) parcelData;
            }

            parcelData = savedInstanceState.getParcelable(LEFT_BITMAP_KEY);
            if (parcelData != null) {
                Assert.assertTrue(parcelData instanceof Bitmap);
                left = (Bitmap) parcelData;
            }

            parcelData = savedInstanceState.getParcelable(RIGHT_BITMAP_KEY);
            if (parcelData != null) {
                Assert.assertTrue(parcelData instanceof Bitmap);
                right = (Bitmap) parcelData;
            }
        }

        if (front == null || back == null || top == null || bottom == null || left == null || right == null) {
            if (front != null) {
                front.recycle();
                front = null;
            }
            if (back != null) {
                back.recycle();
                back = null;
            }
            if (top != null) {
                top.recycle();
                top = null;
            }
            if (bottom != null) {
                bottom.recycle();
                bottom = null;
            }
            if (left != null) {
                left.recycle();
                left = null;
            }
            if (right != null) {
                right.recycle();
                right = null;
            }

            downloadPano();
            //else
            // Toast.makeText(this, "Image data corrupted!", Toast.LENGTH_LONG).show();
        } else {
            cubicPano = new CubicPanoNative(front, back, top, bottom, left, right);
            setupOpenGLView();
        }
    }

    private void processImageFromIntent(final File f) {
        pano = createPurgableBitmap(f);
        convertCubicPano(pano);
    }

    private void processImagesFromIntent() {
        File f = (File) getIntent().getExtras().getSerializable(FRONT_BITMAP_KEY);
        File back = (File) getIntent().getExtras().getSerializable(BACK_BITMAP_KEY);
        File bottom = (File) getIntent().getExtras().getSerializable(BOTTOM_BITMAP_KEY);
        File top = (File) getIntent().getExtras().getSerializable(TOP_BITMAP_KEY);
        File right = (File) getIntent().getExtras().getSerializable(RIGHT_BITMAP_KEY);
        File left = (File) getIntent().getExtras().getSerializable(LEFT_BITMAP_KEY);

        Bitmap bFront = createPurgableBitmap(f);
        Bitmap bback = createPurgableBitmap(back);
        Bitmap bbottom = createPurgableBitmap(bottom);
        Bitmap bright = createPurgableBitmap(right);
        Bitmap bleft = createPurgableBitmap(left);
        Bitmap btop = createPurgableBitmap(top);


        cubicPano = new CubicPanoNative(bFront, bback, btop, bbottom, bleft, bright);
        setupOpenGLView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.panoviewer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == R.id.about) {
            showAbout();
            return true;
        } else {
            Assert.fail();
            return false;
        }
    }

    private void showAbout() {
        Resources resources = getResources();
        CharSequence aboutText = resources.getText(R.string.aboutText);
        UIUtilities.showTextInfo(this, getString(R.string.about), aboutText);
    }

    @Override
    protected void onPause() {
        Log.i(LOG_TAG, "Pausing");

        super.onPause();

        if (glView != null) {
            glView.onPause();
        }
    }

    @Override
    protected void onResume() {
        Log.i(LOG_TAG, "Resuming");

        super.onResume();

        if (glView != null) {
            glView.onResume();
        }
    }

    public void setupOpenGLView() {
        Assert.assertTrue(cubicPano != null);
        glView = new PanodroidGLView(this, cubicPano);
        setContentView(glView);
    }

    private void downloadPano() {
        Log.i(LOG_TAG, "Downloading panorama ...");

        // We might need a lot of memory in the next time (depending on the image size).
        System.gc();

        // TODO: Remove after tests.
        // ****
        /*
    	try {
			//panoUrl = new URL("http://192.168.2.2/~duerrfk/foo/pano-6000.jpg");
			panoUrl = new URL("http://192.168.2.2/~duerrfk/foo/pano-3000.jpg");
			//panoUrl = new URL("http://192.168.2.2/~duerrfk/foo/pano-1024.jpg");
		} catch (MalformedURLException e1) {
			Assert.fail();
		}
    	// ****
    	*/

        Assert.assertTrue(panoUri != null);
        Log.i(LOG_TAG, "Panorama Uri: " + panoUri.toString());

        panoDownloadTask = new BitmapDownloadTask(this);
        panoDownloadTask.execute(panoUri);
    }

    public void convertCubicPano(final Bitmap pano) {
        Assert.assertTrue(pano != null);

        Log.i(LOG_TAG, "Converting panorama ...");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String str = prefs.getString("textureSize", "");
        int maxTextureSize = GlobalConstants.DEFAULT_MAX_TEXTURE_SIZE;
        if (!str.equals("")) {
            try {
                maxTextureSize = Integer.parseInt(str);
            } catch (NumberFormatException ex) {
                maxTextureSize = GlobalConstants.DEFAULT_MAX_TEXTURE_SIZE;
            }
        }

        // On the one hand, we don't want to waste memory for textures whose resolution
        // is too large for the device. On the other hand, we want to have a resolution
        // that is high enough to give us good quality on any device. However, we don't
        // know the resolution of the GLView a priori, and it could be resized later.
        // Therefore, we use the display size to calculate the optimal texture size.
        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();
        int maxDisplaySize = width > height ? width : height;

        int optimalTextureSize = getOptimalFaceSize(maxDisplaySize, pano.getWidth(), GlobalConstants.DEFAULT_FOV_DEG);
        int textureSize = toPowerOfTwo(optimalTextureSize);
        textureSize = textureSize <= maxTextureSize ? textureSize : maxTextureSize;

        Log.i(LOG_TAG, "Texture size: " + textureSize + " (optimal size was " + optimalTextureSize + ")");

        panoConversionTask = new PanoConversionTask(this, textureSize);
        panoConversionTask.execute(pano);
    }

    public static int toPowerOfTwo(int number) {
        int n_2 = 1;

        while (n_2 < number) {
            n_2 *= 2;
        }

        return n_2;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.i(LOG_TAG, "Saving instance state.");

        super.onSaveInstanceState(outState);

        if (cubicPano != null) {
            outState.putParcelable(FRONT_BITMAP_KEY, cubicPano.getFace(TextureFaces.front));
            outState.putParcelable(BACK_BITMAP_KEY, cubicPano.getFace(TextureFaces.back));
            outState.putParcelable(TOP_BITMAP_KEY, cubicPano.getFace(TextureFaces.top));
            outState.putParcelable(BOTTOM_BITMAP_KEY, cubicPano.getFace(TextureFaces.bottom));
            outState.putParcelable(LEFT_BITMAP_KEY, cubicPano.getFace(TextureFaces.left));
            outState.putParcelable(RIGHT_BITMAP_KEY, cubicPano.getFace(TextureFaces.right));
            stateSaved = true;
        }
    }

    @Override
    protected void onDestroy() {
        Log.i(LOG_TAG, "Destroyed");

        if (panoDownloadTask != null) {
            // An AsyncTask will continue, also if the activity has been already destroyed.
            // Therefore, we signal it that the activity was destroyed. The AsyncTask
            // will cancel itself at the earliest possible time and avoid any further action.
            // (In particular, UI actions would be dangerous since the main activity is gone.)
            panoDownloadTask.destroy();
        }

        if (panoConversionTask != null) {
            panoConversionTask.destroy();
        }

        // We might have used a lot of memory.
        // Explicitly free it now.

        if (cubicPano != null && !stateSaved) {
            cubicPano.getFace(TextureFaces.front).recycle();
            cubicPano.getFace(TextureFaces.back).recycle();
            cubicPano.getFace(TextureFaces.top).recycle();
            cubicPano.getFace(TextureFaces.bottom).recycle();
            cubicPano.getFace(TextureFaces.left).recycle();
            cubicPano.getFace(TextureFaces.right).recycle();
            cubicPano = null;
            System.gc();
        }

        super.onDestroy();
    }

    public static int getOptimalFaceSize(int screenSize, int equirectImgSize, double hfov) {
        // Maximum possible size with this equirectangular image.
        int maxFaceSize = (int) (0.25 * equirectImgSize * 90.0 / hfov + 0.5);

        // Optimal face size for this screen size.
        int optimalFaceSize = (int) (90.0 / hfov * screenSize + 0.5);

        return (optimalFaceSize < maxFaceSize ? optimalFaceSize : maxFaceSize);
    }

    public static int getOptimalEquirectSize(int screenSize, double hfov) {
        int optimalEquirectSize = (int) (360.0 / hfov * screenSize + 0.5);
        return optimalEquirectSize;
    }
}
