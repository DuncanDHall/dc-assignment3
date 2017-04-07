import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

/**
 * Created by duncan on 4/5/17.
 */

public class MapTask implements iMapper {

    String name;

    public MapTask(String name, boolean isManager) {

        this.name = name;

        if (isManager) {
            // export manager objects and bind to local rmi for master access
            try {
                Registry registry = LocateRegistry.getRegistry();
                iMapper mapManagerStub = (iMapper) UnicastRemoteObject.exportObject(this);
                registry.rebind("mapManager", mapManagerStub);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public iMapper createMapTask(String name) throws RemoteException, AlreadyBoundException {
        return (iMapper) UnicastRemoteObject.exportObject(new MapTask(name, false), 0);
    }

    @Override
    public void processInput(String input, iMaster theMaster) throws RemoteException, AlreadyBoundException {
        String [] words = input.split("\\s+");
        HashMap<String, Integer> miniHist = new HashMap<>();

        for (String word : words) {
            word = word.replace("[^a-zA-Z]", "").toLowerCase();
            miniHist.put(word, miniHist.getOrDefault(word, 0) + 1);
        }

        String[] distinctWords = (String[]) miniHist.keySet().toArray();

        iReducer[] reducers = theMaster.getReducers(distinctWords);

        for (int i = 0; i < distinctWords.length; i++) {
            final int finalI = i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        reducers[finalI].receiveValues(miniHist.get(distinctWords[finalI]));
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

    }
}
