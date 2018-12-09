import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {

    public static void main(String[] args){
        try{
            Registry registry = LocateRegistry.getRegistry(1992);
            Hello stub = (Hello) registry.lookup("Lamport");
            String response = stub.sayHello();
            System.out.println(response);
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
