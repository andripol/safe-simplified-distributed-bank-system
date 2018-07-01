/**
 * Created by andri on 29/6/2018.
 */

import java.io.*;
import java.net.*;
import java.util.*;

public class MultiServerToServerThread3 extends MultiServer3 implements  Runnable {

    final private Socket cSocket;
    final private String failure_message = "failed to connect to server";

    MultiServerToServerThread3(Socket cSocket){
        this.cSocket = cSocket;
    }

    public void run() {


        System.out.println("HashMap of server 3: ");
        //System.out.println(accounts.size());
        System.out.println(Arrays.asList(accounts));

        //System.out.println("[Thread:" +  Thread.currentThread().getId() + "]New server connection established");

        Scanner sockReader;
        PrintWriter sockWriter;
        String request;
        String[] request_parts;
        String final_response;

        try {
            char action;
            int cId1, amount, cId2;
            int oBalance1;
            int forwarding;

            sockReader = new Scanner(cSocket.getInputStream());
            request = sockReader.nextLine();
            //System.out.println("[Thread:" +  Thread.currentThread().getId() + "]Server1 to Server3 request: " + request);
            request_parts = request.split(",");

            action = request_parts[0].charAt(0);
            cId1 = Integer.parseInt(request_parts[1]);
            amount = Integer.parseInt(request_parts[2]);
            cId2 = Integer.parseInt(request_parts[3]);
            forwarding = Integer.parseInt(request_parts[4]);

            //create account if this is the first request ever
            if ((cId1 >= 0) && !accounts.containsKey(cId1)){
                accounts.put(cId1, 0);
                account_lock.put(cId1, new Object());
            }


            //send messages and receive response from the third server before proceeding
            String response;
            final_response = "true";

            switch (action) {
                case '+':
                    synchronized (account_lock.get(cId1)) {
                        oBalance1 = accounts.get(cId1);
                        //update yourself
                        accounts.put(cId1, oBalance1 + amount);
                    }

                    if (forwarding == 1)
                        break;

                    //update other servers as well if forwarding = 2
                    if (servers_availability[0] == 1){
                        //wait for response... indicates if the other server updated its map
                        response = forward_to_server_and_get_response(request_parts, forwarding);
                        //System.out.println("Server1 response to Server 3 request: " + response);

                        if (response.equals(failure_message))
                            servers_availability[0] = 0;
                    }

                    break;

                case '-':
                    synchronized (account_lock.get(cId1)){
                        oBalance1 = accounts.get(cId1);
                    }

                    //first check the validity of the transaction yourself
                    if ((oBalance1 - amount) < 0){
                        //System.out.println("[Thread" +  Thread.currentThread().getId() + "] Transaction request by Server2 aborted.");
                        final_response = "false";
                        break;
                    }

                    if (forwarding == 1){
                        synchronized (account_lock.get(cId1)){
                            accounts.put(cId1, oBalance1 - amount);
                        }
                        break;
                    }

                    //update other servers as well if forwarding = 2
                    if (servers_availability[0] == 1){
                        //wait for response... indicates if the other server updated its map
                        response = forward_to_server_and_get_response(request_parts, forwarding);
                        //System.out.println("Server1 response to Server3 request: " + response);

                        if (response.equals("false")) {
                            final_response = "false";
                            break;
                        }
                        else if (response.equals(failure_message))
                            servers_availability[0] = 0;
                    }

                    //successor server responsed with "true" or is down. update yourself and leave
                    synchronized (account_lock.get(cId1)){
                        accounts.put(cId1, oBalance1 - amount);
                    }

                    break;

                case '>':
                    if (!accounts.containsKey(cId2)){
                        accounts.put(cId2, 0);
                        account_lock.put(cId2, new Object());
                    }
                    if (cId1 > cId2) {
                        synchronized (account_lock.get(cId1)) {
                            synchronized (account_lock.get(cId2)) {
                                oBalance1 = accounts.get(cId1);
                            }
                        }

                        //first check the validity of the transaction yourself
                        if ((oBalance1 - amount) < 0){
                            final_response = "false";
                            break;
                        }

                        if (forwarding == 1){
                            synchronized (account_lock.get(cId1)) {
                                synchronized (account_lock.get(cId2)) {
                                    accounts.put(cId1, oBalance1 - amount);
                                    accounts.put(cId2, accounts.get(cId2) + amount);
                                }
                            }
                            break;
                        }

                        //update other servers as well if forwarding = 2
                        if (servers_availability[0] == 1){
                            //wait for response... indicates if the other server updated its map
                            response = forward_to_server_and_get_response(request_parts, forwarding);
                            //System.out.println("Server1 response to Server3 request: " + response);

                            if (response.equals("false")) {
                                final_response = "false";
                                break;
                            }
                            else if (response.equals(failure_message))
                                servers_availability[0] = 0;
                        }

                        //successor server responsed with "true" or is down. update yourself and leave
                        synchronized (account_lock.get(cId1)) {
                            synchronized (account_lock.get(cId2)) {
                                accounts.put(cId1, oBalance1 - amount);
                                accounts.put(cId2, accounts.get(cId2) + amount);
                            }
                        }

                    }
                    else{
                        synchronized (account_lock.get(cId1)) {
                            synchronized (account_lock.get(cId2)) {
                                oBalance1 = accounts.get(cId1);
                            }
                        }

                        //first check the validity of the transaction yourself
                        if ((oBalance1 - amount) < 0){
                            final_response = "false";
                            break;
                        }

                        if (forwarding == 1){
                            synchronized (account_lock.get(cId1)) {
                                synchronized (account_lock.get(cId2)) {
                                    accounts.put(cId1, oBalance1 - amount);
                                    accounts.put(cId2, accounts.get(cId2) + amount);
                                }
                            }
                            break;
                        }

                        //update other servers as well if forwarding = 2
                        if (servers_availability[0] == 1){
                            //wait for response... indicates if the other server updated its map
                            response = forward_to_server_and_get_response(request_parts, forwarding);
                            //System.out.println("Server1 response to Server3 request: " + response);

                            if (response.equals("false")) {
                                final_response = "false";
                                break;
                            }
                            else if (response.equals(failure_message))
                                servers_availability[0] = 0;
                        }

                        //successor server responsed with "true" or is down. update yourself and leave
                        synchronized (account_lock.get(cId1)) {
                            synchronized (account_lock.get(cId2)) {
                                accounts.put(cId1, oBalance1 - amount);
                                accounts.put(cId2, accounts.get(cId2) + amount);
                            }
                        }
                    }
                    break;
                case 'i':
                    Socket cSocketSS;
                    PrintWriter sockWriterSS;
                    Scanner sockReaderSS;

                    //lock the whole account map in order to initialize the asking server
                    synchronized (account_lock){
                        if (forwarding == 1){
                            //say that you locked and wait until initialization is completed
                            sockWriter = new PrintWriter(cSocket.getOutputStream());
                            sockWriter.println("locked");
                            sockWriter.flush();
                            //System.out.println("Waiting for initialization to be completed...");
                            response = "false";

                            sockReader = new Scanner(cSocket.getInputStream());
                            response = sockReader.nextLine();

                            //System.out.println("Initialization completed...");
                            //server 1 back up
                            servers_availability[0] = 1;
                            break;
                        }

                        //the other server (if up) should lock its map too
                        if ((servers_availability[0] == 1) && forwarding == 2){
                            //wait for response... indicates if the other server locked its map
                            try {
                                cSocketSS = new Socket("localhost", servers_ports[0]);
                                sockWriterSS = new PrintWriter(cSocketSS.getOutputStream());
                                sockWriterSS.println(request_parts[0] + "," + request_parts[1] + "," + request_parts[2] + "," + request_parts[3] + "," + Integer.toString(forwarding - 1));
                                sockWriterSS.flush();
                                sockReaderSS = new Scanner(cSocketSS.getInputStream());
                                //wait for response... indicates that the other server locked its map as well
                                response = sockReaderSS.nextLine();

                                //send info to server-client
                                sockReader = new Scanner(cSocket.getInputStream());
                                sockWriter = new PrintWriter(cSocket.getOutputStream());

                                for (Map.Entry<Integer, Integer> entry : accounts.entrySet()) {
                                    sockWriter.println(entry.getKey() + "," + entry.getValue());
                                    sockWriter.flush();
                                    //wait for response, be sure he got it
                                    response = sockReader.nextLine();
                                }
                                //inform that it is all sent
                                sockWriter.println("Done");
                                sockWriter.flush();

                                //inform the successor server so as to stop locking
                                sockWriterSS.println("Done");
                                sockWriterSS.flush();
                                cSocketSS.close();

                                //server 2 back up
                                servers_availability[1] = 1;
                                break;
                            }
                            catch (Exception e){
                                //the successor server is down
                                servers_availability[0] = 0;
                                //send info to server-client
                                sockReader = new Scanner(cSocket.getInputStream());
                                sockWriter = new PrintWriter(cSocket.getOutputStream());

                                for (Map.Entry<Integer, Integer> entry : accounts.entrySet()) {
                                    sockWriter.println(entry.getKey() + "," + entry.getValue());
                                    sockWriter.flush();
                                    //wait for response, be sure he got it
                                    response = sockReader.nextLine();
                                }
                                //inform that it is all sent
                                sockWriter.println("Done");
                                sockWriter.flush();
                                //server 2 back up
                                servers_availability[1] = 1;
                                break;
                            }
                        }
                        if (forwarding == 0){
                            for (Map.Entry<Integer, Integer> entry : accounts.entrySet()) {
                                sockWriter = new PrintWriter(cSocket.getOutputStream());
                                sockWriter.println(entry.getKey() + "," + entry.getValue());
                                sockWriter.flush();
                                //wait for response, be sure he got it
                                sockReader = new Scanner(cSocket.getInputStream());
                                response = sockReader.nextLine();
                            }
                            //inform that it is all sent
                            sockWriter = new PrintWriter(cSocket.getOutputStream());
                            sockWriter.println("Done");
                            sockWriter.flush();
                            //server 1 back up
                            servers_availability[0] = 1;
                            break;
                        }
                    }
                    break;
            }
            if (action != 'i'){
                sockWriter = new PrintWriter(cSocket.getOutputStream());
                sockWriter.println(final_response);
                //System.out.println("Server3 response to Server2 request: " + final_response);
                sockWriter.flush();
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    private String forward_to_server_and_get_response(String[] request_parts, int forwarding){
        Socket cSocketSS;
        PrintWriter sockWriterSS;
        Scanner sockReaderSS;

        try {
            cSocketSS = new Socket("localhost", servers_ports[0]);
            sockWriterSS = new PrintWriter(cSocketSS.getOutputStream());
            sockWriterSS.println(request_parts[0] + "," + request_parts[1] + "," + request_parts[2] + "," + request_parts[3] + "," + Integer.toString(forwarding - 1));
            sockWriterSS.flush();
            sockReaderSS = new Scanner(cSocketSS.getInputStream());
            //wait for response... indicates if other servers updated their map
            String response = sockReaderSS.nextLine();
            cSocketSS.close();
            return response;
        }
        catch (Exception e){
            e.printStackTrace();
            return(failure_message);
        }

    }

}
