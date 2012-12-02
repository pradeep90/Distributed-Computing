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
    int operationIndex;
    int transactionStartIndex;
    int currTransactionId;
    String lastExecutionOutput;
    int numOutstandingAcks;
    boolean hasTransactionJustEnded = false;
    boolean lastTransactionCommitDone = false;

    HashSet<Integer> completedTransactions;

    HashMap<Integer, TimeStamp> transactionTimeStampHash;
    HashMap<String, Integer> dataItemLocationHash;
    HashMap<String, DataItem> dataItemHash;
    HashMap<Integer, Integer> startIndexHash;
        
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
	    System.out.println ("Format: id shared_file_name data_items_file init_server_host:init_server_port host1:port1 [host2:port2 ...]");
	    System.exit (1);
        }

        List<String> hostPorts = new ArrayList<String>(Arrays.<String>asList(argv));
        System.out.println ("hostPorts");
        System.out.println (hostPorts);
        
	int processId = Integer.parseInt(hostPorts.remove(0));
        String sharedFileName = hostPorts.remove (0);
        String dataItemsFileName = hostPorts.remove (0);

        HashMap<String, Integer> dataItemLocationHash = getDataItemLocationHash(
            dataItemsFileName);

        System.out.println ("dataItemLocationHash");
        System.out.println (dataItemLocationHash);

	TransactionExecutor transactionExecutor = new TransactionExecutor(
            processId, sharedFileName, hostPorts, dataItemLocationHash);
        transactionExecutor.bootstrap();
	transactionExecutor.startExecution ();
    }

    public TransactionExecutor(int processId, String sharedFileName,
                               List<String> hostPorts,
                               HashMap<String, Integer> dataItemLocationHash){
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
            new FileWriter (sharedFileName),
            dataItemLocationHash);
    }

    public TransactionExecutor(){
        ;
    }


    public void initValues (int processId,
                            String selfHostname,
                            int selfPort,
                            // List<TransactionOperation> transactionOperationList,
                            List<String> allHostnames,
                            List<Integer> allPorts,
                            FileWriter distributedFileWriter,
                            HashMap<String, Integer> dataItemLocationHash) {
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
        transactionTimeStampHash = new HashMap<Integer, TimeStamp>();
        this.dataItemLocationHash = dataItemLocationHash;
        dataItemHash = new HashMap<String, DataItem>();
        startIndexHash = new HashMap<Integer, Integer>();
        completedTransactions = new HashSet<Integer>();

        for (String label : dataItemLocationHash.keySet()){
            if (dataItemLocationHash.get(label) == processId){
                dataItemHash.put(label, new DataItem(label));
            }
        }

        operationIndex = 0;
        transactionStartIndex = 0;
        lastExecutionOutput = null;
        numOutstandingAcks = 0;
        currTransactionId = -1;

        initNodes = new HashSet<String>();

        getTransactionOperationList();

        for (int i = 0; i < transactionOperationList.size(); i++){
            if (!startIndexHash.containsKey(transactionOperationList.get(i).transactionId)){
                startIndexHash.put(transactionOperationList.get(i).transactionId, i);
            }
        }

        System.out.println ("startIndexHash");
        System.out.println (startIndexHash); 

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
    public static List<TransactionOperation> getTransactionOperationListFromReader(
        BufferedReader inputReader) throws IOException{
        List<TransactionOperation> transactionOperationList =
                new ArrayList<TransactionOperation> ();
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
     * @return hash of data item label -> pid of node having the data item.
     */
    public static HashMap<String, Integer> getDataItemLocationHash(String filename){
        HashMap<String, Integer> locationHash = new HashMap<String, Integer>();

        try {

            FileInputStream fstream = new FileInputStream(filename);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println ("line");
                System.out.println (line);
                locationHash.put(line.split(" ")[0],
                                 Integer.parseInt(line.split(" ")[1]));
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace ();
        }
        return locationHash;
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

        try {
	    while (true){
                Thread.sleep (SLEEP_TIME);

	    	handleRequests ();

                tryExecuteDataItemOps();

                System.out.println(getTimeStampedMessage(
                    "currTransactionId: " + currTransactionId));

                System.out.println("dataItemHash");
                System.out.println(getTimeStampedMessage(dataItemHash.toString()));

                if (hasTransactionJustEnded){
                    System.out.println(
                        getTimeStampedMessage("End of previous transaction.")); 
                    hasTransactionJustEnded = false;
                    broadcastCommit(transactionOperationList.get(operationIndex - 1));
                }

                if (allOperationsOver() && !lastTransactionCommitDone){
                    System.out.println(
                        getTimeStampedMessage("End of previous transaction.")); 
                    lastTransactionCommitDone = true;
                    broadcastCommit(transactionOperationList.get(operationIndex - 1));
                }

                if (allOperationsOver() || isWaitingForAck){
                    continue;
                }

                currentTransactionOperation = getNextTransactionOperation();
                sendOperation(currentTransactionOperation);

                // TODO(spradeep): Should this be here?
                clock.update ();
            }
        } catch (Exception e) {
	    System.out.println ("Error while trying for mutual exclusion:"
				+ e.toString ());
            e.printStackTrace();
        } finally {
            receiverExecutor.shutdown();
            // senderExecutor.shutdown();
        }
    }

    /**
     * @return true iff transactionOperationList is empty.
     */
    public boolean allOperationsOver(){
        return !isWaitingForAck && operationIndex >= transactionOperationList.size();
    }

    /**
     * Send INIT request to the Init Server.
     */
    public void sendInitRequest(){
        System.out.println(getTimeStampedMessage("INIT" + " " + selfHostname + ":" + selfPort));
        sendMessage(initServerHost,
                    initServerPort,
                    getTimeStampedMessage("INIT" + " " + selfHostname + ":" + selfPort));
    }

    /**
     * Assumption: transactionOperationList is not empty.
     *
     * Get the next operation to be executed.
     * If it's a new transaction, then assign its timestamp as the current timestamp.
     * 
     * @return the next TransactionOperation in the list.
     */
    public TransactionOperation getNextTransactionOperation(){
        // System.out.println ("transactionOperationList");
        // printTimeStampedMessage (transactionOperationList.toString ());
        TransactionOperation nextOperation = transactionOperationList.get (operationIndex);

        if (!transactionTimeStampHash.containsKey(nextOperation.transactionId)){
            // New transaction
            transactionTimeStampHash.put(nextOperation.transactionId, clock.getTimeStamp());
            transactionStartIndex = operationIndex;
            setCurrTransactionId(nextOperation.transactionId);
        }
        nextOperation.transactionTimeStamp = transactionTimeStampHash.get(
            nextOperation.transactionId);

        System.out.println(getTimeStampedMessage("operationIndex: " + operationIndex));
        operationIndex++;
        return nextOperation;
    }

    /** 
     * Set id for transaction currently in execution.
     *
     * Set hasTransactionJustEnded when a transaction has ended.
     */
    public void setCurrTransactionId(int nextId){
        if (nextId == currTransactionId){
            // Restart of the same transaction.
            return;
        }

        if (currTransactionId == -1){
            // First ever transaction (i.e., not coming after another
            // transaction)
            currTransactionId = nextId;
            initNewTransaction();
            return;
        }


        hasTransactionJustEnded = true;
        currTransactionId = nextId;
        initNewTransaction();

        // if (startIndexHash.values().contains(operationIndex)
        //     || operationIndex == transactionOperationList.size()){

        //     // System.out.println(getTimeStampedMessage("End of previous transaction.")); 
        // }

    }

    public void initNewTransaction(){
        numOutstandingAcks = 0;
    }


    /** 
     * Execute operations from the buffers of your data items (if
     * possible).
     * 
     */
    public void tryExecuteDataItemOps(){
        for (DataItem d : dataItemHash.values()){
            d.tryExecuteOps();
        }

        int i;
        for (DataItem d : dataItemHash.values()){
            while (d.nextReadAckIndex < d.readList.size()) {
                TransactionOperation currOp = d.readList.get(d.nextReadAckIndex);
                d.nextReadAckIndex++;

                AckMutexMessage ack = new AckMutexMessage(
                    currOp, false, processId);

                // System.out.println("Sending Ack... " +
                // getTimeStampedMessage(ack.toString()));

                sendMessage(currOp.transactionTimeStamp.getProcessId(),
                            getTimeStampedMessage(ack.toString()));
            }

            while (d.nextWriteAckIndex < d.writeList.size()) {
                TransactionOperation currOp = d.writeList.get(d.nextWriteAckIndex);
                d.nextWriteAckIndex++;

                AckMutexMessage ack = new AckMutexMessage(
                    currOp, false, processId);
                // System.out.println("Sending Ack... " + getTimeStampedMessage(ack.toString()));
                sendMessage(currOp.transactionTimeStamp.getProcessId(),
                            getTimeStampedMessage(ack.toString()));
            }
        }
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
                    // System.out.println (
                    //     getTimeStampedMessage ("No requests to the server"));
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
                    // System.out.println (getTimeStampedMessage (message));
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
     * + Ack - Set isWaitingForAck to false.
     * + Bootstrap message (Request / Go ahead) - deal with it.
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

            TransactionOperation op = TransactionOperation.fromTimeStampedString(
                mutexMessage.getMessage());

            boolean isSuccess = canExecuteOperation(op);

            TimeStamp messageTimeStamp = mutexMessage.getTimeStamp ();

            AckMutexMessage ack = new AckMutexMessage(op, !isSuccess, processId);

            sendMessage(op.transactionTimeStamp.getProcessId(),
                        getTimeStampedMessage(ack.toString()));
        } else if (AckMutexMessage.isAckMutexMessage(message)){
            AckMutexMessage ack = new AckMutexMessage(message);
            System.out.println(getTimeStampedMessage("Received Ack... " + ack.toString())); 

            if (ack.val == "Commit"){
                for (DataItem d : dataItemHash.values()){
                    d.markTransactionForCommit(ack.getTimeStamp());
                }
            } else if (ack.isTransactionRejected){
                System.out.println(getTimeStampedMessage("Restarting transaction..."));
                // Restart transaction.
                operationIndex = transactionStartIndex;
                // Remove old TS assigned to the transaction.
                transactionTimeStampHash.remove(currTransactionId);
                isWaitingForAck = false;
            } else if (ack.isSuccessfullyCompleted()){
                isWaitingForAck = false;

                if (ack.op.operationType == Operation.OperationType.READ){
                    numOutstandingAcks--;
                } else if (ack.op.operationType == Operation.OperationType.WRITE
                           && !ack.op.isPreWrite){

                    // For pre-writes, the actual completion Ack will
                    // come after commit, so decrement
                    // numOutstandingAcks only for non pre-write Acks
                    numOutstandingAcks--;
                }
            }
        } else if (mutexMessage.isInitRequest()) {
            if (initNodes.size() == numNodes){
                return;
            }

            initNodes.add(mutexMessage.getMessage().split(" ")[1]);
            if (initNodes.size() == numNodes){
                try {
                    System.out.println(getTimeStampedMessage(
                        "I am the Bootstrap Server. Sending GO AHEAD...")); 
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
            System.out.println(getTimeStampedMessage("Received Go Ahead...")); 
        }
    }

    /** 
     * Send op to the node having op's data item.
     * 
     * Set isWaitingForAck as true.
     */
    public void sendOperation(TransactionOperation op){
        int destinationId = dataItemLocationHash.get(op.dataItemLabel);
        System.out.println(getTimeStampedMessage("Sending operation... " + op.toString()));
        sendMessage(destinationId, getTimeStampedMessage(op.toString()));

        isWaitingForAck = true;
        numOutstandingAcks++;
    }

    /** 
     * Broadcast COMMIT message.
     * 
     * Set isWaitingForAck as true.
     */
    public void broadcastCommit(TransactionOperation op){
        // Defining a READ op because only an Ack for a Read can have
        // a `val` field showing up in the toString() output.
        TransactionOperation tempOp = new TransactionOperation(op.transactionId + " R x")
                .setTimeStamp(op.transactionTimeStamp);
        AckMutexMessage commitAck = new AckMutexMessage(tempOp, false, processId);
        commitAck.val = "Commit";

        try {
            String messageString = getTimeStampedMessage(commitAck.toString());
            System.out.println(messageString);
            System.out.println("messageString: " + messageString);

            broadcastMessage(messageString);

            // Note: You need to send a message to yourself as well
            sendMessage(selfHostname, selfPort, messageString);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        isWaitingForAck = true;
    }

    /** 
     * @return true iff execution of op on the appropriate data item
     * is possible.
     */
    public boolean canExecuteOperation(TransactionOperation op){
        DataItem d = dataItemHash.get(op.dataItemLabel);
        return d.canExecuteOperation(op);
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

