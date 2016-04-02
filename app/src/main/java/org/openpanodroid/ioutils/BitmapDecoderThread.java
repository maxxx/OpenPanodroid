package org.openpanodroid.ioutils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.openpanodroid.BitmapUtilities;
import org.openpanodroid.PanoViewerActivity;

import java.io.IOException;
import java.io.InputStream;

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
public class BitmapDecoderThread extends Thread {
    public Bitmap bitmap;
    public String errorMsg;

    private InputStream is;

    public BitmapDecoderThread(InputStream is) {
        bitmap = null;
        this.is = is;
    }

    @Override
    public void run() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inDither = false;
        options.inScaled = false;
        //options.inPreferredConfig = Bitmap.Config.RGB_565;
        BitmapUtilities.setHiddenNativeAllocField(options);

        //ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        //int largeMem = activityManager.getLargeMemoryClass();
        //int regularMem = activityManager.getMemoryClass();

        try {
            bitmap = BitmapFactory.decodeStream(is, null, options);
        } catch (OutOfMemoryError e) {
            Log.e(PanoViewerActivity.LOG_TAG, "Failed to decode image: " + e.getMessage());
            errorMsg = "Out of memory";
        } catch (Exception e) {
            Log.e(PanoViewerActivity.LOG_TAG, "Failed to decode image: " + e.getMessage());
        } finally {
            try {
                is.close();
            } catch (IOException e) {
            }
            if (bitmap == null && errorMsg == null) {
                Log.e(PanoViewerActivity.LOG_TAG, "Failed to decode image");
                errorMsg = "Image could not be decoded.";
            }
        }
    }
}
