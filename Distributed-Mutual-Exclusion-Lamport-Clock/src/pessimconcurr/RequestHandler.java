package pessimconcurr;

import java.io.*;
import java.lang.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
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

public class RequestHandler {
    static final int MAX_TOTAL_REQUESTS = 1;
    static final int SOCKET_READ_TIMEOUT = 200;
    public List<AckMutexMessage> ackList;
    public ServerSocket serverSocket;
    public ExecutorService receiverExecutor;
    public Util util;
    public Set<String> initNodes;
    public boolean receivedGoAhead;
    public boolean isWaitingForAck;

    public RequestHandler(List<AckMutexMessage> ackList,
                          ExecutorService receiverExecutor,
                          ServerSocket serverSocket,
                          Util util) {
        this.serverSocket = serverSocket;
        this.receiverExecutor = receiverExecutor;
        this.ackList = ackList;
        this.util = util;
        initNodes = new HashSet<String>();
        isWaitingForAck = false;
    }

    public RequestHandler(){
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
                    util.printTimeStampedMessage("Received message... " + message);
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
        util.printTimeStampedMessage("util.dataItemHash: " + util.dataItemHash);

        MutexMessage mutexMessage = new MutexMessage (message);
        util.clock.update ();

        if (mutexMessage.isOperationRequest()){
            TransactionOperation op = TransactionOperation.fromTimeStampedString(
                mutexMessage.getMessage());

            // In case this is a restarted transaction, delete preOps
            // that were sent with an earlier TS.
            for (DataItem d : util.dataItemHash.values()){
                d.handleRejectedTransaction(op);
            }

            boolean isSuccess = canExecuteOperation(op);

            DataItem d = util.dataItemHash.get(op.dataItemLabel);

            d.doPreOperation(op);
            
            TimeStamp messageTimeStamp = mutexMessage.getTimeStamp ();
            AckMutexMessage ack = new AckMutexMessage(op, !isSuccess, util.processId);
            sendMessage(op.transactionTimeStamp.getProcessId(),
                        util.getTimeStampedMessage(ack.toString()));
        } else if (AckMutexMessage.isAckMutexMessage(message)){
            AckMutexMessage ack = new AckMutexMessage(message);
            util.printTimeStampedMessage("Received Ack... " + ack.toString());
            ackList.add(ack);
            if (ack.op.equals(util.currentOp)){
                util.printTimeStampedMessage("Set isWaitingForAck to false");
                isWaitingForAck = false;
            }
            if ("Commit".equals(ack.val)){
                System.out.println("Commit... going to markTransactionForCommit");
                for (DataItem d : util.dataItemHash.values()){
                    d.markTransactionForCommit(ack.op.transactionTimeStamp);
                }
            }

        } else if (mutexMessage.isInitRequest()) {
            if (initNodes.size() == util.numNodes){
                return;
            }

            initNodes.add(mutexMessage.getMessage().split(" ")[1]);
            if (initNodes.size() == util.numNodes){
                try {
                    util.printTimeStampedMessage(
                        "I am the Bootstrap Server. Sending GO AHEAD..."); 
                    broadcastMessage(util.getTimeStampedMessage("GO_AHEAD_INIT"));
                    // Note: You need to send a message to yourself as well
                    sendMessage(util.selfHostname, util.selfPort,
                                util.getTimeStampedMessage("GO_AHEAD_INIT"));
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if (mutexMessage.isInitResponse()){
            util.receivedGoAhead = true;
            util.printTimeStampedMessage("Received Go Ahead..."); 
        }
    }
    
    /**
     * Send message to all peers (plus yourself).
     */
    void broadcastMessage (String message) throws UnknownHostException, IOException {
        for (int peerId = 0; peerId < util.numNodes; peerId++){
            // if (peerId == util.processId){
            //     continue;
            // }
            sendMessage (peerId, message);
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
            sendMessage (new Socket(util.allHostnames.get (peerId),
                                    util.allPorts.get (peerId)),
                         message);
        } catch (Exception e) {
            System.out.println("sendMessage error: " + e.toString());
        }
    }

    /** 
     * @return true iff execution of op on the appropriate data item
     * is possible.
     */
    public boolean canExecuteOperation(TransactionOperation op){
        DataItem d = util.dataItemHash.get(op.dataItemLabel);
        return d.canExecuteOperation(op);
    }
    
    public static void main(String[] argv){
        System.out.println(argv);
    }
}
