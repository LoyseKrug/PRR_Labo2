import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Lamport implements ILamport {



    /* Variables */

    // The local clock
    //TODO check si on boucle ce qui se passe
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
    //TODO const nbOfLamports ?
    //TODO register registry in construtor ?
    public Lamport(int nbOfLamports, int thisLamportId){

        messages = new StoredMsg[nbOfLamports];

        for(int i = 0; i < messages.length; ++i){
            messages[i] = new StoredMsg();
        }

        id = thisLamportId;
    }

    public void Init() throws RemoteException{
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

    public void IncrementVar() throws RemoteException {

        //TODO vérifier qu'on en a pas déja une dans le buffet, sinon bloquer
        System.out.println("L"+ id + " ++ attempt");
        try {

        synchronized (this) {
            //increment local clock
            //TODO section critique ?
            ++localClock;   // increment for the action of setting our REQ

            // set our lamport to the correct state;
            //TODO section critique ?
            messages[id].time = localClock;
            messages[id].type = TYPE.REQ;
        }
            // foreach other lamport in the system
            for (int i = 0; i < messages.length; ++i) {
                if (i != id) {
                    // send a Request
                    messages[i].lamport.Request(id, messages[id].time);

                    synchronized (this){
                        //TODO comment
                        while(  messages[i].time < messages[id].time
                                ||
                                (
                                    messages[i].type == TYPE.REQ &&
                                    (
                                        (
                                            messages[i].time == messages[id].time
                                            &&
                                            i < id
                                        )
                                    )
                                )
                             ){
                            System.out.println("L" + id + " waiting for L" + i + " msg type " + messages[id].type + "|" + messages[i].type + " time "  + messages[id].time + "|" + messages[i].time );
                            this.wait();
                        }
                    }
                }
            }
            /*
            // foreach Lamport instance
            for(int i = 0; i < messages.length; ++i){

                // if it isen't us
                if(i != id){
                    // we sync-lock only the current message to ba able to wake him up in time needed
                    synchronized (messages[i]) {
                        //TODO ref vérou ? how does it work
                        // we need to wait in different cases :
                        // 1) if the message we are checking is older than our REQ time
                        // 2) if the message we are checking is a REQ who's the time is exactly equal to ours REQ's time
                        //    and it's emmiter has a smaller id than us
                        while(messages[i].time < messages[id].time ||
                                (messages[i].type == TYPE.REQ && messages[i].time == messages[id].time && (i < id))){

                            System.out.println("L" + id + " is waiting on L" + i + "'s ACK/Free");

                            // we wait for an update to recheck
                            messages[i].wait();
                        }
                    }   // end sync
                }
            }
            */
        int time = 0;
        synchronized (this) {
            // we have access to the variable
            //TODO section critique ?
            ++var;

            //TODO section critique ?
            ++localClock;   // incrementation for the action of modifying var

            // we need to free access to the variable
            // we set our "message" to FREE with new time

            //TODO section critique ?
            messages[id].time = localClock;
            messages[id].type = TYPE.FREE;
            //notify(id); //TODO est-ce qu'on passe plutot par le Free() - non sinon on incrémente 2x la clock
            time = localClock;
        }

            // foreach other lamport in the system
            //TODO section critique ?
            for(int i = 0; i < messages.length; ++i){
                if(i != id) {
                    // send a Free with the new value
                    messages[i].lamport.Free(id, time, var);
                }
            }

            // in case of timeout we throw a remoteException
            //TODO what cases exactly (comment needed)
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RemoteException("Timeout");
        }



    }

    // when we recieve a request.
    public void Request(int p, int t) throws RemoteException {

        int time = 0;
        //TODO section critique ?
        synchronized (this) {
            System.out.println("L" + id + " request from " + p + " at " + t + " local time " + (localClock + 1));
            messages[p].type = TYPE.REQ;
            messages[p].time = t;

            syncClock(t);
            time = localClock;
            //notify(p);
        }

        messages[p].lamport.Acknowledge(id, time);
    }

    // when we recieve an Acknowledge
    public void Acknowledge(int p, int t) {

        synchronized (this) {
            System.out.println("L" + id + " ack from " + p + " at " + t + " local time " + (localClock + 1));
            // if it was not a REQ
            if (messages[p].type != TYPE.REQ) {

                //TODO section critique ?
                messages[p].type = TYPE.ACK;
                messages[p].time = t;

                syncClock(t);
                //notify(p);
            }
            // if last message from p is a REQ we ignore
            else {
                // we don't need to notify if nothing has changes
                syncClock(t);
            }
        }
    }

    // when we recieve a Free
    public void Free(int p, int t, int val) {

        synchronized (this) {
            System.out.println("L" + id + "free from " + p + " at " + t + " local time " + (localClock + 1));
            //TODO section critique ?
            var = val;

            //TODO section critique ?
            messages[p].type = TYPE.FREE;
            messages[p].time = t;

            syncClock(t);
            //notify(p);
            this.notify();
        }
    }

    // symple clock sync to use when recieving an event
    private void syncClock(int t){
        synchronized (this) {
            // in every case, we update the clock if needed.
            if(localClock <= t) {
                localClock = t + 1;
            } else {
                ++localClock;
            }
        }
    }

    // simple system to make requests wake up when they are stopped.
    private void notify(int p){
        synchronized (this){
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
