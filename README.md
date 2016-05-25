HisDroid
===
Introduction
---

Build
---
gradle version >= 2.12
```sh
gradle build
```
Output at build/libs/hisdroid.jar

Usage
---
    usage: hisdroid
     -a,--apk <apkfile>            Application to analyze
     -f,--output-format <format>   Output Format [none, apk, jimple] (Default: apk)
     -i,--instrument <type>        Instrument type [none, prune, stats] (Default: prune)
     -j,--android-jar <jarpath>    Android jar libraries
     -l,--log <logfile>            ICC Log in JSON format
