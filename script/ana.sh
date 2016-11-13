#!/bin/bash

source config.sh

id='0'

for apk in $APPDIR/*.apk; do
	apkname=`basename $apk`
	apkbasename=`basename $apk .apk`
	result=$RESULTDIR/$apkbasename
	id=`expr $id + 1`

	# log success and not analyzed
	if [[ $(grep "$LOGSUCCESS" $result) != "" && $(grep "$ANALYZED" $result) == "" ]]; then
		echo $id $(date +%H:%M:%S) $apk
		
		# parse log
		echo "parse log"
		pwd=$PWD
		cd $FINDERPATH
		python finder.py $pwd/$LOGDIR/${apkbasename}.log --ps $pwd/$LOGDIR/${apkbasename}.ps --json $pwd/$LOGDIR/${apkbasename}.json > /dev/null 2> /dev/null
		cd $pwd

		# analysis
		echo "analysis"
		scp $apk $LOGDIR/${apkbasename}.json $LOGDIR/${apkbasename}.logcat $REMOTEUSER@$REMOTEHOST:$REMOTEDIR
		ssh $REMOTEUSER@$REMOTEHOST "cd $REMOTEDIR; java -Xmx64G -jar ${JAR} -l ${apkbasename}.json -a $apkname -j $ANDROIDJARS_ON_REMOTE_HOST -f apk -i prune -e ${apkbasename}.logcat"

		# move results
		ssh $REMOTEUSER@$REMOTEHOST "cd $REMOTEDIR; mv -f ${apkbasename}.json ${apkbasename}.logcat $apkname apps/${apkbasename}.d"
		ssh $REMOTEUSER@$REMOTEHOST "cd $REMOTEDIR; mv -f HisDroid.log apps/${apkbasename}.d/HisDroid.log.analysis"
		ssh $REMOTEUSER@$REMOTEHOST "cd $REMOTEDIR; rm -rf apps/${apkbasename}.d/sootOutput.analysis"
		ssh $REMOTEUSER@$REMOTEHOST "cd $REMOTEDIR; mv -f sootOutput apps/${apkbasename}.d/sootOutput.analysis"

		echo $ANALYZED >> $result
	fi

done
