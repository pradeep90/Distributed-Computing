package sockets;

import java.net.*;
import java.io.*;
import java.lang.*;

// TODO(spradeep): Make this implement Callable
public class SenderThread extends Thread {

    Socket threadSoc;
    String message;

    int F1 = 1;
    int F2 = 1;


    public static void main(String argv[]) {
        try {
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
            
	    SenderThread sender = new SenderThread (
                new Socket(server_host, server_port), message);
	    sender.run ();
        }
        catch (Exception e) {
            System.out.println("Died... " + e.toString());
        }
    }


    public SenderThread(Socket inSoc, String message) {
	threadSoc = inSoc;
        this.message = message;
    }
	
    public void run() {
	try {
	    PrintStream Out = new PrintStream(threadSoc.getOutputStream());
            Out.print(message);
	}
	catch (Exception e) {
	    System.out.println("Error sending message: " + e.toString());
	}
		
	try {
	    threadSoc.close();
	}
	catch (Exception e) {
	    System.out.println("Error closing sender socket: " + e.toString());
	}
    }
}
