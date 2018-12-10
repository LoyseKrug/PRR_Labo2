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
    private int localClock = 0;

    // this Lamport ID
    private int id;

    // the local state of Lamport Algorithm
    private StoredMsg[] messages;

    // la variable partagée et protégée en écriture
    private int var = 0;

    /* Methodes */

    // constructor, takes the number of Lamport servers.
    //  caution, needs to be Init() before used
    public Lamport(int nbOfLamports, int thisLamportId) {

        messages = new StoredMsg[nbOfLamports];

        for (int i = 0; i < messages.length; ++i) {
            messages[i] = new StoredMsg();
        }

        id = thisLamportId;

        try {
            // bind to registery
            ILamport lamport = (ILamport) UnicastRemoteObject.exportObject(this, 0);

            // each pair lamport-client has his own port number defined by there id
            Registry registry = LocateRegistry.createRegistry(1992 + id);

            registry.bind("Lamport", lamport);

        }
        // remote exception during exportation
        catch (RemoteException e) {
            e.printStackTrace();
        }
        // coulden't bind Registry
        catch (AlreadyBoundException e) {
            e.printStackTrace();
        }
    }

    public void Init() throws RemoteException {
        for(int i = 0; i < messages.length; ++i){
            if(i != id) {
                //TODO change if we are not local
                Registry registry = LocateRegistry.getRegistry(1992 + i);

                try {
                    // gets "remote" reference to the other Lamport (for RMI usage)
                    messages[i].lamport =  (ILamport) registry.lookup("Lamport");
                } catch (NotBoundException e) {
                    e.printStackTrace();
                }
            }
            // if id is ours
            else {
                messages[i].lamport = this;
            }
        }
    }

    // returns var value, not protected
    public int ReadVar() {
        System.out.println("L"+ id + " read : " + var);
        return var;
    }

    // modifies the value
    public void WriteVar(int newValue) {
        System.out.println("L"+ id + " ++ attempt");
        try {
            synchronized (this) {
                //increment local clock
                ++localClock;   // increment for the action of setting our REQ

                // set our lamport to the correct state;
                messages[id].time = localClock;
                messages[id].type = TYPE.REQ;
            }

            // sends the REQ and processes the ACK/FREE
            GetAccessPermission();

            // local temp variable to store the time of free message
            int tempTime;

            synchronized (this) {
                // we have access to the variable
                var = newValue;
                // incrementation for the action of modifying var
                ++localClock;

                // we need to free access to the variable now that we modified it
                // we set our "message" to FREE with new time
                messages[id].time = localClock;
                messages[id].type = TYPE.FREE;

                // we store the free time as we are leaving the synchronized section and need
                // to be coherent when we send the Free message to other Lamports
                // we use tempTime to store it locally
                tempTime = localClock;
            }

            // frees all other Lamports
            SendFreeMessages(tempTime);
        }
        // we catch an eventual exception from the wait()
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        // or an eventual RemoteException from the Free
        catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void IncrementVar() throws RemoteException {

        System.out.println("L"+ id + " ++ attempt");
        try {

            synchronized (this) {
                //increment local clock
                ++localClock;   // increment for the action of setting our REQ

                // set our lamport to the correct state;
                messages[id].time = localClock;
                messages[id].type = TYPE.REQ;
            }

            // sends the REQ and processes the ACK/FREE
            GetAccessPermission();

            // local temp variable to store the time of free message
            int tempTime;

            synchronized (this) {
                // we have access to the variable
                ++var;
                // incrementation for the action of modifying var
                ++localClock;

                // we need to free access to the variable now that we modified it
                // we set our "message" to FREE with new time
                messages[id].time = localClock;
                messages[id].type = TYPE.FREE;

                // we store the free time as we are leaving the synchronized section and need
                // to be coherent when we send the Free message to other Lamports
                // we use tempTime to store it locally
                tempTime = localClock;
            }

            // frees all other Lamports
            SendFreeMessages(tempTime);
        }
        // we catch an eventual exception from the wait()
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }



    // when we recieve a REQ message.
    public void Request(int p, int t) throws RemoteException {


        // local temp variable to store the time of REQ message reception
        int tempTime;

        synchronized (this) {
            System.out.println("L" + id + " recieved request from " + p + " at " + t + " local time " + (localClock + 1));

            // we store the new state of Lamport p and the time it has been sent
            messages[p].type = TYPE.REQ;
            messages[p].time = t;

            // we sync our clock to process the new event
            syncClock(t);

            // we store the REQ reception time to ba able to
            tempTime = localClock;

            // we notify ourself in case we are waiting on a REQ to access the protected var
            this.notify();
        }

        // finally, we send our acknowlagement
        messages[p].lamport.Acknowledge(id, tempTime);
    }

    // when we recieve an Acknowledge
    public void Acknowledge(int p, int t) {

        synchronized (this) {
            System.out.println("L" + id + " ack from " + p + " at " + t + " local time " + (localClock + 1));
            // if Lamport p is not currently waiting on a REQ, we will store the ACK
            if (messages[p].type != TYPE.REQ) {

                // we store the new state of Lamport p and the time it has been sent
                messages[p].type = TYPE.ACK;
                messages[p].time = t;

            }
            // else Lamport p was waiting on a REQ so we discare the ACK, we have all the informations we need

            // we sync our clock to process the new event in any case
            syncClock(t);

            // we notify ourself in case we are waiting on a ACK to access the protected var
            this.notify();
        }
    }

    // when we recieve a Free
    public void Free(int p, int t, int val) {

        synchronized (this) {

            // when we recieve a free message, we update our local var value
            System.out.println("L" + id + "free from " + p + " at " + t + " local time " + (localClock + 1));
            var = val;

            // we store the new state of Lamport p and the time it has been sent
            messages[p].type = TYPE.FREE;
            messages[p].time = t;

            // we sync our clock
            syncClock(t);

            // we notify ourself in case we are waiting on a Free to access the protected var
            this.notify();
        }
    }

    // simple clock sync to use when recieving an event
    private void syncClock(int t){
        synchronized (this) {
            // if our clock is late, we set it to t + 1
            if(localClock <= t) {
                localClock = t + 1;
            }
            // if we are in advance, we simply increment it
            else {
                ++localClock;
            }
        }
    }

    // sends the REQ and processes the ACK/FREE
    private void GetAccessPermission() throws InterruptedException, RemoteException {
        // foreach other lamport in the system we need an ACK
        for (int i = 0; i < messages.length; ++i) {
            if (i != id) {
                // send a Request
                messages[i].lamport.Request(id, messages[id].time);

                synchronized (this){

                    // this will make the current REQ wait until it is allowed to execute
                    while(
                        messages[i].time < messages[id].time                            // if the message[i] is older than our request
                            ||                                                          // or
                            (                                                           // (
                            messages[i].type == TYPE.REQ                                // if the message[i] is a REQ
                                &&                                                      // and
                                (                                                       //  (
                                    messages[i].time == messages[id].time               //   it has the same time than our own request
                                    &&                                                  //   and
                                    i < id                                              //   his id has priority over ours
                                )                                                       //  )
                            )                                                           // )
                    ){                                                                  // then wait for a notify() from Free()
                        System.out.println("L" + id + " waiting for L" + i + " msg type " + messages[id].type + "|" + messages[i].type + " time "  + messages[id].time + "|" + messages[i].time );
                        this.wait();
                    }
                }
            }
        }// end of the REQ, we are good to modify the variable, other Lamports are blocked
    }

    private void SendFreeMessages(int tempTime) throws RemoteException {

        // foreach other lamport in the system
        for(int i = 0; i < messages.length; ++i){
            if(i != id) {
                // we send a Free message with the new value
                messages[i].lamport.Free(id, tempTime, var);
            }
        }
    }

    // pass 2 ints as arguments :
    // num: int, the number of Lamports in the system
    // id:  int, the id of this Lamport (first Lamport id is 0 last is num-1)
    public static void main(String[] args){
        // The number of Lamports servers in the system.
        int num = 0;
        // The id of this Lamport server.
        int id = 0;

        try {
            // Parse the string arguments to integer values.
            num = Integer.parseInt(args[0]);
            id = Integer.parseInt(args[1]);
        }
        catch (NumberFormatException nfe) {
            // In case of error
            System.out.println("The first 2 arguments must be integers.");
            System.exit(1);
        }

        // to read the console inputs for configuration
        InputStreamReader isr = new InputStreamReader(System.in);
        BufferedReader br = new BufferedReader(isr);
        String line = "";

        // we try regestry
        try {

            System.out.println("Lamport server " + id + " is starting ...");

            Lamport lamport = new Lamport(num, id);

            System.out.println("Lamport server " + id + " is binded! - Ready to INIT");

            System.out.println("Type \"init\" to init this lamport server. You should to this only after ALL Lamports servers have been binded and show the \"Ready to INIT\" message.");

            while ((line = br.readLine()) != null && !line.equals("init") ){
                System.out.println("Type \"init\" to init this lamport server. You should to this only after ALL Lamports servers have been binded and show the \"Ready to INIT\" message.");
            }

            lamport.Init();

            System.out.println("Lamport server " + id + " is ready!");

            System.out.println("Type \"exit\" to quit");

            while ((line = br.readLine()) != null && !line.equals("exit") ){
                System.out.println("Type 'exit' to quit");
            }

            isr.close();
        }
        // in case we have a problem with the registry in the Init() function
        catch (RemoteException e) {
            e.printStackTrace();
        }
        // in case we have a problem reading the user configs values
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
