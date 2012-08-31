package sockets;

import java.net.*;
import java.io.*;
import java.lang.*;

public class Sender {

    public static void main(String argv[]) {
	
	try {
	    ServerSocket sSoc = new ServerSocket(2001);
			
	    while(true) {
		Socket inSoc = sSoc.accept();
				
		SenderThread newSender = new SenderThread(inSoc,
                                                          "Testing message send");
				
		newSender.start();
	    }
	}
	catch (Exception e) {
	    System.out.println("Error in Sender: " + e.toString());
	}
    }
}
