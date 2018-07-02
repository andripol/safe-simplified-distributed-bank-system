/**
 * Created by andri on 2/7/2018.
 */


import java.io.*;
import java.net.*;
import java.util.*;

public class InitializeThread3 extends MultiServer3 implements Runnable {

    String failure_message = "failed to connect to server";

    InitializeThread3(){    }

    public void run() {
        System.out.println("Initialization Thread 1 starting..");
        /* server to server request = "Action,Client1,Amount,Client2, forwarding_value"; */
        String response1, response2;

        int next_server_to_send = 0;
        String request = "i,-1,-1,-1";

        response1 = send_request_and_initialize_map(next_server_to_send,request + ",2");

        if (response1.equals(failure_message)) {
            next_server_to_send = 1;
            response2 = send_request_and_initialize_map(next_server_to_send, request + ",0");
            if (response2.equals(failure_message)) {
                System.out.println("No majority. Request aborted.");
            }
        }

        System.out.println("Initialization Thread 1 exiting..");
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

                //inform that you got it
                sockWriter.println("ok");
                sockWriter.flush();

                response = sockReader.nextLine();
            }
            //inform that you got it
            //sockWriter.println("ok");
            //sockWriter.flush();

            cSocket.close();
            return response;
        }
        catch (Exception e){
            return failure_message;
        }

    }

}
