package mutexlamport;

import java.io.*;
import java.lang.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import sockets.ReceiverCallable;
import sockets.SenderThread;

public class MutexExecutor {

    LogicalClock clock;
    ServerSocket serverSocket;
    ExecutorService receiverExecutor;
    // ExecutorService senderExecutor;

    static final int SOCKET_READ_TIMEOUT = 200;

    String hostname;
    int port;
    int processId;
    List<String> peerHostnames;
    List<String> peerPortnames;
    
    static final int NUM_SERVER_THREADS = 3;
    static final int NUM_SENDER_THREADS = 3;
    static final int MAX_TOTAL_REQUESTS = 1;

    public int numNodes;

    public static void main(String[] argv) {
        int port;
        
        if (argv.length == 0){
	    System.out.println ("Format: id host1:port1 [host2:port2 ...]");
	    System.exit (1);
        }
	
	List<String> peerHostnames = new ArrayList<String> ();
	List<String> peerPortnames = new ArrayList<String> ();

	int processId = Integer.parseInt (argv[0]);
        System.out.println ("processId");
        System.out.println (processId);
        
        for (int i = 1; i < argv.length; i++){
            String hostPortPair = argv[i];
            System.out.println (hostPortPair);
            peerHostnames.add (hostPortPair.split (":")[0]);
            peerPortnames.add (hostPortPair.split (":")[1]);
        }

	System.out.println (peerHostnames);
	System.out.println (peerPortnames);

	MutexExecutor mutexExecutor = new MutexExecutor (processId,
                                                         peerHostnames, peerPortnames);

	mutexExecutor.startExecution ();
	
    }

    MutexExecutor (int processId,
                   List<String> peerHostnames,
                   List<String> peerPortnames) {
        this.processId = processId;
	this.peerHostnames = peerHostnames;
	this.peerPortnames = peerPortnames;
        numNodes = peerHostnames.size () + 1;
    }
    
    public void startExecution (){

        boolean isNewRequest = false;
        boolean isWaitingForAcks = false;
        boolean isAtHeadOfRQ = false;

        try {
	    serverSocket = new ServerSocket (
                Integer.parseInt (this.peerPortnames.get (0)));

	    // Pool of threads to which receiver jobs can be submitted.
	    receiverExecutor = Executors.newFixedThreadPool(NUM_SERVER_THREADS);

	    // // Pool of threads to which sender jobs can be submitted.
	    // senderExecutor = Executors.newFixedThreadPool(NUM_SENDER_THREADS);

	    while (true){
                Thread.sleep (1000);

	    	handleRequests ();
        
	    	// TODO(spradeep): Do stuff to see whether you wanna make a new request
	    	Random randomGenerator = new Random ();
	    	isNewRequest =
                        randomGenerator.nextInt (numNodes) == processId ? true: false;

                // if (processId == 0){
                //     isNewRequest = true;
                // }

	    	// If not making a new request and not waiting for Acks, loop 
	    	if (!isNewRequest && !isWaitingForAcks){
	    	    continue;
	    	}

	    	// If making a new request, send requests to all nodes
	    	if (isNewRequest){
	    	    sendRequestToAll ();
	    	    isWaitingForAcks = true;
	    	    isNewRequest = false;
	    	}

	    	// // If waiting for Acks and you have received Acks from
	    	// // everyone and you are at the head of RQ, ENTER CS
	    	// if (isWaitingForAcks && checkAllAcksReceived () && isAtHeadOfRQ ()){
                isWaitingForAcks = false;
	    	//     // TODO(spradeep): ENTER CS

	    	//     // EXIT CS - Dequeue your request and send Release message to all nodes
	    	//     dequeueRequest (yourRequest);
	    	//     sendReleaseMessages ();
	    	// }
	    }
	} catch (Exception e) {
	    System.out.println ("Error while trying for mutual exclusion:"
				+ e.toString ());
        } finally {
            receiverExecutor.shutdown();
            // senderExecutor.shutdown();
        }
    }


    // boolean isAtHeadOfRQ (){
    //     ;
    // }
    
    // boolean checkAllAcksReceived (){
    //     ;
    // }
    
    // MutexExecutor (String givenFilename){

    // }

    /** 
     * Check for incoming Request/Release messages and queue/dequeue
     * the relevant Requests.
     */
    void handleRequests (){
        String message = "";
        
    	try {
            serverSocket.setSoTimeout (SOCKET_READ_TIMEOUT);
    	    for (int i = 0; i < MAX_TOTAL_REQUESTS; i++) {
                System.out.println ("Before accepting a new request");
                Socket newSocket = null;
                
                try {
                    newSocket = serverSocket.accept();
                } catch (SocketTimeoutException e) {
                    System.out.println ("No requests to the server");
                    // No requests to the server
                    return;
                }

                // Create a ReceiverCallable thread
                Callable<String> worker = new ReceiverCallable(newSocket);
                // Submit it to the Thread Pool
                Future<String> future = receiverExecutor.submit(worker);

                try {
                    message += future.get();

                    // TODO(spradeep): Handle the message
                    System.out.println (processId + " - Handle Request: " + message);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                System.out.println ("After dealing with a request (if any)");

    	    }
    	}
    	catch (Exception e) {
    	    System.out.println("Error in the server: " + e.toString());
    	}
    }

    // String makeRequest (foo, bar){
    //     ;
    // }
    
    
    /** 
     * Maybe have a pool of sender threads later.
     */
    void sendMessage (Socket socket, String message){
        try {
	    SenderThread senderThread = new SenderThread (socket, message);
	    senderThread.run ();
            senderThread.join ();
        }
        catch (Exception e) {
            System.out.println("Died... " + e.toString());
        }
    }
    
    void sendRequestToAll () throws UnknownHostException, IOException {
        for (int i = 0; i < peerHostnames.size (); i++){
            String requestMessage = processId + " - Yo, boyz! I am send request.";
            sendMessage (new Socket(peerHostnames.get (i),
                                    Integer.parseInt (peerPortnames.get (i))),
                         requestMessage);
        }
    }
    
    // void sendRelease (){
    //     ;
    // }
    

}
