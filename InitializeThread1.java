/**
 * Created by andri on 30/6/2018.
 */

import java.io.*;
import java.net.*;
import java.util.*;

public class InitializeThread1 extends MultiServer1 implements Runnable {

    InitializeThread1(){}

    public void run() {

        //System.out.println("Initialization Thread 1 starting..");
        String request; /* server to server request = "Action,Client1,Amount,Client2, forwarding_value"; */
        int forwarding = 2;
        String response;

        request = "i,-1,-1,-1," + forwarding;
        response = send_request_and_initialize_map(0, request);


        if (!response.equals("Done")){
            forwarding = 0;
            request = "i,-1,-1,-1," + forwarding;
            response = send_request_and_initialize_map(1, request );
        }

        if (response.equals("Done")){
            //System.out.println("[Thread:" +  Thread.currentThread().getId() + "]Initialization done.");
        }

        //System.out.println("Initialization Thread 1 exiting..");

    }

    private String send_request_and_initialize_map(int index, String request){
        Socket cSocket;
        PrintWriter sockWriter;
        Scanner sockReader;
        String response ;
        String[] response_parts;

        Integer key, value;

        //send request
        try{
            cSocket = new Socket("localhost", servers_ports[index]);
            sockWriter = new PrintWriter(cSocket.getOutputStream());
            sockWriter.println(request);
            sockWriter.flush();

            //wait for the server to response before running the next request
            sockReader = new Scanner(cSocket.getInputStream());
            response = sockReader.nextLine();
            while (!response.equals("Done")){
                response_parts = response.split(",");
                key = Integer.parseInt(response_parts[0]);
                value = Integer.parseInt(response_parts[1]);

                accounts.put(key, value);
                account_lock.put(key, new Object());

                //System.out.println("just inserted a key-value pair!");
                //inform that you got it
                sockWriter.println("ok");
                sockWriter.flush();


                sockReader = new Scanner(cSocket.getInputStream());
                response = sockReader.nextLine();
            }
            //inform that you got it
            sockWriter.println("ok");
            sockWriter.flush();

            cSocket.close();
            return response;
        }
        catch (Exception e){
            return "Failure";
        }

    }

}
