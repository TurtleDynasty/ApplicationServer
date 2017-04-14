package appserver.server;

import appserver.comm.Message;
import static appserver.comm.MessageTypes.JOB_REQUEST;
import static appserver.comm.MessageTypes.REGISTER_SATELLITE;
import appserver.comm.ConnectivityInfo;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;
import utils.PropertyHandler;

/**
 *
 * @author Dr.-Ing. Wolf-Dieter Otte
 */
public class Server {

    // Singleton objects - there is only one of them. For simplicity, this is not enforced though ...
    static SatelliteManager satelliteManager = new SatelliteManager();
    static LoadManager loadManager = new LoadManager();
    static ServerSocket serverSocket = null;
    String host = null;
    int port;
    Properties properties; // = new Properties();

    public Server(String serverPropertiesFile) {

        try {

            // create satellite and load managers
            properties = new PropertyHandler(serverPropertiesFile);
            host = properties.getProperty("HOST");
            System.out.println("Server host : " + host);

            // read server port from server properties file
            port = Integer.parseInt(properties.getProperty("PORT"));
            System.out.println("Server port : " + port);

            // create server socket
            serverSocket = new ServerSocket(port);


        } catch (Exception error) {
            System.err.println("Server error : " + error);
        }

    }

    public void run() {
    // start serving clients in server loop ...
        while(true){
            System.out.println("Server waiting for port");
            try {

                new Thread(new ServerThread(serverSocket.accept())).start();

            } catch (IOException error) {
                System.err.println("Server error : " + error);
            }
        }
    }

    // objects of this helper class communicate with clients
    private class ServerThread extends Thread {

        Socket client = null;
        ObjectInputStream readFromNet = null;
        ObjectOutputStream writeToNet = null;
        Message message = null;

        private ServerThread(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            // setting up object streams
             try {

                readFromNet = new ObjectInputStream(client.getInputStream());
                writeToNet = new ObjectOutputStream(client.getOutputStream());
                message = (Message) readFromNet.readObject();

            } catch (Exception e) {
                System.err.println("[ServerThread.run] Message could not be read from object stream.");
                e.printStackTrace();
                System.exit(1);
            }

            // reading message
            try {
                message = (Message) readFromNet.readObject();
            } catch (Exception e) {
                System.err.println("[ServerThread.run] Message could not be read from object stream.");
                e.printStackTrace();
                System.exit(1);
            }

            // processing message
            ConnectivityInfo satelliteInfo = null;
            switch (message.getType()) {
                case REGISTER_SATELLITE:
                    // read satellite info
                    satelliteInfo = (ConnectivityInfo) message.getContent();
                    System.out.println("[ServerThread] Satellite name: " + satelliteInfo.getName() );

                    // register satellite
                    synchronized (Server.satelliteManager) {
                        // add info from [this] satellite to Manager class
                        satelliteManager.registerSatellite(satelliteInfo);
                    }

                    // add satellite to loadManager
                    synchronized (Server.loadManager) {
                        loadManager.satelliteAdded(satelliteInfo.getName());
                    }

                    break;

                case JOB_REQUEST:
                    System.err.println("\n[ServerThread.run] Received job request");

                    String satelliteName = null;
                    synchronized (Server.loadManager) {
                        // get next satellite from load manager
                        try {

                            satelliteName = loadManager.nextSatellite();

                        } catch (Exception error) {
                            System.err.println("Server error: " + error);
                            System.exit(1);
                        }

                        // get connectivity info for next satellite from satellite manager
                        try {

                            satelliteInfo = satelliteManager.getSatelliteForName(satelliteName);

                        } catch (Exception error) {
                            System.err.println("Server error: " + error);
                            System.exit(1);
                        }

                    }

                    Socket satelliteSocket = null;
                    ObjectInputStream satelliteReadFromNet;
                    ObjectOutputStream satelliteWriteFromNet;

                    try {

                        // connect to satellite
                        satelliteSocket = new Socket(satelliteInfo.getHost(), satelliteInfo.getPort());

                        // open object streams
                        satelliteReadFromNet = new ObjectOutputStream(satelliteSocket.getInputSteam());
                        satelliteWriteFromNet = new ObjectOutputStream(satelliteSocket.getOutputStream());

                        // forward message (as is) to satellite
                        satelliteWriteFromNet.writeObject(message);

                        // receive result from satellite and
                        Object result  = sattelliteReadFromNet.readObject();

                        // write result back to client
                        writeToNet.writeObject(result);writeToNet.writeObject(result);

                    } catch (Exception error) {
                        System.err.println("Server error : " + error);
                        System.exit(1);
                    }

                    break;

                default:
                    System.err.println("[ServerThread.run] Warning: Message type not implemented");
            }
        }
    }

    // main()
    public static void main(String[] args) {
        // start the application server
        Server server = null;
        if (args.length == 1) {
            server = new Server(args[0]);
        } else {
            server = new Server("../../config/Server.properties");
        }
        server.run();
    }
}
