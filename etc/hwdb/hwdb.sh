#!/bin/sh
#

# Default variables - change them appropriately.
HWDB_RC="hwdb.rc"
FLOW_IF="wlan0"
LINK_IF="fish0"
DNSMASQ_CONF="/etc/dnsmasq.conf"
DNSMASQ_EXEC="/etc/init.d/dnsmasq"
DATADIR="data"

# You shouldn't have to change anything from this point onwards.
#
NAME="hwdb.sh"
CURRENTDIR=`pwd`
# First, start the server.
DAEMON1="hwdbserver"
DAEMON1_ARGS="-c $HWDB_RC"
# Then, start the loggers.
DAEMON2="flowlogger"
DAEMON2_ARGS="-d $FLOW_IF"
DAEMON3="linklogger"
DAEMON3_ARGS="-d $LINK_IF"
DAEMON4="dhcplogger"
HWDB_DHCP_LOGGER=$DAEMON4
# Finally, start the programs that log records persistently.
DAEMON5="flowpersist"
DAEMON5_ARGS="-d $CURRENTDIR/$DATADIR/flow.db"
DAEMON6="linkpersist"
DAEMON6_ARGS="-d $CURRENTDIR/$DATADIR/link.db"
DAEMON7="dhcppersist"
DAEMON7_ARGS="-d $CURRENTDIR/$DATADIR/dhcp.db"
PIDDIR=".pids"
# PIDFILES=<program name>.pid

# Only root should run this script.
user=`id -u`
if [ $user != "0" ]; then
	echo "This script must be run as root." 1>&2
	exit 1
fi

if [ $# -ne 1 ]; then
	echo "Usage: $NAME {start|stop|check|status}" 1>&2
	exit 1
fi


# Create the directory ./.pids if it doesn't exist.
[ -d "$CURRENTDIR/$PIDDIR" ] || mkdir -p $CURRENTDIR/$PIDDIR

do_check_masq_cf()
{
	[ \( -w $DNSMASQ_CONF \) -a \( -x $DNSMASQ_EXEC \) ] || return 1
}

do_check_flow_if()
{
	./test_iface $FLOW_IF flow
	[ $? -eq 0 ] || return 1
}

do_check_link_if()
{
	./test_iface $LINK_IF link
	[ $? -eq 0 ] || return 1
}

do_check()
{
	# Exit if hwdbserver does not exist
	if [ ! -x "$DAEMON1" ]; then
		echo "Could not find $DAEMON1 in $CURRENTDIR. Did you run 'make'?" 1>&2
		exit 1
	elif [ ! -x "$DAEMON2" ]; then
		echo "Could not find $DAEMON2 in $CURRENTDIR. Did you run 'make'?" 1>&2
		exit 1
	elif [ ! -x "$DAEMON3" ]; then
		echo "Could not find $DAEMON3 in $CURRENTDIR. Did you run 'make'?" 1>&2
		exit 1
	elif [ ! -x "$DAEMON4" ]; then
		echo "Could not find $DAEMON4 in $CURRENTDIR. Did you run 'make'?" 1>&2
		exit 1
	elif [ ! -x "$DAEMON5" ]; then
		echo "Could not find $DAEMON5 in $CURRENTDIR. Did you run 'make'?" 1>&2
		exit 1
	elif [ ! -x "$DAEMON6" ]; then
		echo "Could not find $DAEMON6 in $CURRENTDIR. Did you run 'make'?" 1>&2
		exit 1
	elif [ ! -x "$DAEMON7" ]; then
		echo "Could not find $DAEMON7 in $CURRENTDIR. Did you run 'make'?" 1>&2
		exit 1
	fi

	do_check_flow_if
	case "$?" in
	0)
	# echo "Interface $FLOW_IF is of type EN10MB"
	;;
	1)
	echo "Interface $FLOW_IF is not of type EN10MB - or doesn't exist."
	exit 1
	;;
	esac
	
	do_check_link_if
	case "$?" in
	0)
	# echo "Interface $LINK_IF is of type IEEE802_11_RADIO"
	;;
	1)
	echo "Interface $LINK_IF is not of type IEEE802_11_RADIO - or doesn't exist"
	exit 1
	;;
	esac
	
	do_check_masq_cf
	case "$?" in
	0)
	# echo "dnsmasq is installed appropriately."
	;;
	1)
	echo "dnsmasq is not installed appropriately - look for /etc/init.d/dnsmasq and /etc/dnsmasq.conf"
	exit 1
	;;
	esac
	
	echo "check OK"
}

