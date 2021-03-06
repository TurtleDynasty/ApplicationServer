package appserver.satellite;

import appserver.job.Job;
import appserver.comm.ConnectivityInfo;
import appserver.job.UnknownToolException;
import appserver.comm.Message;
import static appserver.comm.MessageTypes.JOB_REQUEST;
import static appserver.comm.MessageTypes.REGISTER_SATELLITE;
import appserver.job.Tool;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import utils.PropertyHandler;

/**
 * Class [Satellite] Instances of this class represent computing nodes that execute jobs by
 * calling the callback method of tool implementation, loading the tools code dynamically over a network
 * or locally, if a tool got executed before.
 *
 * @author Dr.-Ing. Wolf-Dieter Otte
 */
public class Satellite extends Thread {

    private ConnectivityInfo satelliteInfo = new ConnectivityInfo();
    private ConnectivityInfo serverInfo = new ConnectivityInfo();
    private HTTPClassLoader classLoader = null;
    private Hashtable toolsCache = null;
    
    private PropertyHandler satelliteProperties;
    private PropertyHandler serverProperties;
    private PropertyHandler classLoaderProperties;

    public Satellite(String satellitePropertiesFile, String classLoaderPropertiesFile, String serverPropertiesFile) {

        // read the configuration information from the file name passed in
        // ---------------------------------------------------------------
        try {
            
            satelliteProperties = new PropertyHandler(satellitePropertiesFile);
            serverProperties = new PropertyHandler(serverPropertiesFile);
            classLoaderProperties = new PropertyHandler(classLoaderPropertiesFile);
            
        } catch (Exception e) {
            System.err.println("Error: " + e);
            System.exit(1);
        }

    
        
        // create a socket info object that will be sent to the server
        try {
            satelliteInfo.setHost((InetAddress.getLocalHost()).getHostAddress());
        } catch (UnknownHostException e) {
            e.printStackTrace();
            System.exit(1);
        }
        satelliteInfo.setPort(Integer.parseInt(satelliteProperties.getProperty("PORT")));
        satelliteInfo.setName(satelliteProperties.getProperty("NAME"));
        
        // get connectivity information of the server
        try {
            PropertyHandler configurationServer = new PropertyHandler(serverPropertiesFile);
            serverInfo.setHost(configurationServer.getProperty("HOST"));
            serverInfo.setPort(Integer.parseInt(configurationServer.getProperty("PORT")));
        } catch (Exception e) {
            // no use carrying on, so bailing out ...
            e.printStackTrace();
            System.exit(1);
        }
        
        // create class loader 
        PropertyHandler classLoaderProperties = null;

        // read class loader config
        try {
            classLoaderProperties = new PropertyHandler(classLoaderPropertiesFile);
        } catch (Exception e) {
            // no use carrying on, so bailing out ...
            e.printStackTrace();
            System.exit(1);
        }

        // get class loader connectivity properties and create class loader
        classLoader = new HTTPClassLoader(classLoaderProperties.getProperty("HOST"), Integer.parseInt(classLoaderProperties.getProperty("PORT")));

        if (classLoader != null) {
            System.err.println("[Satellite.Satellite] HTTPClassLoader created on " + satelliteInfo.getName());
        } else {
            System.err.println("[Satellite.Satellite] Could not create HTTPClassLoader, exiting ...");
            System.exit(1);
        }

        // see init class loader      
        initClassLoader();
        
        // create tools cache
        // -------------------
        toolsCache = new Hashtable();
    }

    @Override
    public void run() {

        // register this satellite with the SatelliteManager on the server
         ObjectOutputStream writeToNet = null;
        Message message = null;

        // connect to the server
        Socket server = null;
        try {
            server = new Socket(serverInfo.getHost(), serverInfo.getPort());
        } catch (IOException ex) {
            System.err.println("[Satellite.run] Opening socket to server failed");
            ex.printStackTrace();
            System.exit(1);
        }
        System.out.println("[Satellite.run] Satellite " + satelliteInfo.getName() + " connected to server, transfer connectivity information ...");

        // setting up output stream
        try {
            writeToNet = new ObjectOutputStream(server.getOutputStream());
            //readFromNet = new ObjectInputStream(server.getInputStream());
        } catch (IOException ex) {
            System.err.println("[Satellite.run] Opening object stream to server failed");
            ex.printStackTrace();
            System.exit(1);
        }

        // creating message containing satellite connectivity information
        message = new Message();
        message.setType(REGISTER_SATELLITE);
        message.setContent(satelliteInfo);

        // sending message object with connectivity info to server
        try {
            writeToNet.writeObject(message);
            writeToNet.flush();
        } catch (Exception ex) {
            System.err.println("[Satellite.run] Writing SatelliteInfo to server failed");
            ex.printStackTrace();
            System.exit(1);
        }
        
        
        // create server socket
        ServerSocket serverSocket;
        
        String satellitePort = satelliteProperties.getProperty("PORT");
        String satelliteName = satelliteProperties.getProperty("NAME");
        // start taking job requests in a server loop     
        // @Note to self Otte's suggestion was different from ours. May need to revist this.   
        try
        { 
            // creates an instance of a server socket 
            serverSocket = new ServerSocket(Integer.parseInt(satellitePort));

            // server loop: infinitely loops and accepts all clients without worrying about race conditions
            while (true) 
            {
                System.out.println(satelliteName +  " waiting to accept a request on port " + satellitePort + "... ");
                //Handle race conditions
               (new Thread(new SatelliteThread(serverSocket.accept() ,this))).start();
            }
        
        } catch (IOException e) {
            System.err.println(satelliteName + " ERROR: " + e);
        }
    }

