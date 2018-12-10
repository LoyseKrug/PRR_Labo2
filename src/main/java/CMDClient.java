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

            System.out.println("Client is running. commands are \"u\" to update, \"<x>\" to increment x times and \"exit\" to exit");

            while ((line = br.readLine()) != null && !line.equals("exit") ){
                if(line.equals("u")){
                    System.out.println("Var value is :" + stub.ReadVar());
                } else {
                    int num = 0;
                    try {
                        num = Integer.parseInt(line);

                        System.out.println("Value will be incremented " + num + " times ...");
                        for(int i = 0; i < num; ++i){
                            stub.IncrementVar();
                            System.out.println("Value incremented " + (i + 1) + " times");
                        }

                        System.out.println("After " + num + " increments, Var value is :" + stub.ReadVar());

                    } catch (NumberFormatException nfe){
                        System.out.println("You must write 'u', 'exit' or a number of time to increment");
                    }
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
