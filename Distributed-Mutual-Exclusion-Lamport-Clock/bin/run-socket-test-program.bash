#!/bin/bash

java -cp ../class/ sockets.Sender localhost:36330 "This is a test message..." &
java -cp ../class/ sockets.ReceiverCallable localhost:36330 

# java -cp ../../class/ sockets.Receiver localhost:36330 &
# sleep 2
# java -cp ../../class/ sockets.SenderThread localhost:36330 "Yo, boyz" &
# java -cp ../../class/ sockets.SenderThread localhost:36330 "Yo, boyz" &
# java -cp ../../class/ sockets.SenderThread localhost:36330 "I am different" &
# java -cp ../../class/ sockets.SenderThread localhost:36330 "Yo, boyz" &
# java -cp ../../class/ sockets.SenderThread localhost:36330 "Yo, boyz" &
# java -cp ../../class/ sockets.SenderThread localhost:36330 "Yo, boyz" &

sleep 2
killall java
