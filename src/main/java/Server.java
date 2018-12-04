/**
 * Authors: Adrien Allemand, Loyse Krug
 *
 * Sources: https://docs.oracle.com/javase/7/docs/technotes/guides/rmi/hello/hello-world.html
 */

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Server implements Hello {

    public String sayHello() throws RemoteException {
        return "Hello World!";
    }

    public static void main(String[] args){

        try{
            Server server = new Server();
            Hello stub = (Hello) UnicastRemoteObject.exportObject(server, 0);

            Registry registry = LocateRegistry.getRegistry();
            registry.bind("Hello", stub);

            System.out.println("Server is ready!");
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (AlreadyBoundException e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }

    }
}
