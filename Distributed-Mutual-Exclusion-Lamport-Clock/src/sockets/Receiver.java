package sockets;

import java.net.*;
import java.io.*;
import java.lang.*;

public class Receiver implements Runnable {

    ServerSocket serverSocket;
    

    Receiver (ServerSocket serverSocket){
        this.serverSocket = serverSocket;
    }

    public void run (){
	try {
	    while(true) {
		Socket newSocket = serverSocket.accept();
		ReceiverThread newReceiver = new ReceiverThread(newSocket);
		newReceiver.start();
	    }
	}
	catch (Exception e) {
	    System.out.println("Error in the server: " + e.toString());
	}
    }
    
    public static void main(String argv[]) {
        int port;
        
        if (argv.length == 0){
            port = 2001;
        } else {
            port = Integer.parseInt (argv[0]);
        }

        try {
            Receiver receiver = new Receiver (new ServerSocket (port));
            Thread receiverThread = new Thread (receiver);
            receiverThread.run ();
        } catch (Exception e) {
            System.out.println ("Error while running Receiver:" + e.toString ());
        }
    }
}
