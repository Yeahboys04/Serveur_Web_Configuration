#!/bin/bash
pid=$(cat "/run/myweb.pid")
kill "$pid"