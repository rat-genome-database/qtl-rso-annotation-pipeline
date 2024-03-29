#!/usr/bin/env bash
# shell script to run QtlRsoAnnotation
. /etc/profile

APPNAME=qtl-rso-annotation-pipeline
APPDIR=/home/rgddata/pipelines/$APPNAME
SERVER=`hostname -s | tr '[a-z]' '[A-Z]'`
EMAIL_LIST=mtutaj@mcw.edu
if [ "$SERVER" = "REED" ]; then
  EMAIL_LIST=mtutaj@mcw.edu
fi

cd $APPDIR

java -Dspring.config=$APPDIR/../properties/default_db2.xml \
    -Dlog4j.configurationFile=file://$APPDIR/properties/log4j2.xml \
    -jar lib/$APPNAME.jar "$@" 2>&1 | tee run.log

/home/rgddata/pipelines/OntologyLoad/run_single.sh RS -skip_download

mailx -s "[$SERVER] QtlRsoAnnotation pipeline OK" $EMAIL_LIST < run.log
