package com.ltlfarm.farmdesk;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.Manifest;
import android.content.pm.PackageManager;
import android.webkit.JavascriptInterface;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.core.content.FileProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String TAG = "GoatDesk";
    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private Uri cameraImageUri;
    private File cameraImageFile;
    private String cameraJsCallback;
    private static final int FILE_CHOOSER_REQUEST = 1;
    private static final int CAMERA_BRIDGE_REQUEST = 3;
    private static final int CAMERA_PERMISSION_REQUEST = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setDatabasePath(getFilesDir().getAbsolutePath() + "/databases");
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                    FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = callback;
                Intent intent = params.createIntent();
                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) { ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST); }
        webView.loadUrl("file:///android_asset/farmdesk.html");
    }

    public class AndroidBridge {
        @JavascriptInterface
        public void takePhoto(String jsCallback) {
            Log.d(TAG, "takePhoto called, callback=" + jsCallback);
            cameraJsCallback = jsCallback;
            runOnUiThread(() -> {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) { ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST); return; }
                try {
                    cameraImageFile = createImageFile();
                    Log.d(TAG, "imageFile=" + cameraImageFile.getAbsolutePath());
                    cameraImageUri = FileProvider.getUriForFile(
                        MainActivity.this,
                        "com.burdenenterprises.goatdesk.fileprovider",
                        cameraImageFile
                    );
                    Log.d(TAG, "cameraUri=" + cameraImageUri);
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
                    startActivityForResult(intent, CAMERA_BRIDGE_REQUEST);
                    Log.d(TAG, "startActivityForResult fired");
                } catch (Exception e) {
                    Log.e(TAG, "takePhoto exception: " + e.getMessage());
                    cameraJsCallback = null;
                }
            });
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("GOATDESK_" + timeStamp, ".jpg", storageDir);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult requestCode=" + requestCode + " resultCode=" + resultCode);

        if (requestCode == CAMERA_BRIDGE_REQUEST) {
            boolean fileReady = (cameraImageFile != null
                                 && cameraImageFile.exists()
                                 && cameraImageFile.length() > 0);
            Log.d(TAG, "fileReady=" + fileReady
                + (cameraImageFile != null ? " size=" + cameraImageFile.length() : " file=null"));

            if (fileReady) {
                try {
                    Bitmap bmp = BitmapFactory.decodeFile(cameraImageFile.getAbsolutePath());
                    Log.d(TAG, "bitmap=" + (bmp != null ? bmp.getWidth() + "x" + bmp.getHeight() : "null"));
                    if (bmp != null) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bmp.compress(Bitmap.CompressFormat.JPEG, 85, baos);
                        String b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
                        final String dataUrl = "data:image/jpeg;base64," + b64;
                        final String cb = cameraJsCallback != null ? cameraJsCallback : "onAndroidPhoto";
                        Log.d(TAG, "calling JS " + cb + " b64len=" + b64.length());
                        webView.post(() ->
                            webView.evaluateJavascript(cb + "('" + dataUrl + "')", null));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "encode exception: " + e.getMessage());
                }
            }
            cameraImageUri = null;
            cameraImageFile = null;
            cameraJsCallback = null;
            return;
        }

        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (filePathCallback == null) return;
            Uri[] results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        webView.evaluateJavascript("(function(){ var v=document.getElementById('photoViewer'); if(v && v.classList.contains('open')){v.classList.remove('open');return true;} if(window.history.length>1){history.back();return true;} return false; })()", val -> { if(!"true".equals(val)) runOnUiThread(()->super.onBackPressed()); });
    }
}
