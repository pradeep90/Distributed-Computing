package sockets;

import java.net.*;
import java.io.*;
import java.lang.*;

public class Receiver {

    public static void main(String argv[]) {
	
	try {
	    ServerSocket sSoc = new ServerSocket(2001);
			
	    while(true) {
		Socket inSoc = sSoc.accept();
				
		ReceiverThread newReceiver = new ReceiverThread(inSoc);
				
		newReceiver.start();
	    }
	}
	catch (Exception e) {
	    System.out.println("Oh Dear! " + e.toString());
	}
    }
}
