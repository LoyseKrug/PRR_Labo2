import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Lamport implements ILamport {



    /* Variables */

    // The local clock
    private int localClock;

    // this lamport ID
    private int id;

    // the local state of Lamport Algorithm
    private MsgType[] messages;

    private int var = 0;

    /* Methodes */

    // constructor, takes the number of Lamport servers.
    public Lamport(int nbOfLamports, int thisLamportId){
        localClock = 0;

        messages = new MsgType[nbOfLamports];
        for(int i = 0; i < messages.length; ++i){
            messages[i] = new MsgType();
        }

        id = thisLamportId;
    }

    public void Init() throws RemoteException{
        for(int i = 0; i < messages.length; ++i){
            if(i != id) {
                Registry registry = LocateRegistry.getRegistry(1992 + i);
                try {
                    messages[i].lamport =  (ILamport) registry.lookup("Lamport");
                } catch (NotBoundException e) {
                    e.printStackTrace();
                }
            } else {
                messages[i].lamport = this;
            }
        }
    }

    public int ReadVar() {
        System.out.println("Lecture");
        return var;
    }

    public void IncrementVar() throws RemoteException {

        System.out.println("Increment attempt :");
        try {

            //increment local clock
            ++localClock;

            // set our lamport to the correct state;
            messages[id].time = localClock;
            messages[id].type = TYPE.REQ;

            // foreach other lamport in the system
            for(int i = 0; i < messages.length; ++i){
                if(i != id) {

                    // send a Request
                    messages[i].lamport.Request(id, localClock);
                }
            }

            // foreach Lamport instance
            for(int i = 0; i < messages.length; ++i){

                // if it isen't us
                if(i != id){
                    // we sync-lock only the current message to ba able to wake him up in time needed
                    synchronized (messages[i]) {
                        // we need to wait in different cases :
                        // if the message we are checking is older than our REQ time
                        // or if is is a REQ who's the time is exactly equal to ours and it has a smaller id than us
                        while(messages[i].time < messages[id].time ||
                                (messages[i].type == TYPE.REQ && messages[i].time == messages[id].time && !(i < id))){
                            System.out.println("Lamport " + id + " is waiting on " + i);
                            // we wait for an update to recheck
                            messages[i].wait();
                        }
                    }
                }
            }

            // we have access to the variable
            ++var;

            // we need to free access to the variable

            ++localClock;

            messages[id].time = localClock;
            messages[id].type = TYPE.FREE;

            // foreach other lamport in the system
            for(int i = 0; i < messages.length; ++i){
                if(i != id) {

                    // send a Free with the new value
                    messages[i].lamport.Free(id, localClock, var);
                }
            }

            // in case of timeout we throw a remoteException
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RemoteException("Timeout");
        }



    }

    // when we recieve a request.
    public synchronized void Request(int p, int t) throws RemoteException {
        System.out.println("Request from " + p + " at " + t + " local time " + (localClock + 1));

        messages[p].type = TYPE.REQ;
        messages[p].time = t;

        syncClock(t);
        notify(p);

        messages[p].lamport.Acknowledge(id, localClock);
    }

    // when we recieve an Acknowledge
    public synchronized void Acknowledge(int p, int t) {
        System.out.println("Ack from " + p + " at " + t + " local time " + (localClock + 1));

        if(messages[p].type != TYPE.REQ) {

            messages[p].type = TYPE.ACK;
            messages[p].time = t;

            syncClock(t);
            notify(p);
        } else {
            // we need to not notify if nothing has changes
            syncClock(t);
        }
    }

    // when we recieve a Free
    public synchronized void Free(int p, int t, int val) {
        System.out.println("Free from " + p + " at " + t + " local time " + (localClock + 1));
        var = val;

        messages[p].type = TYPE.FREE;
        messages[p].time = t;

        syncClock(t);
        notify(p);
    }

    // symple clock sync to use when recieving an event
    private void syncClock(int t){
        // in every case, we update the clock if needed.
        if(localClock <= t) {
            localClock = t + 1;
        } else {
            ++localClock;
        }
    }

    // simple system to make requests wake up when they are stopped.
    private void notify(int p){
        synchronized (messages[p]){
            messages[p].notify();
        }
    }

    public static void main(String[] args){

        /*
        // Read command line
        int num = 0;
        int id = 0;

        try {
            // Parse the string argument into an integer value.
            num = Integer.parseInt(args[0]);
            //id = Integer.parseInt(args[1]);
        }
        catch (NumberFormatException nfe) {
            // The first argument isn't a valid integer.
            System.out.println("The first argument must be an integer.");
            System.exit(1);
        }
        */

        // console version
        InputStreamReader isr = new InputStreamReader(System.in);
        BufferedReader br = new BufferedReader(isr);
        String line = "";

        try {
            System.out.println("Please enter a number of Lamport instances you desire.");
            line = br.readLine();

            int nb = Integer.parseInt(line);

            ILamport[] lamports = new ILamport[nb];

            for(int i = 0; i < nb; ++i) {// init Lamport
                System.out.println("Lamport server " + i + " is starting ...");

                Lamport lamport = new Lamport(nb, i);
                lamports[i] = (ILamport) UnicastRemoteObject.exportObject(lamport, 0);

                // each pair lamport-client has his own port number
                Registry registry = LocateRegistry.createRegistry(1992 + i);
                registry.bind("Lamport", lamports[i]);

                System.out.println("Lamport server " + i + " is binded!");
            }

            for(int i = 0; i < nb; ++i){
                lamports[i].Init();
                System.out.println("Lamport server " + i + " is ready!");
            }

            System.out.println("Type 'exit' to quit");

            while ((line = br.readLine()) != null && !line.equals("exit") ){
                System.out.println("Type 'exit' to quit");
            }

            isr.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
