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

    static final int SLEEP_TIME = 50;

    String selfHostname;
    int selfPort;
    String FINAL_MESSAGE = "Writing to Shared memory... ";
    int processId;
    int replyCounter;
    TimeStamp requestTimeStamp;
    boolean isNewRequest;
    boolean isWaitingForAck;
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
        
    List<Transaction> transactionList;
    List<TransactionOperation> transactionOperationList;
    HashMap<Integer, List<TransactionOperation>> transactionHash;
    List<String> allHostnames;
    List<Integer> allPorts;

    PriorityQueue<TimeStamp> requestQueue;

    public Util util;
    public RequestHandler requestHandler;
    
    static final int NUM_SERVER_THREADS = 3;
    static final int NUM_SENDER_THREADS = 3;

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
                            List<String> allHostnames,
                            List<Integer> allPorts,
                            FileWriter distributedFileWriter,
                            HashMap<String, Integer> dataItemLocationHash) {
        this.processId = processId;
        this.selfHostname = selfHostname;
        this.selfPort = selfPort;
	this.allHostnames = allHostnames;
	this.allPorts = allPorts;
        numNodes = allHostnames.size ();
        requestQueue = new PriorityQueue<TimeStamp>();
        this.distributedFileWriter = distributedFileWriter;

        completedTransactions = new HashSet<Integer>();

        operationIndex = 0;
        transactionStartIndex = 0;
        lastExecutionOutput = null;
        numOutstandingAcks = 0;
        currTransactionId = -1;

        try {
            serverSocket = new ServerSocket (selfPort);
            // Pool of threads to which receiver jobs can be submitted.
            receiverExecutor = Executors.newFixedThreadPool(NUM_SERVER_THREADS);
            // // Pool of threads to which sender jobs can be submitted.
            // senderExecutor = Executors.newFixedThreadPool(NUM_SENDER_THREADS);
        } catch (IOException e) {
            e.printStackTrace();
        }

        util = new Util(processId, selfHostname, selfPort,
                        numNodes, allHostnames, allPorts,
                        dataItemLocationHash);
        util.buildDataItemHash();

        // TODO(spradeep): 
        requestHandler = new RequestHandler(new ArrayList<AckMutexMessage>(),
                                            receiverExecutor,
                                            serverSocket,
                                            util);

        getTransactionOperationList();
        getTransactions();
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
     * Fill transactionList with groups of operations from transactionOperationList.
     */
    public void getTransactions(){
        transactionList = new ArrayList<Transaction>();

        List<TransactionOperation> currList = new ArrayList<TransactionOperation>();
        int currId = transactionOperationList.get(0).transactionId;
        int i = 0;
        while (i < transactionOperationList.size()){
            TransactionOperation op = transactionOperationList.get(i);
            if (op.transactionId == currId){
                currList.add(op);
                i++;
            } else {
                Transaction currTransaction = new Transaction(currId,
                                                              null,
                                                              currList,
                                                              requestHandler,
                                                              util);
                transactionList.add(currTransaction);
                currId = op.transactionId;
                currList = new ArrayList<TransactionOperation>();
            }
        }
        
        // Last transaction
        Transaction currTransaction = new Transaction(currId,
                                                      null,
                                                      currList,
                                                      requestHandler,
                                                      util);
        transactionList.add(currTransaction);
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
        util.receivedGoAhead = false;
        
        try {
            while(!util.receivedGoAhead){
                sendInitRequest();
                Thread.sleep (SLEEP_TIME);
                requestHandler.handleRequests();
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

        executeTransactions();

        while (true){
            requestHandler.handleRequests();
        }
        
        // try {
	//     while (true){
        //         Thread.sleep (SLEEP_TIME);

	//     	requestHandler.handleRequests ();

        //         tryExecuteDataItemOps();

        //         util.printTimeStampedMessage("currTransactionId: " + currTransactionId);

        //         util.printTimeStampedMessage("dataItemHash: " + util.dataItemHash.toString());

        //         if (hasTransactionJustEnded){
        //             util.printTimeStampedMessage("End of previous transaction."); 
        //             hasTransactionJustEnded = false;
        //             // TODO(spradeep): 
        //             // requestHandler.broadcastCommit(transactionOperationList.get(operationIndex - 1));
        //         }

        //         if (allOperationsOver() && !lastTransactionCommitDone){
        //             util.printTimeStampedMessage("End of previous transaction."); 
        //             lastTransactionCommitDone = true;
        //             // TODO(spradeep): 
        //             // requestHandler.broadcastCommit(
        //             //     transactionOperationList.get(operationIndex - 1));
        //         }

        //         if (allOperationsOver() || isWaitingForAck){
        //             continue;
        //         }

        //         currentTransactionOperation = getNextTransactionOperation();
        //         // TODO(spradeep): 
        //         // sendOperation(currentTransactionOperation);

        //         // TODO(spradeep): Should this be here?
        //         util.clock.update ();
        //     }
        // } catch (Exception e) {
	//     System.out.println ("Error while trying for mutual exclusion:"
	// 			+ e.toString ());
        //     e.printStackTrace();
        // } finally {
        //     receiverExecutor.shutdown();
        //     // senderExecutor.shutdown();
        // }
    }

    /** 
     * Execute each transaction in transactionList.
     */
    public void executeTransactions(){
        for (Transaction transaction : transactionList){
            transaction.TS = util.clock.getTimeStamp();
            transaction.execute();
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
        util.printTimeStampedMessage("INIT" + " " + selfHostname + ":" + selfPort);
        requestHandler.sendMessage(initServerHost,
                                   initServerPort,
                                   util.getTimeStampedMessage(
                                       "INIT" + " " + selfHostname + ":" + selfPort));
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
        // util.printTimeStampedMessage (transactionOperationList.toString ());
        TransactionOperation nextOperation = transactionOperationList.get (operationIndex);

        if (!util.transactionTimeStampHash.containsKey(nextOperation.transactionId)){
            // New transaction
            util.transactionTimeStampHash.put(nextOperation.transactionId,
                                              util.clock.getTimeStamp());
            transactionStartIndex = operationIndex;
            setCurrTransactionId(nextOperation.transactionId);
        }
        nextOperation.transactionTimeStamp = util.transactionTimeStampHash.get(
            nextOperation.transactionId);

        util.printTimeStampedMessage("operationIndex: " + operationIndex);
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

        //     // util.printTimeStampedMessage("End of previous transaction."); 
        // }

    }

    public void initNewTransaction(){
        numOutstandingAcks = 0;
    }
}
