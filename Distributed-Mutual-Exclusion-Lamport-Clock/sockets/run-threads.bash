#!/bin/bash

java -cp .. sockets.Sender &
java -cp .. sockets.ReceiverThread
