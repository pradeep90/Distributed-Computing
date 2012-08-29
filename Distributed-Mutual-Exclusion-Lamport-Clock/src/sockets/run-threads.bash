#!/bin/bash

java -cp ../../class/ sockets.Sender &
java -cp ../../class/ sockets.ReceiverThread
