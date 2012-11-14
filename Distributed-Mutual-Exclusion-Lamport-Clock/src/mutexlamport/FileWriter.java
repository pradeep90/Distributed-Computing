package mutexlamport;

import java.net.*;
import java.io.*;
import java.lang.*;
import java.util.Random;

public class FileWriter {

    String filename;

    public static void main(String[] argv) {

        String nodeId;
        if (argv.length == 0){
            nodeId = "0";
        } else {
            nodeId = argv[0];
        }
        
        Random randomGenerator = new Random ();
        String message = nodeId + " - Yo, boyz!";
        for (int i = 0; i < 10; i++){
            int randomInt = randomGenerator.nextInt (1000);
            String randomMessage = message + " " + randomInt;
            System.out.println (randomMessage);
            FileWriter writer = new FileWriter ("output.txt");
            writer.appendToFile (randomMessage + "\n");
            try {
                Thread.sleep(500);
            } catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public FileWriter (String givenFilename){
        filename = givenFilename;
    }

    // TODO: Should a new java.io.FileWriter instance be
    // created each time or can we reuse one instance?
    public void appendToFile (String message){
        try{
            // Create file 
            java.io.FileWriter fstream = new java.io.FileWriter(filename, true);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(message);

            //Close the output stream
            out.close();
        }catch (Exception e){//Catch exception if any
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }
}
