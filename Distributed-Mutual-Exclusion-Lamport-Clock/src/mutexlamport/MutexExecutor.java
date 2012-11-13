package mutexlamport;

import java.io.*;
import java.lang.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
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
    static final int SLEEP_TIME = 1;

    LogicalClock clock;
    String server_hostname;
    String FINAL_MESSAGE = "Writing to Shared memory... ";
    int server_port;
    int processId;
    int replyCounter;
    TimeStamp requestTimeStamp;
    boolean isNewRequest;
    boolean isWaitingForAcks;
    FileWriter distributedFileWriter;
    Operation currentOperation;
    
    List<Operation> operationList;
    List<String> allHostnames;
    List<Integer> allPorts;

    PriorityQueue<TimeStamp> requestQueue;
    
    static final int NUM_SERVER_THREADS = 3;
    static final int NUM_SENDER_THREADS = 3;
    static final int MAX_TOTAL_REQUESTS = 1;

    public int numNodes;

    public static void main(String[] argv) {
        if (argv.length == 0){
	    System.out.println ("Format: id shared_file_name host1:port1 [host2:port2 ...]");
	    System.exit (1);
        }

        List<Operation> operationList = new ArrayList<Operation> ();
        try {
            BufferedReader inputReader = new BufferedReader (
                new InputStreamReader (System.in));
            String currLine;
	    while ((currLine = inputReader.readLine ()) != null){
                System.out.println (currLine);
                Operation operation = new Operation (currLine);
                System.out.println (operation);
                operationList.add (operation);
            }
        } catch (IOException e) {
            e.printStackTrace ();
        }
	
	List<String> allHostnames = new ArrayList<String> ();
	List<Integer> allPorts = new ArrayList<Integer> ();

        List<String> hostPorts = new ArrayList<String> ();
        for (String arg : argv){
            hostPorts.add (arg);
        }

        // The first arg is the processId
	int processId = Integer.parseInt (hostPorts.remove (0));
        String sharedFileName = hostPorts.remove (0);
        System.out.println ("Writing output to " + sharedFileName);

        String server_hostname;
        int server_port;
        
        for (String hostPortPair : hostPorts){
            System.out.println ("hostPortPair");
            System.out.println (hostPortPair);
            allHostnames.add (hostPortPair.split (":")[0]);
            allPorts.add (Integer.parseInt (hostPortPair.split (":")[1]));
        }

        server_hostname = allHostnames.get (processId);
        server_port = allPorts.get (processId);

	MutexExecutor mutexExecutor = new MutexExecutor (
            processId,
            server_hostname,
            server_port,
            operationList,
            allHostnames,
            allPorts,
            new FileWriter (sharedFileName));
	mutexExecutor.startExecution ();
    }

    public MutexExecutor (int processId,
                          String server_hostname,
                          int server_port,
                          List<Operation> operationList,
                          List<String> allHostnames,
                          List<Integer> allPorts,
                          FileWriter distributedFileWriter) {
        this.processId = processId;
        this.server_hostname = server_hostname;
        this.server_port = server_port;
        this.operationList = operationList;
	this.allHostnames = allHostnames;
	this.allPorts = allPorts;
        numNodes = allHostnames.size ();
        clock = new LogicalClock (this.processId);
        requestQueue = new PriorityQueue<TimeStamp>();
        this.distributedFileWriter = distributedFileWriter;
    }
    
    public void startExecution (){
        isNewRequest = false;
        isWaitingForAcks = false;
        try {
	    serverSocket = new ServerSocket (server_port);

	    // Pool of threads to which receiver jobs can be submitted.
	    receiverExecutor = Executors.newFixedThreadPool(NUM_SERVER_THREADS);

	    // // Pool of threads to which sender jobs can be submitted.
	    // senderExecutor = Executors.newFixedThreadPool(NUM_SENDER_THREADS);

	    while (true){
                Thread.sleep (SLEEP_TIME);

	    	handleRequests ();
                System.out.println (getTimeStampedMessage (
                    "requestQueue" + requestQueue.toString ()));

                if (allOperationsOver()){
                    continue;
                }

                if (!isWaitingForAcks){
                    System.out.println ("operationList");
                    printTimeStampedMessage (operationList.toString ());
                    currentOperation = operationList.remove (0);
                    isNewRequest = currentOperation.operationType
                            == Operation.OperationType.WRITE;
                    if (!isNewRequest){
                        continue;
                    }
                    makeNewRequest ();
                    // TODO(spradeep): Should this be here?
                    clock.update ();
                } else if (canEnterCS ()){
                    enterCS ();
                    executeCS ();
                    exitCS ();
                    clock.update ();
	    	}
	    }
	} catch (Exception e) {
	    System.out.println ("Error while trying for mutual exclusion:"
				+ e.toString ());
        } finally {
            receiverExecutor.shutdown();
            // senderExecutor.shutdown();
        }
    }

    /**
     * Add current node's new request to its Request Queue and
     * broadcast a Request to all peers.
     *
     * Update clock and set the various flags to make it wait for acks.
     */
    public void makeNewRequest()
            throws UnknownHostException, IOException {
        requestTimeStamp = clock.getTimeStamp ();
        requestQueue.add (clock.getTimeStamp ());
        System.out.println (getTimeStampedMessage ("Sending request..."));
        broadcastMessage (getTimeStampedMessage ("REQUEST"));
        isWaitingForAcks = true;
        isNewRequest = false;
        replyCounter = numNodes - 1;
    }

    /**
     * @return true iff operationList is empty.
     */
    public boolean allOperationsOver(){
        return !isWaitingForAcks && operationList.isEmpty();
    }


    /**
     * Return true iff current node can enter CS.
     */
    public boolean canEnterCS (){
        return checkAllAcksReceived () && isAtHeadOfRQ ();
    }

    /**
     * Enter CS.
     */
    public void enterCS(){
        isWaitingForAcks = false;
    }

    /**
     * Execute code in CS.
     */
    public void executeCS (){
        System.out.println (getTimeStampedMessage (
            FINAL_MESSAGE + currentOperation.parameter));
        distributedFileWriter.appendToFile (
            getTimeStampedMessage (
                FINAL_MESSAGE + currentOperation.parameter) + "\n");
    }

    /**
     * Dequeue your Request and send Release message to all nodes.
     */
    public void exitCS () throws UnknownHostException, IOException {
        requestQueue.poll ();

        // Send release message with the TS of the request
        System.out.println (getTimeStampedMessage (requestTimeStamp,
                                                   "RELEASE"));
        broadcastMessage (getTimeStampedMessage (requestTimeStamp,
                                                 "RELEASE"));
    }

    /**
     * @return true iff a request from this node is at the head of
     * local RQ.
     */
    boolean isAtHeadOfRQ (){
        return requestQueue.peek ().getProcessId () == processId;
    }
    
    /**
     * @return true iff Acks from all peers have been received.
     */
    boolean checkAllAcksReceived (){
        return replyCounter == 0;
    }
    
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
                // System.out.println ("Before accepting a new request");
                Socket newSocket = null;
                
                try {
                    newSocket = serverSocket.accept();
                } catch (SocketTimeoutException e) {
                    System.out.println (
                        getTimeStampedMessage ("No requests to the server"));
                    // No requests to the server - try again
                    continue;
                }
                
                // Create a ReceiverCallable thread
                Callable<String> worker = new ReceiverCallable(newSocket);
                // Submit it to the Thread Pool
                Future<String> future = receiverExecutor.submit(worker);

                try {
                    message += future.get();

                    // System.out.println ("getTimeStampedMessage (message)");
                    System.out.println (getTimeStampedMessage (message));
                    // TODO(spradeep): Handle the message
                    handleMessage (message);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
                // System.out.println ("After dealing with a request (if any)");
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
     * 
     * TODO(spradeep): Maybe pass in a MutexMessage instead of a
     * String and then use message.isRequest instead of having a
     * tell-tale (?) static method?
     */
    void handleMessage (String message){
        MutexMessage mutexMessage = new MutexMessage (message);
        clock.update ();
        if (mutexMessage.isRequest ()){
            // Send ack
            TimeStamp messageTimeStamp = mutexMessage.getTimeStamp ();
            requestQueue.add (messageTimeStamp);
            System.out.println (getTimeStampedMessage (
                "ACK " + messageTimeStamp.getProcessId ()
                + " from " + processId));
            sendMessage (messageTimeStamp.getProcessId (),
                         getTimeStampedMessage (
                             "ACK " + messageTimeStamp.getProcessId ()
                             + " from " + processId));
        } else if (mutexMessage.isAck ()){
            replyCounter--;
        } else if (mutexMessage.isRelease ()){
            boolean removed = requestQueue.remove (mutexMessage.getTimeStamp ());
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
    
    /**
     * Send message to all peers.
     */
    void broadcastMessage (String message) throws UnknownHostException, IOException {
        for (int peerId = 0; peerId < numNodes; peerId++){
            if (peerId == processId){
                continue;
            }
            sendMessage (peerId, message);
        }
    }

    public void sendAck (String message){
        
    }

    public void printTimeStampedMessage(String message){
        System.out.println (getTimeStampedMessage (message));
    }

    public String getTimeStampedMessage (String message){
        return getTimeStampedMessage (clock.getTimeStamp (), message);
    }

    public String getTimeStampedMessage (TimeStamp timeStamp, String message){
        return new MutexMessage (timeStamp,
                                 "[ " + message + " ]")
                .toString ();
    }
}

