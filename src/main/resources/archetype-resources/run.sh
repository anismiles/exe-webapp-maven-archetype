#!/bin/sh
#
# chkconfig: 35 85 15 
# description: <put some description here>
#
# processname: ${artifactId}

# Source function library.
#. /etc/rc.d/init.d/functions
# Source networking configuration.
#. /etc/sysconfig/network

# Check that networking is up.
[ "$NETWORKING" = "no" ] && exit 0

PATH_TO_WAR=Absoute-Path-To-Dist-War
APP_NAME=Your-App-Name-Here
PID_FILE=$HOME/${APP_NAME}.pid

#EXTRA_JETTY_PROPS_FILE=
#touch ${EXTRA_JETTY_PROPS_FILE}
JAVA_ARGS=
#JAVA_ARGS="-Dconfig=${EXTRA_JETTY_PROPS_FILE}"

start() {
	echo "PID_FILE ==> ${PID_FILE}"
    if [[ -f ${PID_FILE} ]]; then
        echo "Already running"
        exit 1
    fi

    if [[ -f ${PATH_TO_WAR} ]]; then
        echo "Starting ==> ${PATH_TO_WAR}"
        java ${JAVA_ARGS} -jar ${PATH_TO_WAR} start &
        PID=$!
        echo "$PID" > ${PID_FILE}
        echo "Started ${APP_NAME} with pid: ${PID}"
    fi
}

stop() {
    # Try gracefully first
	java ${JAVA_ARGS} -jar ${PATH_TO_WAR} stop
    sleep 10
    if [[ -f ${PID_FILE} ]]; then
        PID=`cat ${PID_FILE}`
        test -z $PID || kill $PID
        rm ${PID_FILE}
        sleep 5
        echo "Forcibly Stopped ${APP_NAME} with pid: ${PID}"
    fi
}

restart() {
	stop
	start
}

case "$1" in
    start)
        start
        ;;
    stop)
    	stop
        ;;
    restart)
        restart
        ;;
    *)
        echo "Usage: $0 [start|stop|restart]"
        echo "      start    Start the server"
        echo "      stop     Stop the server gracefully" 
        echo "      restart  Restart server" 
  		exit 1
esac