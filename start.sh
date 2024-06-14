#!/bin/bash
java -classpath /usr/local/sbin/myweb HttpServer &
echo "$!" > "/run/myweb.pid"