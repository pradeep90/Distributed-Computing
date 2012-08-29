import java.net.*;
import java.io.*;
import java.lang.*;

class SenderThread extends Thread {

    Socket threadSoc;

    int F1 = 1;
    int F2 = 1;


    public static void main(String argv[]) {
        try {
	    Socket appSoc;
            appSoc = new Socket("localhost", 2001);
	    SenderThread sender = new SenderThread (appSoc);
	    sender.run ();
        }
        catch (Exception e) {
            System.out.println("Died... " + e.toString());
        }
    }


    SenderThread(Socket inSoc) {
	threadSoc = inSoc;
    }
	
    public void run() {
	try {
	    PrintStream FibOut = new PrintStream(threadSoc.getOutputStream());
						
	    while (true) {
		int temp;

		temp = F1;

		FibOut.println(F1);
		Thread.sleep(500);

		F1 = F2;
		F2 = temp + F2;
	    }
	}
	catch (Exception e) {
	    System.out.println("Whoops! " + e.toString());
	}
		
	try {
	    threadSoc.close();
	}
	catch (Exception e) {
	    System.out.println("Oh no! " + e.toString());
	}
    }
}
