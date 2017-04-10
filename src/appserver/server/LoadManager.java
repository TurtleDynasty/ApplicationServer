/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package appserver.server;

import java.util.ArrayList;

/**
 *
 * @author Dr.-Ing. Wolf-Dieter Otte
 */
public class LoadManager {

    static ArrayList satellites = null;
    static int lastSatelliteIndex = -1;

    public LoadManager() {
        satellites = new ArrayList<String>();
    }

    public void satelliteAdded(String satelliteName) {
        // add satellite
        // ...
    }

    /*
    public void satelliteRemoved(String satelliteName) {
        satellites.remove(satelliteName);
    }

     public void satelliteJobStarted(String satelliteName, String toolName) {
        
     }
    
     public void satelliteJobStopped(String satelliteName, String toolName) {
        
     }
     */
    public String nextSatellite() throws Exception {
        
        int numberSatellites;
        
        synchronized (satellites) {
            // implement policy that returns the satellite name according to a round robin methodology
            // ...
        }

        return (String) satellites.get(lastSatelliteIndex);
    }
}