do_hack_dns()
{
	# To avoid long delays, search /etc/nsswitch.conf for line 
	# 
	# 	"hosts: files mdns4_minimal [NOTFOUND=return] dns mdns4"
	#
	# and replace it with with
	#
	# 	"hosts: files dns"
	#
	# Or
	#
	if [ -f /etc/nsswitch.conf ]; then
		cp /etc/nsswitch.conf /etc/nsswitch.conf.bak
		awk '$1 == "hosts:" && $2 == "files" && $3 == "mdns4_minimal" && $4 == "[NOTFOUND=return]" && $5=="dns" && $6 == "mdns4" { print "#"$0; print $1"\t\t", $2, $5; next }{ print $0 }' /etc/nsswitch.conf > tmp$$ && mv tmp$$ /etc/nsswitch.conf
	fi
}

undo_hack_dns()
{
	if [ -f /etc/nsswitch.conf.bak ]; then
		mv /etc/nsswitch.conf.bak /etc/nsswitch.conf
	fi
}

do_hack_masq()
{
	cp $CURRENTDIR/$HWDB_DHCP_LOGGER /bin
	line="dhcp-script=/bin/$HWDB_DHCP_LOGGER"
	echo $line
	cat $DNSMASQ_CONF | grep $line > tmp_file$$
	if [ ! -s tmp_file$$ ]; then
		echo "$line" >> $DNSMASQ_CONF
	fi
	rm tmp_file$$
	sudo /etc/init.d/dnsmasq restart
	if [ $? -eq 1 ]; then
		return 1
	fi
	return 0
}

undo_hack_masq()
{
	rm /bin/$HWDB_DHCP_LOGGER
	line="/dhcp-script=\/bin\/$HWDB_DHCP_LOGGER/d"
	sed $line $DNSMASQ_CONF > tmp_file$$
	mv tmp_file$$ $DNSMASQ_CONF
	sudo /etc/init.d/dnsmasq restart
	if [ $? -eq 1 ]; then
		return 1
	fi
	return 0
}

do_start_daemon()
{
	[ ! -f $CURRENTDIR/$PIDDIR/$1.pid ] || return 2
	#
	# TODO: $2 and $3 are passed as a single parameter, DAEMON<x>_FLAGS
	#
	nice ./$1 $2 $3 &
	PID=$!
	ERR=$?
	sleep 3
	if [ $ERR -eq 0 ]; then
		echo $PID > $CURRENTDIR/$PIDDIR/$1.pid
		return 0
	else
		return 1
	fi
}

do_stop_daemon()
{
	[ -s $CURRENTDIR/$PIDDIR/$1.pid ] || return 1
        PID=`cat $CURRENTDIR/$PIDDIR/$1.pid`
        kill -15 $PID
        rm $CURRENTDIR/$PIDDIR/$1.pid
	return 0
}

