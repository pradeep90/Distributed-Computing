#!/bin/bash

# java -cp ../../class/ sockets.Sender &
# java -cp ../../class/ sockets.ReceiverCallable

java -cp ../../class/ sockets.Receiver &
sleep 2
java -cp ../../class/ sockets.SenderThread &
java -cp ../../class/ sockets.SenderThread &
java -cp ../../class/ sockets.SenderThread "I am different" &
java -cp ../../class/ sockets.SenderThread &
java -cp ../../class/ sockets.SenderThread &
java -cp ../../class/ sockets.SenderThread &
