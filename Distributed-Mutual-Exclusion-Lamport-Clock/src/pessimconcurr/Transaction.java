package pessimconcurr;

import java.io.*;
import java.lang.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

public class Transaction {
    public List<TransactionOperation> operationList;
    public boolean isTransactionRejected;
    public int transactionId;
    public TimeStamp TS;
    public int numOutstandingAcks;
    public RequestHandler requestHandler;
    public Util util;
    public boolean isWaitingForCommit = false;

    public Transaction(int transactionId,
                       TimeStamp TS,
                       List<TransactionOperation> operationList,
                       RequestHandler requestHandler,
                       Util util) {
        this.operationList = operationList;
        this.requestHandler = requestHandler;

        isTransactionRejected = false;
        numOutstandingAcks = 0;
        this.transactionId = transactionId;
        this.TS = TS;
        this.util = util;
    }

    /** 
     * Execute the operations in operationList. Then, send the Commit
     * message and wait for the writes to be committed.
     */
    public void execute(){
        int i;
        for (i = 0; i < operationList.size(); i++){
            util.printTimeStampedMessage("util.dataItemHash: " + util.dataItemHash);
            TransactionOperation op = operationList.get(i);
            op.transactionTimeStamp = TS;

            util.currentOp = op;

            sendOperation(op);

            requestHandler.isWaitingForAck = true;

            while (requestHandler.isWaitingForAck){
                tryExecuteDataItemOps();
                requestHandler.handleRequests();
            }

            AckMutexMessage ack = requestHandler.ackList.remove(
                requestHandler.ackList.size() - 1);

            handleAck(ack);

            if (isTransactionRejected){
                i = -1;
                numOutstandingAcks = 0;
                util.printTimeStampedMessage("Restarting transaction...");
                TS = util.clock.getTimeStamp();
                isTransactionRejected = false;
            }

            // util.printTimeStampedMessage("numOutstandingAcks: " + numOutstandingAcks);
            // util.printTimeStampedMessage("i: " + i);
        }

        broadcastCommit(transactionId, TS);

        isWaitingForCommit = true;

        // TODO(spradeep): 
        while (numOutstandingAcks != 0){
            while (requestHandler.ackList.size() == 0){
                tryExecuteDataItemOps();
                requestHandler.handleRequests();
            }
            
            AckMutexMessage ack = requestHandler.ackList.remove(
                requestHandler.ackList.size() - 1);
            handleCommitAck(ack);
            util.printTimeStampedMessage("numOutstandingAcks: " + numOutstandingAcks);
        }

        isWaitingForCommit = false;
    }

    public void handleAck(AckMutexMessage ack){
        // if ("Commit".equals(ack.val)){
        //     System.out.println("here");
        //     for (DataItem d : util.dataItemHash.values()){
        //         d.markTransactionForCommit(ack.getTimeStamp());
        //     }
        // }
        if (ack.isTransactionRejected){
            isTransactionRejected = true;
        } else if (ack.isSuccessfullyCompleted()){
            util.printTimeStampedMessage("Successfully completed ack: " + ack);
            if (ack.op.operationType == Operation.OperationType.READ){
                numOutstandingAcks--;
                util.printTimeStampedMessage("Decremented numOutstandingAcks: " + numOutstandingAcks);
            }
        }
    }

    public void handleCommitAck(AckMutexMessage ack){
        if (!ack.op.transactionTimeStamp.equals(TS)){
            return;
        }
        System.out.println("inside handleCommitAck"); 

        if (ack.op.operationType == Operation.OperationType.WRITE
            && !ack.op.isPreWrite){

            util.printTimeStampedMessage("Decrementing numOutstandingAcks ack.op.isPreWrite: " + ack.op.isPreWrite);
            // For pre-writes, the actual completion Ack will
            // come after commit, so decrement
            // numOutstandingAcks only for non pre-write Acks
            numOutstandingAcks--;
        }

    }

    /** 
     * Execute operations from the buffers of your data items (if
     * possible).
     * 
     */
    public void tryExecuteDataItemOps(){
        for (DataItem d : util.dataItemHash.values()){
            d.tryExecuteOps();
        }

        int i;
        for (DataItem d : util.dataItemHash.values()){
            while (d.nextReadAckIndex < d.readList.size()) {
                TransactionOperation currOp = d.readList.get(d.nextReadAckIndex);
                d.nextReadAckIndex++;

                AckMutexMessage ack = new AckMutexMessage(
                    currOp, false, util.processId);

                System.out.println("tryExecuteOps - Sending Ack... "
                                   + util.getTimeStampedMessage(ack.toString()));

                requestHandler.sendMessage(currOp.transactionTimeStamp.getProcessId(),
                                           util.getTimeStampedMessage(ack.toString()));
            }

            while (d.nextWriteAckIndex < d.writeList.size()) {
                TransactionOperation currOp = d.writeList.get(d.nextWriteAckIndex);
                d.nextWriteAckIndex++;

                AckMutexMessage ack = new AckMutexMessage(
                    currOp, false, util.processId);
                System.out.println("tryExecuteOps - Sending Ack... "
                                   + util.getTimeStampedMessage(ack.toString()));
                requestHandler.sendMessage(currOp.transactionTimeStamp.getProcessId(),
                                           util.getTimeStampedMessage(ack.toString()));
            }
        }
    }

    /** 
     * Send op to the node having op's data item.
     * 
     * Set isWaitingForAck as true.
     */
    public void sendOperation(TransactionOperation op){
        int destinationId = util.dataItemLocationHash.get(op.dataItemLabel);

        util.printTimeStampedMessage("Sending operation... " + op.toString());
        requestHandler.sendMessage(destinationId, util.getTimeStampedMessage(op.toString()));

        requestHandler.isWaitingForAck = true;
        numOutstandingAcks++;
    }

    /** 
     * Broadcast COMMIT message.
     * 
     * Set isWaitingForAck as true.
     */
    public void broadcastCommit(int transactionId, TimeStamp TS){
        // Defining a READ op because only an Ack for a Read can have
        // a `val` field showing up in the toString() output.
        TransactionOperation tempOp = new TransactionOperation(transactionId + " R x")
                .setTimeStamp(TS);
        AckMutexMessage commitAck = new AckMutexMessage(tempOp, false, util.processId);
        commitAck.val = "Commit";

        try {
            String messageString = util.getTimeStampedMessage(commitAck.toString());
            System.out.println(messageString);
            requestHandler.broadcastMessage(messageString);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        requestHandler.isWaitingForAck = true;
    }

    public String toString(){
        String result = "";
        result += "operationList.toString():" + " ";
        result += operationList.toString() + " ";
        result += "isWaitingForAck:" + " ";
        result += requestHandler.isWaitingForAck + " ";
        result += "isTransactionRejected:" + " ";
        result += isTransactionRejected + " ";
        result += "transactionId:" + " ";
        result += transactionId + " ";
        result += "TS:" + " ";
        result += TS + " ";
        result += "numOutstandingAcks:" + " ";
        result += numOutstandingAcks + " ";
        return result;
    }

}
