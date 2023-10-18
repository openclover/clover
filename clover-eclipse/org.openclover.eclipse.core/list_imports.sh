#!/usr/bin/env bash

find target/classes -name '*.class' | sed 's@\(.*/\).*@\1@' | sed 's@target/classes/@@' | sed 's@/@.@g' | sort -u | sed 's@\.$@@' | tr '\n' ','
