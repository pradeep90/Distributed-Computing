package sockets;

import java.net.*;
import java.io.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ReceiverCallable implements Callable<String> {

    Socket socket;
    BufferedReader reader;

    public static void main(String argv[]) {
        try {
            // Pool of threads to which jobs can be submitted.
            ExecutorService executor = Executors.newFixedThreadPool(1);

            String server_host;
            int server_port;
            if (argv.length == 0){
                System.out.println ("Format: server_host:server_port");
                System.exit (0);
            }

            server_host = argv[0].split (":")[0];
            server_port = Integer.parseInt (argv[0].split (":")[1]);
            
            // Create a ReceiverCallable thread
            Callable<String> worker = new ReceiverCallable(
                new Socket(server_host, server_port));
            // Submit it to the Thread Pool
            Future<String> future = executor.submit(worker);

            try {
                System.out.println ("ReceiverCallable main:" + future.get());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            executor.shutdown();
        }
        catch (Exception e) {
            System.out.println("Died... " + e.toString());
        }
    }
    
    public ReceiverCallable(Socket inSoc) {
	socket = inSoc;
    }

    /** 
     * @return the message received at socket.
     */
    public String call() {
        String message = "";
	try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line = reader.readLine();
            message += line;
            // System.out.println("ReceiverCallable: " + message);
	}
	catch (Exception e) {
	    System.out.println("Error while receiving message: " + e.toString());
	}
		
	try {
	    socket.close();
	}
	catch (Exception e) {
	    System.out.println("Error while closing Receiver socket: " + e.toString());
	}

        return message;
    }
}

