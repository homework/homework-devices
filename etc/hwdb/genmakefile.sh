#!/bin/sh
#
# shell script to generate the HWDB Makefile for the system upon which the
# script is executed
# usage: ./genmakefile.sh
#
# if a Makefile exists in the current directory, it is renamed to Makefile.save
# 
# a Makefile customized to the system upon which the script is executed is
# then generated

if [ -e ./Makefile ]; then
	echo "Renaming Makefile to Makefile.save"
	mv Makefile Makefile.save
fi
echo "# Makefile for Homework HWDB system" >Makefile
echo "# Customized for `uname -n` running `uname -s` on `date`" >>Makefile
echo "# " >>Makefile
echo "OS=`uname -s`" >>Makefile
if [ -e /usr/lib/libpcap.a ]; then
	echo "HAVE_PCAP=yes" >>Makefile
fi
if [ -e /usr/local/BerkeleyDB.4.8/lib/libdb.a ]; then
	echo "HAVE_BERKELEYDB=yes" >>Makefile
fi

cat <<!endoftemplate! >>Makefile
# Template makefile for Homework HWDB system
#
# Conditionalized upon the value of \$(OS) - if unspecified in the command
# line, OS is assumed to be the value defined above
#
# e.g.
#      make			# makes appropriate binaries for $OS
#      make OS=CYGWIN_NT-6.0	# makes appropriate binaries for Cygwin
#      make OS=Darwin           # makes appropriate binaries for OSX
#      make OS=Linux		# makes appropriate binaries for Linux

# base definitions
CC = gcc
BERKELEYDB_LIB = /usr/local/BerkeleyDB.4.8/lib/libdb.a
PCAP_LIB = /usr/lib/libpcap.a
CFL_BASE = -W -Wall -DWARNING
ifdef HAVE_BERKELEYDB
    CFL_BASE += -I /usr/local/BerkeleyDB.4.8/include
endif
LFL_COMMON =
CFL_COMMON = \$(CFL_BASE)
#CFL_COMMON = \$(CFL_BASE) -DLOG -DVLOG
#CFL_COMMON = \$(CFL_BASE) -g -DLOG -DVLOG -DDEBUG -DVDEBUG

ifeq "\$(OS)" "CYGWIN_NT-6.0"
    EXT=.exe
else
    EXT=
endif

CLIENTS = hwdbclient\$(EXT) flowmonitor\$(EXT) linkmonitor\$(EXT) dhcpmonitor\$(EXT) hwdbcallback\$(EXT)
ifdef HAVE_BERKELEYDB
    CLIENTS += flowpersist\$(EXT) flowdump\$(EXT) linkpersist\$(EXT) linkdump\$(EXT) dhcppersist\$(EXT) dhcpdump\$(EXT)
endif
SERVERS = hwdbserver\$(EXT) stdinserver\$(EXT) rpcserver\$(EXT)
LOGGERS =
ifdef HAVE_PCAP
    LOGGERS += flowlogger\$(EXT) linklogger\$(EXT) dhcplogger\$(EXT) test_iface
endif

CFLAGS=\$(CFL_COMMON)
LIBS = -lpthread
LDFLAGS = \$(LFL_COMMON)
# OS-specific definitions
ifeq (\$(OS),CYGWIN_NT-6.0)
    LIBS =
    LDFLAGS = \$(LFL_COMMON) -Wl,--enable-auto-import
endif
ifeq (\$(OS),Darwin)
    CFLAGS = \$(CFL_COMMON) -DHAVE_SOCKADDR_LEN
endif

OBJ = crecord.o ctable.o endpoint.o hashtable.o hwdb.o indextable.o list.o mb.o mem.o nodecrawler.o parser.o rtab.o sqlstmts.o srpc.o stable.o table.o tslist.o timestamp.o typetable.o portmap.o i8_parser.o hostmap.o dhcprec.o protomap.o

all: \$(CLIENTS) \$(SERVERS) \$(LOGGERS)

clients: \$(CLIENTS)

servers: \$(SERVERS)

loggers: \$(LOGGERS)

