import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ILamport extends Remote{
    /* Inner Definitions */

    // enum to descripe the individual states of the other Lamport servers
    enum TYPE  { FREE, ACK, REQ }

    // simple class to regroup the state and the time into one array
    class StoredMsg {
        public TYPE type = TYPE.FREE;
        public int time = 0;
        public ILamport lamport;
    }

    // a appeler une fois que tout les Lamports ont été démarré pour qu'ils établissent la connection entre eux.
    void Init() throws RemoteException;

    // lit la valeur
    int ReadVar() throws RemoteException;

    // modification de la valeur
    void WriteVar(int i) throws RemoteException;

    // incrémente la valeur
    void IncrementVar() throws RemoteException;

    // Lamport access restriction methodes
    // Réception d'un request du ILamport p qui a été envoyé au temps t
    void Request(int p, int t) throws RemoteException;

    // Réception d'un ACK du ILamport p qui a été envoyé au temps t
    void Acknowledge(int p, int t) throws RemoteException;

    // Réception d'un Free du ILamport p qui a été envoyé au temps t, la nouvelle valeur est val
    void Free(int p, int t, int val) throws RemoteException;

}
