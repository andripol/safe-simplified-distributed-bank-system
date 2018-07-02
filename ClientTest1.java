import java.io.*;
import java.net.*;
import java.util.*;

class ClientRunTest implements Runnable {

    ClientRunTest() {}

    public void run() {

        final int requests = 5000;
        final Random rand = new Random();

        /* request = "Action,Client1,Amount,Client2"; */

        int action_nr, amount, cId1, cId2, answer;

        char action;

        try {
            for (int request_id = 0; request_id < requests; request_id++) {

                Thread.sleep(rand.nextInt(10));

                action_nr = rand.nextInt(4);
                amount = rand.nextInt(20) + 1;

                cId1 = rand.nextInt(ClientTest1.cThreads);
                cId2 = rand.nextInt(ClientTest1.cThreads);

                switch (action_nr) {
                    case 0:
                        action = '+';
                        //balance = balance + amount;
                        break;
                    case 1:
                        action = '-';
                        //balance = balance - amount;
                        break;
                    case 2:
                        action = '>';
                        //balance = balance - amount;
                        break;
                    default:
                        action = '?';
                        break;
                }

                new Thread(new ClientRun(cId1, action, amount, cId2 )).start();

                //System.out.println("Client " + cId1 + ", Request " + request + " to Server " + sId);

            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

public class ClientTest1 {

    final static int cThreads = 20;

    public static void main(String[] args) {

        new Thread(new ClientRunTest()).start();
    }
}