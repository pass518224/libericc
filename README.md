HisDroid
===
Introduction
---
Malicious or vulnerable apps are discovered every second. People may wonder what have these apps done after installation. The program behavior of such time period may be long, from weeks to months, so we call them lifetime behavior. To gather app lifetime behaviors is non-trivial. It must be coupled with user devices for runtime information, so the resource usage is the main concern. Conventional dynamic approaches cannot meet this requirement; on the other hand, traditional logging cannot produce more details information about execution. Here we present libericc, a system that can reconstruct lifetime behaviors, a record of long-term, system-wide execution, for post analysis. We make attempt to decide the runtime execution paths by recording Inter-Component Communi-cation (ICC) data. ICC is an Android-specific program statement used for lifecycle management, resource allocation, process interac-tion, etc. Even with very few runtime information, our system still can resolve the runtime value so that it can eliminate the code blocks not executed. The recorded ICC data and app binaries are the input of IDE/IFDS program analysis, and the system outputs the pruned app conservatively preserving the possible execution paths.

Build
---
```sh
./gradlew build
```
Output at build/libs/libericc.jar

Usage
---
	usage: libericc
	 -a,--apk <apkfile>            Application to analyze
	 -e,--evaluate-log <adblog>    evaluate accuracy with runtime adb log
	 -f,--output-format <format>   Output Format [none, apk, jimple] (Default: apk)
	 -i,--instrument <type>        Instrument type [none, prune, aggressive, pre-eva] (Default: prune)
	 -j,--android-jar <jarpath>    Android jar libraries
	 -l,--log <logfile>            ICC Log in JSON format
	 -n,--iccno <iccno>            only process iccno


Environment Setup
---
1. Install Android SDK; Download SDK platforms(android.jar) for each API level
1. Create an Android Emulator
1. A Modified kernel to log runtime ICCs
1. APK to analyze (must be 1 dex)

Analysis Steps
---
1. (Optional) Instrument APK to evaluate
    ```sh
    java -Xmx64G -jar libericc.jar -a <apk> -j <android-jar> -f apk -i pre-eva
    ```

1. Collect ICCs from Android emulator
    ```
    emulator -avd <devicename> -kernel zImage -gpu on
    ```

1. Start Logging
    ```
    adb shell ps > <pslog> && adb shell cat /proc/kmsg > <icclog>
    adb logcat | grep "libericc" > <evaluatelog> # only for evaluation
    ```

1. Parse ICC log
    ```
    python finder.py <icclog> --ps <pslog> --json <jsonlog>
    ```

1. Analysis
    ```
    java -Xmx64G -jar libericc.jar -l <jsonlog> -a <apk> -j <android-jar> -f apk -i prune -e <evaluatelog>
    ```

Other Usage
---
Life cycle entry coverage
```sh
java -cp libericc.jar libericc.evaluatecoverage.Main <apk> <android-jar>