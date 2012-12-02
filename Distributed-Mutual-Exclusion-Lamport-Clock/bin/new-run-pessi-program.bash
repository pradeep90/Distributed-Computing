#!/bin/bash +x

java -cp ../classes/ pessimconcurr.TransactionExecutor 0 shared-file.txt data-items-locations.txt localhost:36330 localhost:36330 localhost:36331 localhost:36332 < new-sample-program-1.txt &
java -cp ../classes/ pessimconcurr.TransactionExecutor 1 shared-file.txt data-items-locations.txt localhost:36330 localhost:36330 localhost:36331 localhost:36332 < new-sample-program-2.txt &
java -cp ../classes/ pessimconcurr.TransactionExecutor 2 shared-file.txt data-items-locations.txt localhost:36330 localhost:36330 localhost:36331 localhost:36332 < new-sample-program-3.txt &

sleep 6
killall java
