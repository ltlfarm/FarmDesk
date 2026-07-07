package com.ltlfarm.farmdesk;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.GeolocationPermissions;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private Uri cameraImageUri;
    private File cameraImageFile;
    private String cameraJsCallback;
    private static final int FILE_CHOOSER_REQUEST = 1;
    private static final int CAMERA_BRIDGE_REQUEST = 3;
    private static final int CAMERA_PERMISSION_REQUEST = 10;
    private static final int LOCATION_PERMISSION_REQUEST = 11;

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
        settings.setGeolocationEnabled(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                    FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = callback;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/xml", "application/xml", "text/plain", "*/*"});
                try {
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST);
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
        });

        // Request camera permission on startup so it's ready when needed
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        }

        // Request location permission on startup
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);
        }

        webView.loadUrl("file:///android_asset/farmdesk.html");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera ready", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Camera permission denied - tap Camera to try again", Toast.LENGTH_LONG).show();
            }
        }
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location ready", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Location permission denied - map wont auto-center", Toast.LENGTH_LONG).show();
            }
        }
    }

    public class AndroidBridge {

        // ===== SAVE XML — write to public Downloads folder =====
        @JavascriptInterface
        public void saveXML(String content, String filename) {
            runOnUiThread(() -> {
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        android.content.ContentValues values = new android.content.ContentValues();
                        values.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, filename);
                        values.put(android.provider.MediaStore.Downloads.MIME_TYPE, "text/xml");
                        values.put(android.provider.MediaStore.Downloads.IS_PENDING, 1);
                        android.net.Uri collection = android.provider.MediaStore.Downloads.getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY);
                        android.net.Uri item = getContentResolver().insert(collection, values);
                        try (java.io.OutputStream os = getContentResolver().openOutputStream(item)) {
                            os.write(content.getBytes("UTF-8"));
                        }
                        values.clear();
                        values.put(android.provider.MediaStore.Downloads.IS_PENDING, 0);
                        getContentResolver().update(item, values, null, null);
                    } else {
                        File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        downloads.mkdirs();
                        File outFile = new File(downloads, filename);
                        FileWriter fw = new FileWriter(outFile);
                        fw.write(content);
                        fw.close();
                    }
                    Toast.makeText(MainActivity.this, "Saved to Downloads: " + filename, Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Save error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }

        // ===== SHARE XML — Standalone mode export =====
        @JavascriptInterface
        public void shareXML(String content, String filename) {
            runOnUiThread(() -> {
                try {
                    File shareFile = new File(getCacheDir(), filename);
                    FileWriter fw = new FileWriter(shareFile);
                    fw.write(content);
                    fw.close();
                    Uri fileUri = FileProvider.getUriForFile(
                        MainActivity.this,
                        "com.ltlfarm.farmdesk.fileprovider",
                        shareFile
                    );
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("text/xml");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(shareIntent, "Share Farm Log"));
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Share error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }

        // ===== CAMERA BRIDGE =====
        @JavascriptInterface
        public void takePhoto(String jsCallback) {
            cameraJsCallback = jsCallback;
            runOnUiThread(() -> {
                // Check permission before launching
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
                    Toast.makeText(MainActivity.this,
                        "Grant camera permission then tap Camera again", Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    cameraImageFile = createImageFile();
                    cameraImageUri = FileProvider.getUriForFile(
                        MainActivity.this,
                        "com.ltlfarm.farmdesk.fileprovider",
                        cameraImageFile
                    );
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
                    startActivityForResult(intent, CAMERA_BRIDGE_REQUEST);
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Camera error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    cameraJsCallback = null;
                }
            });
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("FARMDESK_" + timeStamp, ".jpg", storageDir);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == CAMERA_BRIDGE_REQUEST) {
            boolean fileReady = (cameraImageFile != null
                                 && cameraImageFile.exists()
                                 && cameraImageFile.length() > 0);
            if (fileReady) {
                try {
                    Bitmap bmp = BitmapFactory.decodeFile(cameraImageFile.getAbsolutePath());
                    if (bmp != null) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bmp.compress(Bitmap.CompressFormat.JPEG, 85, baos);
                        String b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
                        final String dataUrl = "data:image/jpeg;base64," + b64;
                        final String cb = cameraJsCallback != null ? cameraJsCallback : "onAndroidPhoto";
                        webView.post(() ->
                            webView.evaluateJavascript(cb + "('" + dataUrl + "')", null));
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Photo encode error: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
