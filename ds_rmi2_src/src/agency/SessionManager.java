package agency;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class SessionManager implements ISessionManager {
	
	SessionAgency cra;
	List<GenericSession> sessions = new ArrayList<>();
	
	public SessionManager(SessionAgency cra)
	{
		this.cra = cra;
	}
	
	public ReservationSession createReservationSession(String name) throws RemoteException {
		
		try {
			ConcreteReservationSession newSession = new ConcreteReservationSession(cra, name);
			ReservationSession stub = (ReservationSession) UnicastRemoteObject.exportObject(newSession,0);
			this.sessions.add(newSession);
			return stub;
		} catch (RemoteException e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	protected void removeSession(ConcreteGenericSession session) throws IllegalArgumentException
	{
		if(sessions.remove(session) == false) throw new IllegalArgumentException("Session not present in SessionManager");
	}
	
	public ManagerSession createManagerSession(String name) throws RemoteException {
		try {
			ConcreteManagerSession newSession = new ConcreteManagerSession(cra, name);
			ManagerSession stub = (ManagerSession) UnicastRemoteObject.exportObject(newSession,0);
			this.sessions.add(newSession);
			return stub;
		} catch (RemoteException e) {
			e.printStackTrace();
			throw e;
		}
	}

	
}
