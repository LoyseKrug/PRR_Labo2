/**
 * class :      CLI.java
 *
 * Authors:     Adrien Allemand & Loyse Krug
 *
 * Description:
 *              A Simple CLI. It allows access to read, set, increment commands onto the protected variable.
 *              You must first start AND init the Lamport servers. Once they are READY, You can start one client per
 *              Lamport server. To connect a CLI to a Lamport server via RMI, run the CLI main(), enter
 *              the Lamport ID you want it to connect to when asked for it.
 *              Available commands are "read" to read, "set <int>" to set var to specific value, "inc <x>" to increment var x times and "exit" to exit
 *
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class CLI {

    /**
     * A Simple CLI. It allows access to read, set, increment commands onto the protected variable.
     * @param args -
     */
    public static void main(String[] args){

        try{
            //local vars
            int clientID;   // this will be the ID of the Lamport server this client will be binded to thus this client's ID
            String line;    // to read user input

            // to be able to read user input
            InputStreamReader isr = new InputStreamReader(System.in);
            BufferedReader br = new BufferedReader(isr);

            // read user input to bind to the correct Lamport Server
            System.out.println("Please enter the \"Lamport Server ID\" you want to to map this CLI to");
            line = br.readLine();
            clientID = Integer.parseInt(line);

            // Once we have user input, we start the RMI binding procedure
            System.out.println("Client is starting ...");
            Registry registry = LocateRegistry.getRegistry(Protocol.PORT + clientID);
            ILamport lamportServer = (ILamport) registry.lookup("Lamport");
            System.out.println("Client is running. commands are \"read\" to read, \"set <int>\" to set to specific value, \"inc <x>\" to increment x times and \"exit\" to exit");

            // CLI has correctly been binded, now entering the main CMD reading loop
            while ((line = br.readLine()) != null && !line.equals("exit") ){

                // local var
                int num;

                // we split the user command in case it has a parameter
                String[] commands = line.split(" ");

                // now we process the user input depending on the command he entered
                try {
                    // Read
                    if(commands[0].equals("read")){
                        System.out.println("Var value is :" + lamportServer.ReadVar());
                    }
                    //Write
                    else if(commands[0].equals("set")) {

                        num = Integer.parseInt(commands[1]);
                        System.out.println("Value will be set to " + num);
                        lamportServer.WriteVar(num);
                    }
                    // Incrementation
                    else if(commands[0].equals("inc")) {

                        num = Integer.parseInt(commands[1]);
                        System.out.println("Value will be incremented " + num + " times ...");

                        // this loop will to <num> incrementation but not at once,
                        // to simulate charge and highlight concurency problems, it makes one request per
                        // incrementation to the Lamport Server
                        for (int i = 0; i < num; ++i) {
                            lamportServer.IncrementVar();
                            System.out.println("Value incremented " + (i + 1) + " times");
                        }

                        System.out.println("Incrementation done.");
                    }
                    // unknown command
                    else{
                        System.out.println("Unknown Command - Commands are \"read\" to read, \"set <int>\" to set to specific value, \"inc <int>\" to increment int times and \"exit\" to exit");
                    }
                }
                // in case a command's parameter wasen't a valide number
                catch (NumberFormatException nfe){
                    System.out.println("Command " + commands[0] + " requires a valide <int> as parameter.");
                }
                System.out.println("Process finished, please enter the next command : ");
            }
            System.out.println("Bye !");
            // close input stream reader
            isr.close();
        }
        // If anything fails when contacting the Lamport server
        catch (RemoteException e) {
            e.printStackTrace();
        }
        // if we have a problem with user input
        catch (IOException e) {
            e.printStackTrace();
        }
        // if the lookup of the registry fails
        catch (NotBoundException e) {
            e.printStackTrace();
        }
    }
}
