import glob

java_path = glob.glob("**/MainActivity.java", recursive=True)[0]
print("Java:", java_path)

with open(java_path, encoding='utf-8') as f:
    j = f.read()

edits = []

edits.append(("import android.webkit.WebChromeClient;",
    "import android.webkit.WebChromeClient;\nimport android.webkit.GeolocationPermissions;",
    "already imported"))

edits.append(("settings.setCacheMode(WebSettings.LOAD_DEFAULT);",
    "settings.setCacheMode(WebSettings.LOAD_DEFAULT);\n        settings.setGeolocationEnabled(true);",
    "already enabled"))

edits.append(("                return true;\n            }\n        });",
    "                return true;\n            }\n\n            @Override\n            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {\n                callback.invoke(origin, true, false);\n            }\n        });",
    "already overridden"))

edits.append(('webView.loadUrl("file:///android_asset/farmdesk.html");',
    '// Request location permission on startup\n        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)\n                != PackageManager.PERMISSION_GRANTED) {\n            ActivityCompat.requestPermissions(this,\n                new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST);\n        }\n\n        webView.loadUrl("file:///android_asset/farmdesk.html");',
    "already requesting"))

ok = 0
skip = 0
fail = 0
for i in range(len(edits)):
    anchor, replacement, already_msg = edits[i]
    if replacement in j:
        print("SKIP edit " + str(i+1) + "/4: " + already_msg + " (already present)")
        skip = skip + 1
        continue
    c = j.count(anchor)
    if c == 1:
        j = j.replace(anchor, replacement)
        ok = ok + 1
        print("OK edit " + str(i+1) + "/4")
    else:
        print("FAIL edit " + str(i+1) + "/4: anchor found " + str(c) + " times, expected 1")
        fail = fail + 1

if ok > 0:
    with open(java_path, 'w', encoding='utf-8') as f:
        f.write(j)
    print("")
    print(str(ok) + " edit(s) applied and saved. " + str(skip) + " already present. " + str(fail) + " failed.")
else:
    print("")
    print("No new edits applied. " + str(skip) + " already present, " + str(fail) + " failed. " + ("Nothing to do." if fail == 0 else "Send this output back."))
