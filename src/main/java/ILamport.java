/**
 * class :      ILamport.java
 *
 * Authors:     Adrien Allemand & Loyse Krug
 *
 * Description:
 *              Interface ILamport extends Remote and must be implemented to create an RMI server
 *              It defines the different methods used by the Lamport algorithm
 *
 */


import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ILamport extends Remote{

    /**
     * enum to descripe the individual states of the other Lamport servers:
     * - REQ when a site ask for the access to the critic section
     * - ACK when a process answer positively to a REQ message
     * - FREE when a site has had access to the critic section and wants informe the other sites it is free again
     */
    enum TYPE  { FREE, ACK, REQ }

    /**
     * simple class to regroup the state and the time into one array
     */
    class StoredMsg {
        public TYPE type = TYPE.FREE;
        public int time = 0;
        public ILamport lamport;
    }

    /**
     * The function is called once all the Lamports server have been started. It is used to bind them together
     * @throws RemoteException, in case there is a problem with the RMI connection
     */
    void Init() throws RemoteException;

    /**
     * Read the value of the local value of the variable shared between all the sites
     * @return an int containing the value
     * @throws RemoteException, in case of an RMI exception
     */
    int ReadVar() throws RemoteException;

    /**
     * Increments locally the value of the shared variable and informe all the other machines that the value
     * has changed. Therefore calls request for acknowlegment from all the other sites, treat the request,
     * then sends free messages to all
     * @throws RemoteException
     */
    void IncrementVar() throws RemoteException;

    /**
     * Lamport access restriction method - Treat reception of a REQ message from the ILamport of site p,
     * sent at time t
     * @param p, id of the site sending the request
     * @param t, time at the creation of the request in site p
     * @throws RemoteException in case of an RMI exception
     */
    void Request(int p, int t) throws RemoteException;

    /**
     * Lamport access restriction method - Treat reception of an ACK message from the ILamport of site p,
     * sent at time t
     * @param p id of the site sending the request
     * @param t time at the creation of the request in site p
     * @throws RemoteException in case of an RMI exception
     */
    void Acknowledge(int p, int t) throws RemoteException;

    /**
     * Lamport access restriction method - Treat reception of a FREE message from the ILamport of site p,
     * sent at time t, with the new value of the shared variable
     * @param p id of the site sending the request
     * @param t time at the creation of the request in site p
     * @param val new value of the shared variable
     * @throws RemoteException in case of an RMI exception
     */
    void Free(int p, int t, int val) throws RemoteException;

}
