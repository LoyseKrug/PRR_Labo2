import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ILamport extends Remote{
    /* Inner Definitions */

    // enum to descripe the individual states of the other Lamport servers
    enum TYPE  { FREE, ACK, REQ }

    // simple class to regroup the state and the time into one array
    class MsgType {
        public TYPE type = TYPE.FREE;
        public int time = 0;
        public ILamport lamport;
    }

    void Init() throws RemoteException;

    // client methodes
    int ReadVar() throws RemoteException;

    void IncrementVar() throws RemoteException;

    // Lamport access restriction methodes
    void Request(int p, int t) throws RemoteException;

    void Acknowledge(int p, int t) throws RemoteException;

    void Free(int p, int t, int val) throws RemoteException;

}
