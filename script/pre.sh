#!/bin/bash

source config.sh

for apk in $APPDIR/*.apk; do
	apkname=`basename $apk`
	apkbasename=`basename ${apk} .apk`
	result=$RESULTDIR/$apkbasename

	# test the count of dex file
	if [ ! -e $result ]; then
		dexcount=`./dexcount.sh $apk`
		if [ $dexcount == "1" ]; then
			dexresult=$DEX1
		elif (( $dexcount > 1 )); then
			dexresult=$DEX2
		else
			dexresult=$DEX0
		fi
		echo $dexresult > $result
		echo "$apkbasename has $dexresult"
	else
		if [[ $(grep "$DEX1" $result) != "" ]]; then
			dexresult=$DEX1
		elif [[ $(grep "$DEX2" $result) != "" ]]; then
			dexresult=$DEX2
		else
			dexresult=$DEX0
		fi
		echo "$apkbasename had $dexresult"
	fi
	
	# if have only one dex then instrument the app
	if [[ $dexresult == $DEX1 && ! -e $INSAPPDIR/$apkname ]]; then
		scp $apk $REMOTEUSER@$REMOTEHOST:$REMOTEDIR
		ssh $REMOTEUSER@$REMOTEHOST "cd $REMOTEDIR; java -Xmx64G -jar ${JAR} -a $apkname -j $ANDROIDJARS_ON_REMOTE_HOST -f apk -i pre-eva"
		ssh $REMOTEUSER@$REMOTEHOST "cd $REMOTEDIR; if [ ! -d ${apkbasename}.d ]; then mkdir ${apkbasename}.d; fi; mv -f $apkname HisDroid.log sootOutput ${apkbasename}.d"
		scp $REMOTEUSER@$REMOTEHOST:$REMOTEDIR/${apkbasename}.d/sootOutput/$apkname $INSAPPDIR
		insapk=$INSAPPDIR/$apkname
		if [ -e $insapk ]; then
			echo asdfzxcv | jarsigner -sigalg SHA1withRSA -digestalg SHA1 -keystore ~/my-release-key.keystore $insapk alias_name
			echo $INSSUCCESS >> $result
		else
			echo $INSFAILED >> $result
		fi
	fi
done