do_start()
{
	echo "Starting services..."
	
	do_hack_dns
	
	do_start_daemon $DAEMON1 $DAEMON1_ARGS
	case "$?" in
	0)
	echo "$DAEMON1 started successfully."
	;;
	1)
	echo "$DAEMON1 failed."
	exit 1
	;;
	2)
	echo "$DAEMON1 is already running."
	;;
	esac
	
	do_start_daemon $DAEMON2 $DAEMON2_ARGS
	case "$?" in
	0)
	echo "$DAEMON2 started successfully."
	;;
	1)
	echo "$DAEMON2 failed."
	;;
	2)
	echo "$DAEMON2 is already running."
	;;
	esac
	
	do_start_daemon $DAEMON5 $DAEMON5_ARGS
	case "$?" in
	0)
	echo "$DAEMON5 started successfully."
	;;
	1)
	echo "$DAEMON5 failed."
	;;
	2)
	echo "$DAEMON5 is already running."
	;;
	esac
	
	do_start_daemon $DAEMON3 $DAEMON3_ARGS
	case "$?" in
	0)
	echo "$DAEMON3 started successfully."
	;;
	1)
	echo "$DAEMON3 failed."
	;;
	2)
	echo "$DAEMON3 is already running."
	;;
	esac

	do_start_daemon $DAEMON6 $DAEMON6_ARGS
	case "$?" in
	0)
	echo "$DAEMON6 started successfully."
	;;
	1)
	echo "$DAEMON6 failed."
	;;
	2)
	echo "$DAEMON6 is already running."
	;;
	esac
	
	do_start_daemon $DAEMON7 $DAEMON7_ARGS
	case "$?" in
	0)
	echo "$DAEMON7 started successfully."
	;;
	1)
	echo "$DAEMON7 failed."
	;;
	2)
	echo "$DAEMON7 is already running."
	;;
	esac
	
	do_hack_masq
	if [ $? -eq 0 ]; then
		echo "$DAEMON4 installed."
	else
		echo "$DAEMON4 failed."
	fi
}

do_stop()
{
	echo "Stopping services..."
	
	undo_hack_dns

	do_stop_daemon $DAEMON7
	case "$?" in
	0)
	echo "$DAEMON7 stopped."
	;;
	1)
	echo "$DAEMON7 is not running."
	;;
	esac

	do_stop_daemon $DAEMON6
	case "$?" in
	0)
	echo "$DAEMON6 stopped."
	;;
	1)
	echo "$DAEMON6 is not running."
	;;
	esac
	
	do_stop_daemon $DAEMON5
	case "$?" in
	0)
	echo "$DAEMON5 stopped."
	;;
	1)
	echo "$DAEMON5 is not running."
	;;
	esac
	
	undo_hack_masq
	if [ $? -eq 0 ]; then
		echo "$DAEMON4 uninstalled."
	else
		echo "$DAEMON4 failed."
	fi

	do_stop_daemon $DAEMON3
	case "$?" in
	0)
	echo "$DAEMON3 stopped."
	;;
	1)
	echo "$DAEMON3 is not running."
	;;
	esac
	
	do_stop_daemon $DAEMON2
	case "$?" in
	0)
	echo "$DAEMON2 stopped."
	;;
	1)
	echo "$DAEMON2 is not running."
	;;
	esac
	
	do_stop_daemon $DAEMON1
	case "$?" in
	0)
	echo "$DAEMON1 stopped."
	;;
	1)
	echo "$DAEMON6 is not running."
	;;
	esac
}

check_process_status()
{
	if [ -s $CURRENTDIR/$PIDDIR/$1.pid ]; then
		pid=`cat $CURRENTDIR/$PIDDIR/$1.pid` && ps -p $pid > /dev/null
		if [ $? -eq 0 ]; then
			echo "$1 is running."
		else
			echo "Error: according to hwdb, $1 appears to be running but is not."
		fi
	else
		ps -C $1 > /dev/null
		if [ $? -eq 1 ]; then
			echo "$1 is not running."
		else
			echo "Error: $1 is running but it is beyond the control of this script."
		fi
	fi
}

check_status()
{
	check_process_status $DAEMON1
	check_process_status $DAEMON2
	check_process_status $DAEMON3
	
	line="dhcp-script=/bin/$HWDB_DHCP_LOGGER"
	cat $DNSMASQ_CONF | grep $line > tmp_file$$
        if [ \( -s tmp_file$$ \) -a \( -f /bin/$HWDB_DHCP_LOGGER \) ]; then
		echo "$HWDB_DHCP_LOGGER is installed."
	else
		echo "$HWDB_DHCP_LOGGER is not installed."
	fi
	rm tmp_file$$
	
	check_process_status $DAEMON5
	check_process_status $DAEMON6
	check_process_status $DAEMON7
}

case "$1" in
	start)
	do_check
	do_start
	;;
	stop)
	do_stop
	;;
	check)
	do_check
	;;
	status)
	check_status
	;;
	*) 
	echo "Usage: $NAME {start|stop|check|status}" 1>&2
	exit 1
	;;
esac

exit

