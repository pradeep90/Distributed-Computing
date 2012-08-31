#!/bin/bash +x

# java -cp ../../class/ mutexlamport.FileWriter $@
# java -cp ../../class/ mutexlamport.LogicalClock $@
java -cp ../../class/ mutexlamport.MutexExecutor $@
