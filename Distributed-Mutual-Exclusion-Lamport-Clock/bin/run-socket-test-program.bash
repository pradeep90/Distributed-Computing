#!/bin/bash

java -cp ../classes/ sockets.Sender localhost:36330 "This is a test message..." &
java -cp ../classes/ sockets.ReceiverCallable localhost:36330 

# java -cp ../../classes/ sockets.Receiver localhost:36330 &
# sleep 2
# java -cp ../../classes/ sockets.SenderThread localhost:36330 "Yo, boyz" &
# java -cp ../../classes/ sockets.SenderThread localhost:36330 "Yo, boyz" &
# java -cp ../../classes/ sockets.SenderThread localhost:36330 "I am different" &
# java -cp ../../classes/ sockets.SenderThread localhost:36330 "Yo, boyz" &
# java -cp ../../classes/ sockets.SenderThread localhost:36330 "Yo, boyz" &
# java -cp ../../classes/ sockets.SenderThread localhost:36330 "Yo, boyz" &

sleep 2
killall java
