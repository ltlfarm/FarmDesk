import glob

manifest_path = glob.glob("**/AndroidManifest.xml", recursive=True)[0]
java_path = glob.glob("**/MainActivity.java", recursive=True)[0]
print("Manifest:", manifest_path)
print("Java:", java_path)

with open(manifest_path) as f:
    m = f.read()
anchor = '<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="29" />'
new_perms = anchor + '\n    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />\n    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />'
count = m.count(anchor)
if count == 1:
    m = m.replace(anchor, new_perms)
    with open(manifest_path, 'w') as f:
        f.write(m)
    print("OK: AndroidManifest.xml updated")
else:
    print("FAIL manifest: anchor found " + str(count) + " times, expected 1")

with open(java_path) as f:
    j = f.read()

edits = []

edits.append((
    "import android.webkit.WebChromeClient;",
    "import android.webkit.WebChromeClient;\nimport android.webkit.GeolocationPermissions;"
))

edits.append((
    "private static final int CAMERA_PERMISSION_REQUEST = 10;",
    "private static final int CAMERA_PERMISSION_REQUEST = 10;\n    private static final int LOCATION_PERMISSION_REQUEST = 11;"
))

edits.append((
    "settings.setCacheMode(WebSettings.LOAD_DEFAULT);",
    "settings.setCacheMode(WebSettings.LOAD_DEFAULT);\n        settings.setGeolocationEnabled(true);"
))

old4 = "                return true;\n            }\n        });"
new4 = "                return true;\n            }\n\n            @Override\n            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {\n                callback.invoke(origin, true, false);\n            }\n        });"
edits.append((old4, new4))

old5 = 'webView.loadUrl("file:///android_asset/farmdesk.html");'
new5 = '// Request location permission on startup\n        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)\n                != PackageManager.PERMISSION_GRANTED) {\n            ActivityCompat.requestPermissions(this,\n                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);\n        }\n\n        webView.loadUrl("file:///android_asset/farmdesk.html");'
edits.append((old5, new5))

old6 = '            } else {\n                Toast.makeText(this, "Camera permission denied - tap Camera to try again", Toast.LENGTH_LONG).show();\n            }\n        }\n    }'
new6 = '            } else {\n                Toast.makeText(this, "Camera permission denied - tap Camera to try again", Toast.LENGTH_LONG).show();\n            }\n        }\n        if (requestCode == LOCATION_PERMISSION_REQUEST) {\n            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {\n                Toast.makeText(this, "Location ready", Toast.LENGTH_SHORT).show();\n            } else {\n                Toast.makeText(this, "Location permission denied - map wont auto-center", Toast.LENGTH_LONG).show();\n            }\n        }\n    }'
edits.append((old6, new6))

ok = 0
for i in range(len(edits)):
    anchor, replacement = edits[i]
    c = j.count(anchor)
    if c == 1:
        j = j.replace(anchor, replacement)
        ok = ok + 1
        print("OK edit " + str(i+1) + "/6")
    else:
        print("FAIL edit " + str(i+1) + "/6: anchor found " + str(c) + " times, expected 1")

if ok == len(edits):
    with open(java_path, 'w') as f:
        f.write(j)
    print("")
    print("All 6 edits applied and saved.")
else:
    print("")
    print("Only " + str(ok) + "/6 edits matched. NOTHING WRITTEN to java file. Send this output back.")
