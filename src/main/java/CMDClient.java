import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class CMDClient {

    public static void main(String[] args){

        try{
            int clientID = 0;

            InputStreamReader isr = new InputStreamReader(System.in);
            BufferedReader br = new BufferedReader(isr);

            String line = "";
            System.out.println("Please enter the client ID to map it to the correct Registry");
            line = br.readLine();
            clientID = Integer.parseInt(line);

            System.out.println("Client is starting ...");

            Registry registry = LocateRegistry.getRegistry(1992 + clientID);
            ILamport stub = (ILamport) registry.lookup("Lamport");

            System.out.println("Client is running. commands are \"read\" to read, \"set <int>\" to set to specific value, \"inc <x>\" to increment x times and \"exit\" to exit");

            while ((line = br.readLine()) != null && !line.equals("exit") ){

                // we split the user command
                String[] commands = line.split(" ");
                int num = 0;

                try {
                    // Read
                    if(commands[0].equals("read")){
                        System.out.println("Var value is :" + stub.ReadVar());
                    }
                    //Write
                    else if(commands[0].equals("set")) {
                        num = Integer.parseInt(commands[1]);

                        System.out.println("Value will be set to " + num);

                        stub.WriteVar(num);
                    }
                    // Incrementation
                    else if(commands[0].equals("inc")) {

                        num = Integer.parseInt(commands[1]);

                        System.out.println("Value will be incremented " + num + " times ...");
                        for (int i = 0; i < num; ++i) {
                            stub.IncrementVar();
                            System.out.println("Value incremented " + (i + 1) + " times");
                        }
                    }
                    // wrong command
                    else{
                        System.out.println("ERROR - Commands are \"read\" to read, \"set <int>\" to set to specific value, \"inc <x>\" to increment x times and \"exit\" to exit");
                    }
                } catch (NumberFormatException nfe){
                    nfe.printStackTrace();
                }
                System.out.println("Process finished, please enter the next command : ");
            }

            isr.close();

        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
