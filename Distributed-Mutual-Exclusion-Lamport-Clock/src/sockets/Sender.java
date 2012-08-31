package sockets;

import java.net.*;
import java.io.*;
import java.lang.*;

public class Sender {

    public static void main(String argv[]) {

        String server_host;
        int server_port;
        String message;
        if (argv.length == 0){
            System.out.println ("Format: server_host:server_port message");
            System.exit (0);
        }

        server_host = argv[0].split (":")[0];
        server_port = Integer.parseInt (argv[0].split (":")[1]);
        message = argv[1];
	
	try {
	    ServerSocket sSoc = new ServerSocket(server_port);
			
	    while(true) {
		Socket inSoc = sSoc.accept();
		SenderThread newSender = new SenderThread(inSoc, message);
		newSender.start();
	    }
	}
	catch (Exception e) {
	    System.out.println("Error in Sender: " + e.toString());
	}
    }
}