#
# client applications
#

hwdbclient\$(EXT): hwdbclient.c config.h util.h rtab.h srpc.h libhwdb.a
	\$(CC) \$(CFLAGS) \$(LFLAGS) -o hwdbclient\$(EXT) hwdbclient.c libhwdb.a \$(LIBS)

hwdbcallback\$(EXT): hwdbcallback.c config.h util.h rtab.h srpc.h libhwdb.a
	\$(CC) \$(CFLAGS) \$(LDFLAGS) -o hwdbcallback\$(EXT) hwdbcallback.c libhwdb.a \$(LIBS)

flowmonitor\$(EXT): flowmonitor.c flowmonitor.h config.h util.h rtab.h srpc.h timestamp.h portmap.h hostmap.h protomap.h flowrec.h libhwdb.a
	\$(CC) \$(CFLAGS) \$(LDFLAGS) -o flowmonitor\$(EXT) flowmonitor.c libhwdb.a \$(LIBS)

flowpersist\$(EXT): flowpersist.c flowmonitor.h config.h util.h rtab.h srpc.h timestamp.h portmap.h protomap.h flowrec.h libhwdb.a
	\$(CC) \$(CFLAGS) \$(LDFLAGS) -o flowpersist\$(EXT) flowpersist.c libhwdb.a \$(LIBS) \$(BERKELEYDB_LIB)

flowdump\$(EXT): flowdump.c flowrec.h timestamp.h portmap.h protomap.h libhwdb.a
	\$(CC) \$(CFLAGS) \$(LDFLAGS) -o flowdump\$(EXT) flowdump.c libhwdb.a \$(LIBS) \$(BERKELEYDB_LIB)

linkmonitor\$(EXT): linkmonitor.c linkmonitor.h config.h util.h rtab.h srpc.h timestamp.h i8_parser.h libhwdb.a
	\$(CC) \$(CFLAGS) \$(LDFLAGS) -o linkmonitor\$(EXT) linkmonitor.c libhwdb.a \$(LIBS)

linkpersist\$(EXT): linkpersist.c linkmonitor.h config.h util.h rtab.h srpc.h timestamp.h i8_parser.h libhwdb.a
	\$(CC) \$(CFLAGS) \$(LDFLAGS) -o linkpersist\$(EXT) linkpersist.c libhwdb.a \$(LIBS) \$(BERKELEYDB_LIB)

linkdump\$(EXT): linkdump.c linkrec.h timestamp.h libhwdb.a
	\$(CC) \$(CFLAGS) \$(LDFLAGS) -o linkdump\$(EXT) linkdump.c libhwdb.a \$(LIBS) \$(BERKELEYDB_LIB)

dhcpmonitor\$(EXT): dhcpmonitor.c config.h util.h rtab.h srpc.h timestamp.h dhcprec.h libhwdb.a
	\$(CC) \$(CFLAGS) \$(LDFLAGS) -o dhcpmonitor\$(EXT) dhcpmonitor.c libhwdb.a \$(LIBS)

dhcppersist\$(EXT): dhcppersist.c config.h util.h rtab.h srpc.h timestamp.h dhcprec.h libhwdb.a
	\$(CC) \$(CFLAGS) \$(LDFLAGS) -o dhcppersist\$(EXT) dhcppersist.c libhwdb.a \$(LIBS) \$(BERKELEYDB_LIB)

dhcpdump\$(EXT): dhcpdump.c dhcprec.h timestamp.h libhwdb.a
	\$(CC) \$(CFLAGS) \$(LDFLAGS) -o dhcpdump\$(EXT) dhcpdump.c libhwdb.a \$(LIBS) \$(BERKELEYDB_LIB)

#
# server applications
#

hwdbserver\$(EXT): hwdbserver.c config.h util.h hwdb.h rtab.h srpc.h mb.h y.tab.h libhwdb.a
	\$(CC) \$(CFLAGS) \$(LDFLAGS) -o hwdbserver\$(EXT) hwdbserver.c y.tab.c lex.yy.c libhwdb.a \$(LIBS)

