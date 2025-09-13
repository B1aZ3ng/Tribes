package players;

import core.actions.Action;
import core.Types;
import core.game.GameState;
import utils.ElapsedCpuTimer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

/**
 * SocketAgent connects to a Python server via TCP,
 * sends available actions as JSON,
 * and waits for Python to send back the chosen action string.
 */
public class SocketAgent extends Agent {
    private String host;
    private int port;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    public SocketAgent(long seed, String host, int port) {
        super(seed);
        this.host = host;
        this.port = port;
        connectToPython();
    }

    private void connectToPython() {
    while (true) {
        System.out.println("[SocketAgent] Connecting to Python at " + host + ":" + port + "...");
        try {
            socket = new Socket(host, port);
            System.out.println("[SocketAgent] Connected to Python at " + host + ":" + port);
            out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            break; // success
        } catch (IOException e) {
            System.out.println("[SocketAgent] Python not ready. Retrying in 1s...");
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
        }
    }
}

    @Override
    public Action act(GameState gs, ElapsedCpuTimer ect) {
        try {
            // Build JSON with available actions
            JSONObject payload = new JSONObject();
            payload.put("player_id", gs.getActiveTribeID());
            //payload.put("map",gs.get)
            payload.put("is_game_over", gs.isGameOver());

            ArrayList<Action> allActions = gs.getAllAvailableActions(gs.getActiveTribeID());
            JSONArray actionList = new JSONArray();
            for (Action a : allActions) {
                actionList.put(a.toString());
            }
            payload.put("available_actions", actionList);

            // Send JSON to Python
            out.println(payload.toString());
            out.flush();

            // Wait for Python's response
            String line = in.readLine();
            if (line == null) return getEndTurn(gs);

            JSONObject response = new JSONObject(line);
            String chosenAction = response.getString("action");

            // Match chosen action with available actions
            for (Action a : allActions) {
                if (a.toString().equals(chosenAction)) {
                    return a;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Default: End turn
        return getEndTurn(gs);
    }

    private Action getEndTurn(GameState gs) {
        ArrayList<Action> actions = gs.getAllAvailableActions(gs.getActiveTribeID());
        for (Action a : actions) {
            if (a.getActionType() == Types.ACTION.END_TURN) return a;
        }
        return actions.get(0); // fallback
    }

    @Override
    public Agent copy() {
        return new SocketAgent(getSeed(), host, port);
    }
}
