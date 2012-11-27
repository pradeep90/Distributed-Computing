package pessimconcurr;

import java.io.*;
import java.lang.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import sockets.ReceiverCallable;
import sockets.SenderThread;
import mutexlamport.LogicalClock;
import mutexlamport.TimeStamp;
import mutexlamport.Operation;
import mutexlamport.MutexMessage;
import mutexlamport.FileWriter;


/** 
 * Class to execute Transactions.
 *
 * For Bootstrapping, one of the peers is initially the Initializing
 * Server which waits for all the nodes to start before giving the Go
 * Ahead.
 */
public class TransactionExecutor {

    ServerSocket serverSocket;
    ExecutorService receiverExecutor;
    // ExecutorService senderExecutor;

    static final int SOCKET_READ_TIMEOUT = 200;
    static final int SLEEP_TIME = 50;

    LogicalClock clock;
    String selfHostname;
    int selfPort;
    String FINAL_MESSAGE = "Writing to Shared memory... ";
    int processId;
    int replyCounter;
    TimeStamp requestTimeStamp;
    boolean isNewRequest;
    boolean isWaitingForAck;
    boolean receivedGoAhead;
    String initServerHost;
    int initServerPort;
    FileWriter distributedFileWriter;
    TransactionOperation currentTransactionOperation;

    List<TransactionOperation> transactionOperationList;
    List<String> allHostnames;
    List<Integer> allPorts;

    Set<String> initNodes;
    PriorityQueue<TimeStamp> requestQueue;
    
    static final int NUM_SERVER_THREADS = 3;
    static final int NUM_SENDER_THREADS = 3;
    static final int MAX_TOTAL_REQUESTS = 1;

    public int numNodes;

    public static void main(String[] argv) {
        if (argv.length == 0){
	    System.out.println ("Format: id shared_file_name init_server_host:init_server_port host1:port1 [host2:port2 ...]");
	    System.exit (1);
        }

        List<String> hostPorts = new ArrayList<String>(Arrays.<String>asList(argv));
        System.out.println ("hostPorts");
        System.out.println (hostPorts);
        
	int processId = Integer.parseInt(hostPorts.remove(0));
        String sharedFileName = hostPorts.remove (0);

	TransactionExecutor transactionExecutor = new TransactionExecutor(
            processId, sharedFileName, hostPorts);
        transactionExecutor.bootstrap();
	transactionExecutor.startExecution ();
    }

    public TransactionExecutor(int processId, String sharedFileName, List<String> hostPorts){
        this.processId = processId;
        System.out.println ("Writing output to " + sharedFileName);

        
        String initServerHostPort = hostPorts.remove(0);
        initServerHost = initServerHostPort.split (":")[0];
        initServerPort = Integer.parseInt(initServerHostPort.split (":")[1]);

	List<String> allHostnames = new ArrayList<String> ();
	List<Integer> allPorts = new ArrayList<Integer> ();

        for (String hostPortPair : hostPorts){
            System.out.println ("hostPortPair");
            System.out.println (hostPortPair);
            allHostnames.add (hostPortPair.split (":")[0]);
            allPorts.add (Integer.parseInt (hostPortPair.split (":")[1]));
        }

        String selfHostname = allHostnames.get (processId);
        int selfPort = allPorts.get (processId);

        initValues(
            processId,
            selfHostname,
            selfPort,
            allHostnames,
            allPorts,
            new FileWriter (sharedFileName));
    }