stdinserver\$(EXT): stdinserver.c config.h util.h hwdb.h rtab.h mb.h y.tab.h libhwdb.a
	\$(CC) \$(CFLAGS) \$(LDFLAGS) -o stdinserver\$(EXT) stdinserver.c y.tab.c lex.yy.c libhwdb.a \$(LIBS)

rpcserver\$(EXT): rpcserver.c config.h srpc.h libhwdb.a
	\$(CC) \$(CFLAGS) \$(LDFLAGS) -o rpcserver\$(EXT) rpcserver.c libhwdb.a \$(LIBS)


#
# loggers
#

flowlogger\$(EXT): flowlogger.o flow_accumulator.o libhwdb.a
	\$(CC) \$(CFLAGS) \$(LDFLAGS) -o flowlogger\$(EXT) flowlogger.o flow_accumulator.o libhwdb.a \$(LIBS) -lpcap

linklogger\$(EXT): linklogger.o link_accumulator.o rt_parser.o libhwdb.a
	\$(CC) \$(CFLAGS) \$(LDFLAGS) -o linklogger\$(EXT) linklogger.o link_accumulator.o rt_parser.o libhwdb.a \$(LIBS) -lpcap

dhcplogger: dhcplogger.o libhwdb.a
	\$(CC) \$(CFLAGS) \$(LDFLAGS) -o dhcplogger dhcplogger.o libhwdb.a \$(LIBS)
	
test_iface: test_iface.o
	\$(CC) \$(CFLAGS) -o test_iface test_iface.o -lpcap

y.tab.h: scan.l gram.y
	flex scan.l
	yacc -d gram.y

clean:
	rm -f y.tab.c y.tab.h lex.yy.c
	rm -f *.o *~ \$(CLIENTS) \$(SERVERS) \$(LOGGERS)

crecord.o: crecord.c crecord.h ctable.h mem.h endpoint.h stable.h
ctable.o: ctable.c ctable.h endpoint.h crecord.h
endpoint.o: endpoint.c endpoint.h mem.h
hashtable.o: hashtable.c hashtable.h
hwdb.o: hwdb.c hwdb.h mb.h util.h rtab.h sqlstmts.h parser.h indextable.h table.h hashtable.h pubsub.h srpc.h tslist.h y.tab.h
indextable.o: indextable.c indextable.h tuple.h hashtable.h table.h sqlstmts.h util.h nodecrawler.h typetable.h rtab.h srpc.h pubsub.h list.h
list.o: list.c list.h mem.h
mb.o: mb.c mb.h node.h table.h tuple.h timestamp.h
mem.o: mem.c mem.h
nodecrawler.o: nodecrawler.c nodecrawler.h util.h list.h rtab.h sqlstmts.h table.h tuple.h timestamp.h node.h
parser.o: parser.c parser.h typetable.h util.h sqlstmts.h y.tab.h
rtab.o: rtab.c rtab.h util.h typetable.h sqlstmts.h config.h srpc.h
sqlstmts.o: sqlstmts.c sqlstmts.h util.h timestamp.h y.tab.h typetable.h
srpc.o: srpc.c srpc.h srpcdefs.h tslist.h endpoint.h ctable.h crecord.h mem.h stable.h
stable.o: stable.c stable.h mem.h tslist.h
table.o: table.c table.h util.h typetable.h sqlstmts.h list.h pubsub.h srpc.h node.h rtab.h
tslist.o: tslist.c tslist.h mem.h
timestamp.o: timestamp.c timestamp.h mem.h
typetable.o: typetable.c typetable.h
portmap.o: portmap.c portmap.h
protomap.o: protomap.c protomap.h
flow_accumulator.o: flow_accumulator.c flow_accumulator.h
link_accumulator.o: link_accumulator.c link_accumulator.h
hostmap.o: hostmap.c hostmap.h
dhcprec.o: dhcprec.c dhcprec.h rtab.h timestamp.h

libhwdb.a: \$(OBJ)
	rm -f libhwdb.a
	ar r libhwdb.a \$(OBJ)
	ranlib libhwdb.a
!endoftemplate!
