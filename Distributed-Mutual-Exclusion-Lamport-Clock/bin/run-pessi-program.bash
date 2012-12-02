#!/bin/bash +x

# java -cp ../classes/ mutexlamport.FileWriter $@
# java -cp ../classes/ mutexlamport.Operation $@

# java -cp ../classes/ mutexlamport.MutexExecutor 0 shared-file.txt localhost:36330 localhost:36331 localhost:36332

java -cp ../classes/ pessimconcurr.TransactionExecutor 0 shared-file.txt data-items.txt localhost:36330 localhost:36330 localhost:36331 localhost:36332 < sample-program-1.txt &
java -cp ../classes/ pessimconcurr.TransactionExecutor 1 shared-file.txt data-items.txt localhost:36330 localhost:36330 localhost:36331 localhost:36332 < sample-program-2.txt &
java -cp ../classes/ pessimconcurr.TransactionExecutor 2 shared-file.txt data-items.txt localhost:36330 localhost:36330 localhost:36331 localhost:36332 < sample-program-3.txt &

# java -cp ../classes/ sockets.SenderThread localhost:36330 "Yo, boyz" &

# java -cp ../classes/ mutexlamport.MutexMessage $@

sleep 6
killall java
