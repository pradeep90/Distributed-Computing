#!/bin/bash +x

# java -cp ../../class/ mutexlamport.FileWriter $@
# java -cp ../../class/ mutexlamport.LogicalClock $@
java -cp ../../class/ mutexlamport.MutexExecutor 0 localhost:36330 localhost:36331 localhost:36332 &
java -cp ../../class/ mutexlamport.MutexExecutor 1 localhost:36331 localhost:36330 localhost:36332 &
java -cp ../../class/ mutexlamport.MutexExecutor 2 localhost:36332 localhost:36330 localhost:36331 &
# java -cp ../../class/ sockets.SenderThread localhost:36330 "Yo, boyz" &

sleep 10
killall java
