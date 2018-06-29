/**
 * Created by andri on 29/6/2018.
 */

import java.io.*;
import java.net.*;
import java.util.*;

public class MultiServerToServerThread2 extends MultiServer2 implements  Runnable {

    final private Socket cSocket;

    MultiServerToServerThread2(Socket cSocket){
        this.cSocket = cSocket;
    }

    public void run() {

        Scanner sockReader;
        PrintWriter sockWriter;
        String request;
        String[] request_parts;
        String final_response = "true";

        try {
            char action;
            int cId1, amount, cId2;
            int oBalance1, oBalance2;
            int nBalance1, nBalance2;
            int forwarding;

            sockReader = new Scanner(cSocket.getInputStream());
            request = sockReader.nextLine();
            System.out.println("[Thread:" +  Thread.currentThread().getId() + "]Server 1 " + "to Server2 Request: " + request);
            request_parts = request.split(",");

            action = request_parts[0].charAt(0);
            cId1 = Integer.parseInt(request_parts[1]);
            amount = Integer.parseInt(request_parts[2]);
            cId2 = Integer.parseInt(request_parts[3]);
            forwarding = Integer.parseInt(request_parts[4]);


            //create account if this is the first request ever
            if (!accounts.containsKey(cId1)){
                accounts.put(cId1, 0);
                account_lock.put(cId1, new Object());
            }

            //send messages and receive response from the third server before proceeding
            Socket cSocketSS;
            PrintWriter sockWriterSS;
            Scanner sockReaderSS;
            String response;

            final_response = "true";

            switch (action) {

                case '+':
                    System.out.println("here2");
                    synchronized (account_lock.get(cId1)){

                        oBalance1 = accounts.get(cId1);

                        //update yourself
                        accounts.put(cId1, oBalance1 + amount);

                        if (forwarding == 1)
                            break;

                        //update the 3rd server as well if forwarding = 2
                        cSocketSS = new Socket("localhost", servers_ports[0]);
                        sockWriterSS = new PrintWriter(cSocketSS.getOutputStream());
                        sockWriterSS.println(request_parts[0] + "," + request_parts[1] + "," + request_parts[2] + "," + request_parts[3] + "," + Integer.toString(forwarding - 1));
                        sockWriterSS.flush();
                        sockReaderSS = new Scanner(cSocketSS.getInputStream());
                        //wait for response... indicates if other servers updated their map
                        response = sockReaderSS.nextLine();
                        System.out.println("Server2 to Server 3 request: " + response);
                        cSocketSS.close();
                        break;

                    }

                case '-':
                    synchronized (account_lock.get(cId1)){

                        //create account if this is the first request ever
                        if (!accounts.containsKey(cId1)){
                            accounts.put(cId1, 0);
                            account_lock.put(cId1, new Object());
                        }

                        oBalance1 = accounts.get(cId1);
                        //first check the validity of the transaction yourself
                        if ((oBalance1 - amount) < 0){
                            System.out.println("[Thread" +  Thread.currentThread().getId() + "] Transaction request by Server1 aborted.");
                            final_response = "false";
                            break;
                        }

                        if (forwarding == 1){
                            accounts.put(cId1, oBalance1 - amount);
                            break;
                        }

                        //ask the 3rd server as well if forwarding = 2
                        cSocketSS = new Socket("localhost", servers_ports[0]);
                        sockWriterSS = new PrintWriter(cSocketSS.getOutputStream());
                        sockWriterSS.println(request_parts[0] + "," + request_parts[1] + "," + request_parts[2] + "," + request_parts[3] + "," + Integer.toString(forwarding - 1));
                        sockWriterSS.flush();
                        sockReaderSS = new Scanner(cSocketSS.getInputStream());
                        //wait for response... indicates that 3rd server updated its map
                        response = sockReaderSS.nextLine();
                        cSocketSS.close();

                        if (response.equals("true")) {
                            accounts.put(cId1, oBalance1 - amount);
                        }
                        else {
                            final_response = "false";
                        }
                    }
                    break;

                case '>':

                    if (cId1 > cId2) {
                        synchronized (account_lock.get(cId1)){
                            synchronized (account_lock.get(cId2)) {
                                oBalance1 = accounts.get(cId1);
                                //first check the validity of the transaction yourself
                                if ((oBalance1 - amount) < 0){
                                    final_response = "false";
                                    break;
                                }

                                if (forwarding == 1){
                                    nBalance1 = oBalance1 - amount;
                                    accounts.put(cId1, nBalance1);
                                    oBalance2 = accounts.get(cId2);
                                    accounts.put(cId2, oBalance2 + amount);
                                    break;
                                }

                                //ask the 3rd server as well if forwarding = 2
                                cSocketSS = new Socket("localhost", servers_ports[0]);
                                sockWriterSS = new PrintWriter(cSocketSS.getOutputStream());
                                sockWriterSS.println(request_parts[0] + "," + request_parts[1] + "," + request_parts[2] + "," + request_parts[3] + "," + Integer.toString(forwarding - 1));
                                sockWriterSS.flush();
                                sockReaderSS = new Scanner(cSocketSS.getInputStream());
                                //wait for response... indicates if other servers updated their map
                                response = sockReaderSS.nextLine();
                                cSocketSS.close();

                                if (response.equals("true")) {
                                    nBalance1 = oBalance1 - amount;
                                    accounts.put(cId1, nBalance1);
                                    oBalance2 = accounts.get(cId2);
                                    accounts.put(cId2, oBalance2 + amount);
                                }
                                else {
                                    final_response = "false";
                                }
                            }
                        }
                    }
                    else{
                        synchronized (account_lock.get(cId2)){
                            synchronized (account_lock.get(cId1)) {
                                oBalance1 = accounts.get(cId1);
                                //first check the validity of the transaction yourself
                                if ((oBalance1 - amount) < 0){
                                    final_response = "false";
                                    break;
                                }

                                if (forwarding == 1){
                                    nBalance1 = oBalance1 - amount;
                                    accounts.put(cId1, nBalance1);
                                    oBalance2 = accounts.get(cId2);
                                    accounts.put(cId2, oBalance2 + amount);
                                    break;
                                }

                                //ask the 3rd server as well if forwarding = 2
                                cSocketSS = new Socket("localhost", servers_ports[0]);
                                sockWriterSS = new PrintWriter(cSocketSS.getOutputStream());
                                sockWriterSS.println(request_parts[0] + "," + request_parts[1] + "," + request_parts[2] + "," + request_parts[3] + "," + Integer.toString(forwarding - 1));
                                sockWriterSS.flush();
                                sockReaderSS = new Scanner(cSocketSS.getInputStream());
                                //wait for response... indicates if other servers updated their map
                                response = sockReaderSS.nextLine();
                                cSocketSS.close();

                                if (response.equals("true")) {
                                    nBalance1 = oBalance1 - amount;
                                    accounts.put(cId1, nBalance1);
                                    oBalance2 = accounts.get(cId2);
                                    accounts.put(cId2, oBalance2 + amount);
                                }
                                else {
                                    final_response = "false";
                                }
                            }
                        }
                    }
                    break;
            }
            sockWriter = new PrintWriter(cSocket.getOutputStream());
            sockWriter.println(final_response);
            sockWriter.flush();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

}
