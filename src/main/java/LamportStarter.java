/**
 * class :      LamportStarter.java
 *
 * Authors:     Adrien Allemand & Loyse Krug
 *
 * Description:
 *              This is a helper class that you can simply run, then give it the number of lamport servers
 *              you want to start when prompted and wait, it will do all the startup and int job for you.
 *              The Lamport servers id will range from 0 to (n-1) , n beeing the number you gave him.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.RemoteException;

public class LamportStarter {

    /**
     * simply run, then give it the number of lamport servers
     * you want to start when prompted and wait, it will do all the startup and int job for you.
     * @param args -
     */
    public static void main(String[] args){

        // to be able to read the user input
        InputStreamReader isr = new InputStreamReader(System.in);
        BufferedReader br = new BufferedReader(isr);
        String line = "";

        try {

            // we get the number of servers we want
            System.out.println("Please enter a number of Lamport instances you desire.");
            line = br.readLine();
            int nb = Integer.parseInt(line);

            // initializes an empty array
            ILamport[] lamports = new ILamport[nb];

            // populates the array with Lamports servers
            for(int i = 0; i < nb; ++i) {// init Lamport

                System.out.println("Lamport server " + i + " is starting ...");
                lamports[i] = new Lamport(nb, i);
                System.out.println("Lamport server " + i + " is binded!");
            }

            // once they are all binded, we Init them all
            for(int i = 0; i < nb; ++i){
                lamports[i].Init();
                System.out.println("Lamport server " + i + " is ready!");
            }

            // we are done you can type exit if you want to stop everyting and quit
            do {
                System.out.println("Type 'exit' to stop all lamports and quit");
            }
            while ((line = br.readLine()) != null && !line.equals("exit") );

            // close the input stream reader
            isr.close();
        }
        // if we have a problem with the RMI system or the Lamport servers
        catch (RemoteException e) {
            e.printStackTrace();
        }
        // if we have a user input problem.
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
