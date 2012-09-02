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

    ServerSocket serverSocket;
    ExecutorService receiverExecutor;
    // ExecutorService senderExecutor;

    static final int SOCKET_READ_TIMEOUT = 200;

    LogicalClock clock;
    String server_hostname;
    int server_port;
    int processId;
    List<String> allHostnames;
    List<Integer> allPorts;
    
    static final int NUM_SERVER_THREADS = 3;
    static final int NUM_SENDER_THREADS = 3;
    static final int MAX_TOTAL_REQUESTS = 1;

    public int numNodes;

    public static void main(String[] argv) {
        if (argv.length == 0){
	    System.out.println ("Format: id host1:port1 [host2:port2 ...]");
	    System.exit (1);
        }
	
	List<String> allHostnames = new ArrayList<String> ();
	List<Integer> allPorts = new ArrayList<Integer> ();

        List<String> hostPorts = new ArrayList<String> ();
        for (String arg : argv){
            hostPorts.add (arg);
        }

        // The first arg is the processId
	int processId = Integer.parseInt (hostPorts.remove (0));

        String server_hostname;
        int server_port;
        System.out.println ("processId");
        System.out.println (processId);
        
        for (String hostPortPair : hostPorts){
            System.out.println (hostPortPair);
            allHostnames.add (hostPortPair.split (":")[0]);
            allPorts.add (Integer.parseInt (hostPortPair.split (":")[1]));
        }

        server_hostname = allHostnames.get (processId);
        server_port = allPorts.get (processId);

	System.out.println (allHostnames);
	System.out.println (allPorts);

	MutexExecutor mutexExecutor = new MutexExecutor (processId,
                                                         server_hostname,
                                                         server_port,
                                                         allHostnames,
                                                         allPorts);
	mutexExecutor.startExecution ();
    }

    MutexExecutor (int processId,
                   String server_hostname,
                   int server_port,
                   List<String> allHostnames,
                   List<Integer> allPorts) {
        this.server_hostname = server_hostname;
        this.server_port = server_port;
        this.processId = processId;
	this.allHostnames = allHostnames;
	this.allPorts = allPorts;
        numNodes = allHostnames.size ();
        clock = new LogicalClock (this.processId);
    }
    
    public void startExecution (){

        boolean isNewRequest = false;
        boolean isWaitingForAcks = false;
        boolean isAtHeadOfRQ = false;

        try {
	    serverSocket = new ServerSocket (server_port);

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
                    // No requests to the server - try again
                    continue;
                }
                
                // Create a ReceiverCallable thread
                Callable<String> worker = new ReceiverCallable(newSocket);
                // Submit it to the Thread Pool
                Future<String> future = receiverExecutor.submit(worker);

                try {
                    message += future.get();

                    // TODO(spradeep): Handle the message
                    handleMessage (message);
                    // System.out.println ("getTimeStampedMessage (message)");
                    System.out.println (getTimeStampedMessage (message));
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
            e.printStackTrace ();
    	}
    }

    /**
     * If message is:
     * + Request - Add to RQ and send Ack
     * + Release - Remove original request from RQ
     *
     * Update clock based on the message.
     */
    void handleMessage (String message){
        clock.update ();
        // TODO(spradeep): if the message is not an ACK send an ack.
        // Send ack
        if (!MutexMessage.isAck (message)){
            TimeStamp messageTimeStamp = new MutexMessage (message).getTimeStamp ();
            sendMessage (messageTimeStamp.getProcessId (),
                         "ACK " + messageTimeStamp.getTimeValue ());
            
        }
    }
    
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
            System.out.println("sendMessage error: " + e.toString());
        }
    }

    /**
     * Send message to peer with PID peerId.
     */
    void sendMessage (int peerId, String message){
        try {
            sendMessage (new Socket(allHostnames.get (peerId),
                                    allPorts.get (peerId)),
                         message);
        } catch (Exception e) {
            System.out.println("sendMessage error: " + e.toString());
        }
    }
    
    void sendRequestToAll () throws UnknownHostException, IOException {
        for (int peerId = 0; peerId < allHostnames.size (); peerId++){
            if (peerId == processId){
                continue;
            }

            String requestMessage = getTimeStampedMessage ("REQUEST");
            sendMessage (peerId, requestMessage);
        }
    }

    public void sendAck (String message){
        
    }

    public String getTimeStampedMessage (String message){
        return new MutexMessage (clock.getTimeStamp (),
                                 "[ " + message + " ]")
                .toString ();
    }

    // void sendRelease (){
    //     ;
    // }
}