    public void initValues (int processId,
                            String selfHostname,
                            int selfPort,
                            // List<TransactionOperation> transactionOperationList,
                            List<String> allHostnames,
                            List<Integer> allPorts,
                            FileWriter distributedFileWriter) {
        this.processId = processId;
        this.selfHostname = selfHostname;
        this.selfPort = selfPort;
        // this.transactionOperationList = transactionOperationList;
	this.allHostnames = allHostnames;
	this.allPorts = allPorts;
        numNodes = allHostnames.size ();
        clock = new LogicalClock (this.processId);
        requestQueue = new PriorityQueue<TimeStamp>();
        this.distributedFileWriter = distributedFileWriter;

        initNodes = new HashSet<String>();

        getTransactionOperationList();

        try {
            serverSocket = new ServerSocket (selfPort);
            // Pool of threads to which receiver jobs can be submitted.
            receiverExecutor = Executors.newFixedThreadPool(NUM_SERVER_THREADS);
            // // Pool of threads to which sender jobs can be submitted.
            // senderExecutor = Executors.newFixedThreadPool(NUM_SENDER_THREADS);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Read a list of transactionOperations from inputReader.
     */
    public static List<TransactionOperation> getTransactionOperationListFromReader(BufferedReader inputReader)
            throws IOException{
        List<TransactionOperation> transactionOperationList = new ArrayList<TransactionOperation> ();
        String currLine;
        while ((currLine = inputReader.readLine ()) != null){
            System.out.println (currLine);
            TransactionOperation transactionOperation = new TransactionOperation (currLine);
            System.out.println (transactionOperation);
            transactionOperationList.add (transactionOperation);
        }
        return transactionOperationList;
    }

    /**
     * Read list of transactionOperations from stdin.
     */
    public void getTransactionOperationList(){
        transactionOperationList = new ArrayList<TransactionOperation>();
        try {
            BufferedReader inputReader = new BufferedReader (
                new InputStreamReader (System.in));
            transactionOperationList = getTransactionOperationListFromReader(inputReader);
        } catch (IOException e) {
            e.printStackTrace ();
        }
    }


    /**
     * Bootstrap by sending messages to a centralized init server
     * until it gives you the go-ahead.
     */
    public void bootstrap(){
        receivedGoAhead = false;
        
        try {
            while(!receivedGoAhead){
                sendInitRequest();
                Thread.sleep (SLEEP_TIME);
                handleRequests();
            }
        } catch (InterruptedException e) {
	    System.out.println ("Error while trying for mutual exclusion:"
				+ e.toString ());
            e.printStackTrace ();
        }
    }
    
    /**
     * Whenever possible, send transaction operations out to the data items.
     *
     * Handle any requests or acks sent by other nodes.
     */
    public void startExecution (){
        isNewRequest = false;
        isWaitingForAck = false;

        HashMap<Integer, TimeStamp> transactionTimeStampHash =
                new HashMap<Integer, TimeStamp>();
        
        try {
	    while (true){
                Thread.sleep (SLEEP_TIME);

	    	handleRequests ();
                System.out.println (getTimeStampedMessage (
                    "requestQueue" + requestQueue.toString ()));

                if (allOperationsOver()){
                    continue;
                }

                if (!isWaitingForAck){
                    System.out.println ("transactionOperationList");
                    printTimeStampedMessage (transactionOperationList.toString ());
                    currentTransactionOperation = transactionOperationList.remove (0);

                    if (!transactionTimeStampHash.containsKey(
                            currentTransactionOperation.transactionId)){
                        transactionTimeStampHash.put(
                            currentTransactionOperation.transactionId,
                            clock.getTimeStamp());
                    }
                    currentTransactionOperation.transactionTimeStamp =
                            transactionTimeStampHash.get(
                                currentTransactionOperation.transactionId);

                    // sendOperation(currentTransactionOperation);

                    // TODO(spradeep): Should this be here?
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
     * @return true iff transactionOperationList is empty.
     */
    public boolean allOperationsOver(){
        return !isWaitingForAck && transactionOperationList.isEmpty();
    }

    /**
     * Send INIT request to the Init Server.
     */
    public void sendInitRequest(){
        sendMessage(initServerHost,
                    initServerPort,
                    getTimeStampedMessage("INIT" + " " + selfHostname + ":" + selfPort));
    }

    /** 
     * Check for incoming TransactionOperation messages or Acks and
     * handle them.
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
     * + TransactionOperation Request - Execute it and send Ack.
     * + Ack - Set isWaitingForAcks to false.
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
        if (mutexMessage.isOperationRequest()){

            executeOperation(TransactionOperation.fromTimeStampedString(
                mutexMessage.toString()));

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
        } else if (mutexMessage.isInitRequest()) {
            if (initNodes.size() == numNodes){
                return;
            }

            initNodes.add(mutexMessage.getMessage().split(" ")[1]);
            if (initNodes.size() == numNodes){
                try {
                    broadcastMessage(getTimeStampedMessage("GO_AHEAD_INIT"));
                    // Note: You need to send a message to yourself as well
                    sendMessage(selfHostname, selfPort,
                                getTimeStampedMessage("GO_AHEAD_INIT"));
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if (mutexMessage.isInitResponse()){
            receivedGoAhead = true;
        }
    }

    public void executeOperation(TransactionOperation op){
        ;
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
     * Send message to node with host:port.
     */
    void sendMessage(String host, int port, String message){
        try {
            sendMessage (new Socket(host, port), message);
        } catch (Exception e) {
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

