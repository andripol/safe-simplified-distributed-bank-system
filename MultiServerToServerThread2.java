/**
 * Created by andri on 02/07/2018.
 */

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Map.Entry;

public class MultiServerToServerThread2 extends MultiServer2 implements  Runnable {

    final private Socket cSocket;
    final private String failure_message = "failed to connect to server";
    Scanner sockReader;
    PrintWriter sockWriter;

    MultiServerToServerThread2(Socket cSocket){
        this.cSocket = cSocket;
    }

    public void run() {

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

                        //update other servers as well if forwarding = 2
                        if (forwarding == 2)
                            forward_to_server_and_get_response(request_parts, forwarding-1);

                        //already in majority. update yourself
                        accounts.put(cId1, oBalance1 + amount);

                    }
                    break;

                case '-':
                    synchronized (account_lock.get(cId1)) {
                        oBalance1 = accounts.get(cId1);

                        if ((oBalance1 - amount) < 0){
                            final_response = "false";
                            break;
                        }

                        if (forwarding == 1){
                            accounts.put(cId1, oBalance1 - amount);
                            break;
                        }

                        //update other servers as well if forwarding = 2
                        response = forward_to_server_and_get_response(request_parts, forwarding-1);

                        if (response.equals("true"))
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

                                if ((oBalance1 - amount) < 0){
                                    final_response = "false";
                                    break;
                                }

                                if (forwarding == 1){
                                    accounts.put(cId1, oBalance1 - amount);
                                    accounts.put(cId2, accounts.get(cId2) + amount);
                                    break;
                                }

                                //update other servers as well if forwarding = 2
                                response = forward_to_server_and_get_response(request_parts, forwarding-1);

                                if (response.equals("true")){
                                    accounts.put(cId1, oBalance1 - amount);
                                    accounts.put(cId2, accounts.get(cId2) + amount);
                                }
                            }
                        }
                    }
                    else{
                        synchronized (account_lock.get(cId2)) {
                            synchronized (account_lock.get(cId1)) {
                                oBalance1 = accounts.get(cId1);

                                if ((oBalance1 - amount) < 0){
                                    final_response = "false";
                                    break;
                                }

                                if (forwarding == 1){
                                    accounts.put(cId1, oBalance1 - amount);
                                    accounts.put(cId2, accounts.get(cId2) + amount);
                                    break;
                                }

                                //update other servers as well if forwarding = 2
                                response = forward_to_server_and_get_response(request_parts, forwarding-1);

                                if (response.equals("true")){
                                    accounts.put(cId1, oBalance1 - amount);
                                    accounts.put(cId2, accounts.get(cId2) + amount);
                                }
                            }
                        }
                    }
                    break;

                case 'i':
                    Thread.currentThread().setPriority(10);

                    //iterator over the hashmap
                    Iterator<Entry<Integer, Object>> it = account_lock.entrySet().iterator();
                    recursive_locking_and_update(it, forwarding, request_parts);
                    break;
            }

            if (action != 'i'){
                sockWriter = new PrintWriter(cSocket.getOutputStream());
                sockWriter.println(final_response);
                //System.out.println("Server3 response to Server2 request: " + final_response);
                sockWriter.flush();
            }

            System.out.println(accounts.size());
            System.out.println(Arrays.asList(accounts));

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
            sockWriterSS.println(request_parts[0] + "," + request_parts[1] + "," + request_parts[2] + "," + request_parts[3] + ",1");
            sockWriterSS.flush();
            sockReaderSS = new Scanner(cSocketSS.getInputStream());
            //wait for response... indicates if other servers updated their map
            String response = sockReaderSS.nextLine();
            cSocketSS.close();
            return response;
        }
        catch (Exception e){
            return(failure_message);
        }
    }

    private void recursive_locking_and_update(Iterator<Entry<Integer, Object>> it, int forwarding, String[] request_parts){
        Thread.currentThread().setPriority(10);
        if (it.hasNext()) {
            synchronized (account_lock.get(it.next().getKey())){
                System.out.println("I am here, in recursion!");
                recursive_locking_and_update(it, forwarding, request_parts);
            }
        }
        else{
            System.out.println("I have locked everything!");
            //you got the locks over all the accounts. do your thing
            Socket cSocketSS;
            PrintWriter sockWriterSS;
            Scanner sockReaderSS;

            try{
                if (forwarding == 1){
                    //say that you locked and wait until initialization is completed
                    sockWriter = new PrintWriter(cSocket.getOutputStream());
                    sockWriter.println("locked");
                    sockWriter.flush();
                    //System.out.println("Waiting for initialization to be completed...");

                    sockReader = new Scanner(cSocket.getInputStream());
                    sockReader.nextLine();
                    //System.out.println("Initialization completed...");
                }

                if (forwarding == 2){
                    try {
                        cSocketSS = new Socket("localhost", servers_ports[0]);
                        sockWriterSS = new PrintWriter(cSocketSS.getOutputStream());
                        sockWriterSS.println(request_parts[0] + "," + request_parts[1] + "," + request_parts[2] + "," + request_parts[3] + "," + Integer.toString(forwarding - 1));
                        sockWriterSS.flush();
                        sockReaderSS = new Scanner(cSocketSS.getInputStream());
                        //wait for response... means that the other server locked its map as well
                        sockReaderSS.nextLine();

                        //send info to server-client
                        sockReader = new Scanner(cSocket.getInputStream());
                        sockWriter = new PrintWriter(cSocket.getOutputStream());

                        for (Map.Entry<Integer, Integer> entry : accounts.entrySet()) {
                            sockWriter.println(entry.getKey() + "," + entry.getValue());
                            sockWriter.flush();
                            //wait for response, make sure he got it
                            sockReader.nextLine();
                        }
                        //inform that it is all sent
                        sockWriter.println("Done");
                        sockWriter.flush();

                        //inform the successor server so as to stop locking
                        sockWriterSS.println("Done");
                        sockWriterSS.flush();
                        cSocketSS.close();

                    }
                    catch (Exception e){
                        //send info to server-client
                        sockReader = new Scanner(cSocket.getInputStream());
                        sockWriter = new PrintWriter(cSocket.getOutputStream());

                        for (Map.Entry<Integer, Integer> entry : accounts.entrySet()) {
                            sockWriter.println(entry.getKey() + "," + entry.getValue());
                            sockWriter.flush();
                            //wait for response, be sure he got it
                            sockReader.nextLine();
                        }
                        //inform that it is all sent
                        sockWriter.println("Done");
                        sockWriter.flush();
                    }
                }

                if (forwarding == 0){
                    for (Map.Entry<Integer, Integer> entry : accounts.entrySet()) {
                        sockWriter = new PrintWriter(cSocket.getOutputStream());
                        sockWriter.println(entry.getKey() + "," + entry.getValue());
                        sockWriter.flush();
                        //wait for response, be sure he got it
                        sockReader = new Scanner(cSocket.getInputStream());
                        sockReader.nextLine();
                    }
                    //inform that it is all sent
                    sockWriter = new PrintWriter(cSocket.getOutputStream());
                    sockWriter.println("Done");
                    sockWriter.flush();
                }
                System.out.println("Now I am exiting recursively.. that's style!");
            }
            catch(Exception e){
                e.printStackTrace();
            }


        }
    }

}
