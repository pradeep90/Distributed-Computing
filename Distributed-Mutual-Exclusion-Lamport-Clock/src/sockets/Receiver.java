package sockets;

import java.net.*;
import java.io.*;
import java.lang.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Receiver implements Runnable {

    ServerSocket serverSocket;
    ExecutorService executor;
    
    static final int NUM_THREADS = 3;
    static final int MAX_TOTAL_REQUESTS = 10;

    public Receiver (ServerSocket serverSocket){
        this.serverSocket = serverSocket;

	// Pool of threads to which jobs can be submitted.
	executor = Executors.newFixedThreadPool(NUM_THREADS);
    }

    public void run (){
        String message = "";
        
	try {
	    for (int i = 0; i < MAX_TOTAL_REQUESTS; i++) {
                System.out.println ("Before accepting a new request");

                Socket newSocket = serverSocket.accept();

                // Create a ReceiverCallable thread
                Callable<String> worker = new ReceiverCallable(newSocket);
                // Submit it to the Thread Pool
                Future<String> future = executor.submit(worker);

                try {
                    message += future.get();
                    System.out.println ("Receiver: " + message);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                System.out.println ("After dealing with a request (if any)");

	    }
            executor.shutdown();
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