    // @Note to self this class is also different in Otte's suggestion. Revist this.
    // inner helper class that is instanciated in above server loop and processes job requests
    private class SatelliteThread extends Thread {

        Satellite satellite = null;
        Socket jobRequest = null;
        ObjectInputStream readFromNet = null;
        ObjectOutputStream writeToNet = null;
        Message message = null;
        String satelliteName = satelliteProperties.getProperty("NAME");

        SatelliteThread(Socket jobRequest, Satellite satellite) {
            this.jobRequest = jobRequest;
            this.satellite = satellite;
            
        }
        
        // Calculate the answer and send the response back to the client
        @Override
        public void run() {
            try{ 
                // setting up object streams 
                readFromNet = new ObjectInputStream(jobRequest.getInputStream());
                writeToNet = new ObjectOutputStream(jobRequest.getOutputStream());

                // reading message
               message = (Message) readFromNet.readObject();

               // processing message
                switch (message.getType()) {
                    case JOB_REQUEST:
                        // Gets job from contents of message
                        Job job = (Job) message.getContent();

                        // Finds tool object
                        Tool tool = getToolObject( job.getToolName() );

                        // Calculates result
                        Object result = tool.go(job.getParameters());

                        // sending results back 
                        writeToNet.writeObject(result);
                    
                        System.out.println(satelliteName  + " sent result back to Client.");
                        break;

                    default:
                        System.err.println(satelliteName  +" WARNING: Job and or Tool failed to work properly");
                }

            } catch (IOException e) {
                System.err.println(satelliteName  +" ERROR: Couldn't setup object stream " + e);

            } catch (ClassNotFoundException e){
                System.err.println(satelliteName  +"ERROR: Couldn't not read object stream " + e);

            } catch (Exception e){
                System.err.println(satelliteName  +" ERROR: " + e);

            }
        }
    }
    
    // Gets the appropriate tool. If tool not in cache then load it in.
    public Tool getToolObject(String toolClassString) throws UnknownToolException, ClassNotFoundException, InstantiationException, IllegalAccessException {

        Tool toolObject = null;
        
        // Check if the tool is in the cache, if its not then load it
        // otherwise let the user know
        if ((toolObject = (Tool) toolsCache.get(toolClassString)) == null) 
        {
            System.out.println("Tools Class: " + toolClassString);

            if (toolClassString == null) {
                throw new UnknownToolException();
            }
            
            // load the class and place it into cache
            Class toolClass = classLoader.loadClass(toolClassString);
            toolObject = (Tool) toolClass.newInstance();
            toolsCache.put(toolClassString, toolObject);

        } else {
            System.out.println("Tool: " + toolClassString + " already in Cache");
        }

        return toolObject;
    }

    // read class loader config and get class loader connectivity properties and create class loader 
    // Like the dynamic calculator we are technically connecting to the webserver here
    private void initClassLoader() {
        String host = classLoaderProperties.getProperty("HOST");
        String portString = classLoaderProperties.getProperty("PORT");
        if ((host != null) && (portString != null)) 
        {
            try 
            {
                classLoader = new HTTPClassLoader(host, Integer.parseInt(portString));
            } catch (NumberFormatException e) {
                System.err.println("ERROR: Wrong port number, using Defaults");
            }
            
        } else {
            System.err.println("ERROR: Configuration data incomplete, using Defaults");
        }

        if (classLoader == null) 
        {
            System.err.println("ERROR: Could not create HTTPClassLoader, exiting ...");
            System.exit(1);
        }
    }
    
    // @Grader - Use webServer properties for the classLoader propterties file 
    // An addition to the properties handler class was necessary for this.
    // Also note that strings here work on Linux, but might not on Windows. You need to change file paths accordingly. 
    public static void main(String[] args) {
        // start a satellite
        
        Satellite server = null;
        if(args.length == 3) {
            server = new Satellite(args[0], args[1], args[2]);
        } else {
            server = new Satellite("../../config/Satellite.Earth.properties", "../../config/WebServer.properties" , "../../config/WebServer.properties");
        }
        server.run();
        
    }
}

