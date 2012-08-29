package sockets;

import java.net.*;
import java.io.*;

public class ReceiverThread extends Thread {

    static Socket threadSoc;
    static BufferedReader reader;
    static String message;

    public static void main(String argv[]) {
        try {
	    ReceiverThread receiver = new ReceiverThread (
                new Socket("localhost",2001));
	    receiver.run ();
        }
        catch (Exception e) {
            System.out.println("Died... " + e.toString());
        }
    }
    
    ReceiverThread(Socket inSoc) {
	threadSoc = inSoc;
    }

    public void run() {
	try {
            reader = new BufferedReader(new InputStreamReader(threadSoc.getInputStream()));
            for (int i = 0; i < 100; i++) {
                message = reader.readLine();
                System.out.println(message);
            }
	}
	catch (Exception e) {
	    System.out.println("Error while receiving message: " + e.toString());
	}
		
	try {
	    threadSoc.close();
	}
	catch (Exception e) {
	    System.out.println("Error while closing Receiver socket: " + e.toString());
	}
    }

}

