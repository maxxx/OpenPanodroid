This fork represents original OpenPanodroid but moved to gradle (android studio) + updated to api level 22 and type changed to library, you can call panorama viewer from intent with various params. Also you can use pano convert task.

Can be included as Gradle dependency (look how to work with jitpack - https://jitpack.io/):
compile 'com.github.maxxx:OpenPanodroid:1.3'

To convert panoram image into cube sides use this snippet as example (you must decode ишеьфз with ARGB_8888 !):
    private void convertPanoram(final String filePath) {
        Bitmap pano = loadBitmap(filePath);
        final String dir = Environment.getExternalStorageDirectory() + "/yourApp/";

        new PanoConversionTask(getActivity(), pano, dir, new Runnable() {
            @Override
            public void run() {
                openPanoram();
            }
        }).execute(pano);
    }
    public static Bitmap loadBitmap(String imgPath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        try {
            return BitmapFactory.decodeFile(imgPath, options);
        } catch (Exception var3) {
            var3.printStackTrace();
            return null;
        }
    }


To call panoram viewer use this snippet as example:
    private void openPanoram(String panoImagePath) {
        Intent intent = new Intent(getActivity(), PanoViewerActivity.class);
        Bundle b = new Bundle();

        b.putBoolean(PanoViewerActivity.INTENT_PROVIDE_IMAGES, true);
        b.putSerializable(PanoViewerActivity.ORIGINAL_BITMAP_KEY, new File(panoImagePath));

        intent.putExtras(b);
        startActivity(intent);
    }
    -OR with already converted files -
    private void openPanoram(String dir) {
        Intent intent = new Intent(getActivity(), PanoViewerActivity.class);
        Bundle b = new Bundle();

        b.putBoolean(PanoViewerActivity.INTENT_PROVIDE_IMAGES, true);
        b.putSerializable(PanoViewerActivity.FRONT_BITMAP_KEY, new File(dir + "_f.jpg"));
        b.putSerializable(PanoViewerActivity.BACK_BITMAP_KEY, new File(dir + "_b.jpg"));
        b.putSerializable(PanoViewerActivity.TOP_BITMAP_KEY, new File(dir + "_t.jpg"));
        b.putSerializable(PanoViewerActivity.BOTTOM_BITMAP_KEY, new File(dir + "_d.jpg"));
        b.putSerializable(PanoViewerActivity.RIGHT_BITMAP_KEY, new File(dir + "_r.jpg"));
        b.putSerializable(PanoViewerActivity.LEFT_BITMAP_KEY, new File(dir + "_l.jpg"));

        intent.putExtras(b);
        startActivity(intent);
    }

Don't forget to register this activity in your manifest
<activity
        android:name="org.openpanodroid.PanoViewerActivity"/>
---------
OpenPanodroid is a panorama image viewer for Google's Android platform.

OpenPanodroid was originally developed by Frank Dürr 
(email: frank.d.durr@googlemail.com, homepage: http://www.frank-durr.de/)

The latest version of OpenPanodroid is available from GitHub:

https://github.com/duerrfk/OpenPanodroid

OpenPanodroid is released under the GNU General Public License (GNU GPLv3), 
to enable the community to extend OpenPanodroid and use it as basis for
further applications needing a high quality panorama image viewer.

= Flickr Image Import =

In order to access panorama images from Flickr, an API key is required. 
This key is not included with the open source version of OpenPandroid!
You can apply for a key here:

  http://www.flickr.com/services/apps/create/apply

This key has to be imported into the class 

  org.openpanodroid.flickrapi.FlickrConstants, 

attribute 

  API_KEY_PANODROID.

= Calling OpenPanodroid from other Apps =

OpenPanodroid can be used by other applications to display panorama images. 
To call the panorama viewer activity, you have to supply an URI pointing at 
the panorama image (remote file URL (http://), local file path (file://), 
content URI (content://)). The following code example shows how to invoke 
OpenPanodroid:

Uri panoUri = Uri.parse("http://www.frank-durr.de/foo/pano-6000.jpg");
   
ComponentName panoViewerComponent = 
    new ComponentName("org.openpanodroid", 
    "org.openpanodroid.PanoViewerActivity");
    	
Intent intent = new Intent(Intent.ACTION_VIEW);
intent.setComponent(panoViewerComponent);
intent.setData(panoUri);
    	
startActivity(intent);

