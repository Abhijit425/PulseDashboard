#!/bin/bash
set -e
P="$(cd "$(dirname "$0")/../app" && pwd)"; J=/usr/lib/android-sdk/platforms/android-23/android.jar; B="$(dirname "$P")/build"
rm -rf $B; mkdir -p $B/gen $B/obj $B/dex
aapt package -f -m -J $B/gen -M $P/AndroidManifest.xml -S $P/res -I $J -F $B/app.unaligned.apk
javac -nowarn -Xlint:-options -source 8 -target 8 -encoding UTF-8 -bootclasspath $J -d $B/obj $(find $P/src $B/gen -name '*.java')
dalvik-exchange --dex --output=$B/dex/classes.dex $B/obj
(cd $B/dex && aapt add $B/app.unaligned.apk classes.dex >/dev/null)
zipalign -f -p 4 $B/app.unaligned.apk $B/app.aligned.apk
[ -f "${KEYSTORE:-$HOME/.pulse.keystore}" ] || keytool -genkeypair -keystore "${KEYSTORE:-$HOME/.pulse.keystore}" -storepass pulse123 -keypass pulse123 -alias pulse -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Abhijit, O=Pulse, C=IN" 2>/dev/null
apksigner sign --ks "${KEYSTORE:-$HOME/.pulse.keystore}" --ks-pass pass:pulse123 --key-pass pass:pulse123 --ks-key-alias pulse --out $B/PulseDashboard.apk $B/app.aligned.apk
echo BUILD_OK; ls -la $B/PulseDashboard.apk
