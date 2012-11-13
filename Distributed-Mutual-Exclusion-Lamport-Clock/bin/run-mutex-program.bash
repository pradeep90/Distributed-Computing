#!/bin/bash +x

# java -cp ../classes/ mutexlamport.FileWriter $@
# java -cp ../classes/ mutexlamport.Operation $@

# java -cp ../classes/ mutexlamport.MutexExecutor 0 shared-file.txt localhost:36330 localhost:36331 localhost:36332

java -cp ../classes/ mutexlamport.MutexExecutor 0 shared-file.txt localhost:36330 localhost:36330 localhost:36331 localhost:36332 < sample-program.txt &
java -cp ../classes/ mutexlamport.MutexExecutor 1 shared-file.txt localhost:36330 localhost:36330 localhost:36331 localhost:36332 < sample-program.txt &
java -cp ../classes/ mutexlamport.MutexExecutor 2 shared-file.txt localhost:36330 localhost:36330 localhost:36331 localhost:36332 < sample-program.txt &

# java -cp ../classes/ sockets.SenderThread localhost:36330 "Yo, boyz" &

# java -cp ../classes/ mutexlamport.MutexMessage $@

sleep 6
killall java
