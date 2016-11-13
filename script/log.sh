#!/bin/bash

source config.sh

function success {
	echo $LOGGED >> $result
	echo $LOGSUCCESS >> $result
}

function fail {
	echo $LOGGED >> $result
	echo $LOGFAILED >> $result
}

for apk in $INSAPPDIR/*.apk; do
	apkname=`basename $apk`
	apkbasename=`basename $apk .apk`
	result=$RESULTDIR/$apkbasename

	# instrument success and not logged
	if [[ $(grep "$INSSUCCESS" $result) != "" && $(grep "$LOGGED" $result) == "" ]]; then
		echo $id $(date +%H:%M:%S) $apk

		# copy avd
		rm -rf $AVDPATH/${TMPAVDNAME}.avd $AVDPATH/${TMPAVDNAME}.ini
		cp -r $AVDPATH/${AVDNAME}.avd $AVDPATH/${TMPAVDNAME}.avd
		cat $AVDPATH/${AVDNAME}.ini | sed "s/${AVDNAME}/${TMPAVDNAME}/g" > $AVDPATH/${TMPAVDNAME}.ini

		# install
		adb kill-server
		killall adb
		emulator -avd $TMPAVDNAME -kernel zImage -gpu on > /dev/null 2>&1 &
		emupid=$!
		sleep 150
		echo install $apk
		timeout 300 adb install $apk
		installresult=$?
		echo kill emulator
		kill -15 $emupid
		sleep 10
		if [[ $installresult == "124" ]]; then
			fail
			continue
		fi

		# run
		emulator -avd $TMPAVDNAME -kernel zImage -gpu on > /dev/null 2>&1 &
		emupid=$!
		sleep 150
		echo "start logging"
		adb logcat | grep "hisdroid" > $LOGDIR/${apkbasename}.logcat &
		adb shell ps > $LOGDIR/${apkbasename}.ps && adb shell cat /proc/kmsg > $LOGDIR/${apkbasename}.log &
		echo "start monkey"
		timeout 500 adb shell monkey -p $apkbasename --throttle 300 --ignore-timeouts 1000 > $LOGDIR/${apkbasename}.monkey 2>&1
		monkeyresult=$?

		echo "kill emulator"
		kill -15 $emupid
		sleep 10
		rm -rf $AVDPATH/${TMPAVDNAME}.avd $AVDPATH/${TMPAVDNAME}.ini

		# result
		if [[ $(grep "Monkey aborted due to error" $LOGDIR/${apkbasename}.monkey) != "" ]]; then
			fail
		elif [[ $(grep "monkey aborted." $LOGDIR/${apkbasename}.monkey) != "" ]]; then
			fail
		elif [[ $monkeyresult == "124" ]]; then
			fail
		else
			success
		fi
	fi
done
