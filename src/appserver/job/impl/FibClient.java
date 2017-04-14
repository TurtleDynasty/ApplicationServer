package appserver.client;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Properties;
import utils.PropertyHandler;
import appserver.comm.Message;
import appserver.comm.MessageTypes;
import appserver.job.Job;

public class FibClient extends Thread implements MessageTypes{

    Properties properties;
    Integer num;
    string host = null;
    int port;

    public FibClient(String serverPropertiesFile, Integer num) {
        try {

            properties = new PropertyHandler(serverPropertiesFile);
            host = properties.getProperty("HOST");
            System.out.println("FibClient host : " + host);
            port = Integer.parseInt(properties.getProperty("PORT"));
            System.out.println("FibClient port : " + port);

        } catch (Exception error) {
            System.err.println("FibClient error : " + error);
            error.printStackTrace();
        }
        this.num = num;
    }
    @Override
    public void run() {
        try {
            //@Grader adapted from PlusOneClient
            // connect to application server
            Socket server = new Socket(host, port);

            // string of class (tool name)
            String classString = "appserver.job.impl.Fib";
            Integer number = new Integer(this.num);

            // create job and request message
            Job job = new Job(classString, num);
            Message message = new Message(JOB_REQUEST, job);

            // send job to application server
            ObjectOutputStream writeToNet = new ObjectOutputStream(server.getOutputStream());
            writeToNet.writeObject(message);

            // result from application server
            ObjectInputStream readFromNet = new ObjectInputStream(server.getInputStream());
            Integer result = (Integer) readFromNet.readObject();
            System.out.println("RESULT: " + result);

        } catch (Exception error) {
            System.err.println("FibClient.run error : " + error);
            error.printStackTrace();
        }
    }

    public static void main(String[] args) {

        for (int i=48;i>0;i--){
            (new FibClient("../../config/Server.properties",i)).start();
        }

    }

}

