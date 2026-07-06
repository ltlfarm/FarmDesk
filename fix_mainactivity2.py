import glob

java_path = glob.glob("**/MainActivity.java", recursive=True)[0]
print("Java:", java_path)

with open(java_path, encoding='utf-8') as f:
    j = f.read()

old6 = '            } else {\n                Toast.makeText(this, "Camera permission denied \u2014 tap Camera to try again", Toast.LENGTH_LONG).show();\n            }\n        }\n    }'
new6 = '            } else {\n                Toast.makeText(this, "Camera permission denied - tap Camera to try again", Toast.LENGTH_LONG).show();\n            }\n        }\n        if (requestCode == LOCATION_PERMISSION_REQUEST) {\n            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {\n                Toast.makeText(this, "Location ready", Toast.LENGTH_SHORT).show();\n            } else {\n                Toast.makeText(this, "Location permission denied - map wont auto-center", Toast.LENGTH_LONG).show();\n            }\n        }\n    }'

c = j.count(old6)
print("Anchor found:", c, "times")
if c == 1:
    j = j.replace(old6, new6)
    with open(java_path, 'w', encoding='utf-8') as f:
        f.write(j)
    print("OK: edit 6/6 applied and saved.")
elif c == 0:
    print("FAIL: anchor still not found. Nothing written. Send this output back.")
else:
    print("FAIL: anchor found more than once (" + str(c) + "). Nothing written. Send this output back.")
