import glob

java_path = glob.glob("**/MainActivity.java", recursive=True)[0]
print("Java:", java_path)

with open(java_path, encoding='utf-8') as f:
    j = f.read()

old = "private static final int CAMERA_PERMISSION_REQUEST = 10;"
new = "private static final int CAMERA_PERMISSION_REQUEST = 10;\n    private static final int LOCATION_PERMISSION_REQUEST = 11;"

c = j.count(old)
print("Anchor found:", c, "times")
if c == 1:
    j = j.replace(old, new)
    with open(java_path, 'w', encoding='utf-8') as f:
        f.write(j)
    print("OK: constant added and saved.")
elif c == 0:
    print("Anchor not found - checking if constant already exists...")
    if "LOCATION_PERMISSION_REQUEST" in j:
        print("LOCATION_PERMISSION_REQUEST already exists somewhere in the file. No change made. Send this output back.")
    else:
        print("FAIL: anchor not found and constant missing. Nothing written. Send this output back.")
else:
    print("FAIL: anchor found more than once. Nothing written. Send this output back.")
