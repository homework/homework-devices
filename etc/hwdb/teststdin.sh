#!/bin/bash

{ echo "create table a (i integer, r real)";
export count=1;
while true; do
	for f in 1 2 3 4 5 6 7 8 9 10; do
		i=$((count++));
		echo "insert into a values ('$i', '$i.0')";
	done;
	echo 'select * from a [rows 2]';
done; } | ./stdinserver
