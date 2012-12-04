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

public class Util {

    public HashMap<Integer, TimeStamp> transactionTimeStampHash;
    public HashMap<String, Integer> dataItemLocationHash;
    public HashMap<String, DataItem> dataItemHash;
    public HashMap<Integer, Integer> startIndexHash;
    public LogicalClock clock;
    public int processId;
    public String selfHostname;
    public int selfPort;
    public int numNodes;
    public List<String> allHostnames;
    public List<Integer> allPorts;
    public boolean receivedGoAhead;
    public TransactionOperation currentOp = null;

    public Util(int processId,
                String selfHostname,
                int selfPort,
                int numNodes,
                List<String> allHostnames,
                List<Integer> allPorts,
                HashMap<String, Integer> dataItemLocationHash) {

        this.dataItemLocationHash = dataItemLocationHash;
        this.startIndexHash = startIndexHash;
        this.processId = processId;
        this.selfHostname = selfHostname;
        this.selfPort = selfPort;
        this.numNodes = numNodes;
        this.allHostnames = allHostnames;
        this.allPorts = allPorts;

        clock = new LogicalClock (this.processId);
        transactionTimeStampHash = new HashMap<Integer, TimeStamp>();
        dataItemHash = new HashMap<String, DataItem>();
        startIndexHash = new HashMap<Integer, Integer>();
    }

    /** 
     * Build hash table of (label, Data Item for that label).
     * 
     */
    public void buildDataItemHash(){
        for (String label : dataItemLocationHash.keySet()){
            if (dataItemLocationHash.get(label) == processId){
                dataItemHash.put(label, new DataItem(label));
            }
        }
    }

    
    public void printTimeStampedMessage(String message){
        System.out.println(getTimeStampedMessage(message));
    }

    public String getTimeStampedMessage (String message){
        return getTimeStampedMessage(clock.getTimeStamp(), message);
    }

    public String getTimeStampedMessage (TimeStamp timeStamp, String message){
        return new MutexMessage(timeStamp,
                                "[ " + message + " ]")
                .toString();
    }
}
