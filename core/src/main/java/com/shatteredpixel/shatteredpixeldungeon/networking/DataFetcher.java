package com.shatteredpixel.shatteredpixeldungeon.networking;

import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.networking.NetworkManager;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Map;

public class DataFetcher {

    private static final NetworkManager net = NetworkManager.INSTANCE;




    public void depositData(Map<String, Integer> data, String header) {
        try {

            StringBuilder payload = new StringBuilder();
            payload.append(header+":"); //header.

            for (Map.Entry<String, Integer> entry : data.entrySet()) {
                payload.append(entry.getKey())
                        .append("=")
                        .append(entry.getValue())
                        .append(";");
            }
            payload.deleteCharAt(payload.length()-1);


            net.send(payload.toString());

        } catch (Exception e) {
            System.err.println("Failed to send data: " + e.getMessage());
        }
    }
}
