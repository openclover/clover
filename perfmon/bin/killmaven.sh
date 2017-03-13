#!/bin/bash

function killkids() {
    for child in $(ps -o pid,ppid -ax | awk "{ if ( \$2 == $1 ) { print \$1 }}")
    do
      killkids $child $1
    done
    echo "Killing child process $1 (ppid = $2)"
    kill -9 $1
}

for i in `ls $1/*.pid`
do
    PID=`cat $i`
    killkids $PID "maven"
done
