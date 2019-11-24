package roles;


import com.google.gson.*;
import helpers.Event;

import helpers.AcceptedRequest;

import messaging.helpers.*;
import helpers.Site;
import messaging.MessagingClient;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


//maxPre
//accNum
//accVal

public class Acceptor {
    static Acceptor instance = null;
    int maxPrepare = -1;
    HashMap<Integer, AcceptedRequest>  acceptedEntries = null;
    String accNum;
    String accValue;
    HashMap<String, Site> siteHashMap = null;
    HashMap<Integer,String> siteIDMap = null;
    Site site = null;

    public Acceptor(Site siteInformation, HashMap<String, Site> siteMap,HashMap<Integer,String> siteIDMap){
        this.site = siteInformation;
        this.maxPrepare = 0;
        this.siteHashMap = siteMap;
        this.siteIDMap = siteIDMap;

        this.acceptedEntries = new HashMap<>();
        accNum = null;
        accValue = null;
    }

    public static Acceptor getInstance(Site siteInformation, HashMap<String, Site> siteMap,HashMap<Integer,String> siteIDMap){
        if (instance == null){
            instance = new Acceptor(siteInformation, siteMap,siteIDMap);
        }
        return instance;
    }
    public static Acceptor getInstance(){
        return instance;
    }


    /*static void saveState(){

        try(FileWriter fw = new FileWriter("current_log.json")){
            Gson gson = new Gson();
            JsonArray arr = new JsonArray();
            for(Map.Entry<Integer,List<Integer>> entry: accEntry.entrySet()){
                JsonObject temp = new JsonObject();
                JsonArray tempArray = new JsonArray();
                String ob = gson.toJson(entry.getValue());
                tempArray.add(ob);
                temp.add(entry.getKey().toString(),tempArray);
                arr.add(temp);
            }
            fw.append(gson.toJson(arr));
        }
        catch (IOException e) {
            e.printStackTrace();
        }


    }*/
    /*static void getState(){
        try {
            //convert the json string back to object
            BufferedReader backup = new BufferedReader(new FileReader("current_log.json"));
            JsonParser parser = new JsonParser();
            JsonArray parsed = parser.parse(backup).getAsJsonArray();
            Gson gson = new Gson();
            accEntry = new HashMap<>();
            for(JsonElement ob: parsed){
                JsonObject temp = ob.getAsJsonObject();
                Set<String> id = temp.keySet();
                String s = "";
                for(String myId:id)
                    s = myId;

                JsonArray array = temp.getAsJsonArray(s);
                JsonElement obj = array.get(0);
                List<String> cl = gson.fromJson(obj.getAsString(),List.class);
                List<Integer> values = new ArrayList<>();
                values.add(Integer.parseInt(cl.get(0)));
                values.add(Integer.parseInt(cl.get(1)));
                values.add(Integer.parseInt(cl.get(2)));
                accEntry.put(Integer.parseInt(s),values);

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }*/

    private void sendCommitToLearner(int sender, int logPosition){
        Learner instance = Learner.getInstance();
        LearnMessage learnmessage = new LearnMessage();

        learnmessage.setAccNum(String.valueOf(acceptedEntries.get(logPosition).getAccNum()));
        learnmessage.setAccValue(String.valueOf(acceptedEntries.get(logPosition).getAccVal()));

        learnmessage.setMessageType(8);
        learnmessage.setFrom(site.getSiteNumber());
        learnmessage.setLogPosition(logPosition);

        for(Map.Entry<String, Site> client :siteHashMap.entrySet()){


            try {
                String destinationAddress = client.getValue().getIpAddress();
                int port = client.getValue().getRandomPort();

                if(client.getValue().getSiteNumber() == site.getSiteNumber()){
                    instance.learner(learnmessage);
                }
                else {
                    MessagingClient mClient = new MessagingClient(destinationAddress, port);
                    mClient.send(learnmessage);
                    mClient.close();
                }
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }

    }

    private void sendAckMessages(int sender, PrepareAck ack){
            try {
                System.out.println("Sender is" + siteHashMap.get(siteIDMap.get(sender)).getIpAddress());
                String destinationAddress = siteHashMap.get(siteIDMap.get(sender)).getIpAddress();
                int port = siteHashMap.get(siteIDMap.get(sender)).getRandomPort();
                MessagingClient mClient = new MessagingClient(destinationAddress, port);


                mClient.send(ack);
                mClient.close();
            }
            catch (IOException e){
                e.printStackTrace();
            }

    }

    private void sendReconcileMessage(int sender, ReconcileMessage ack){
        try {
            String destinationAddress = siteHashMap.get(siteIDMap.get(sender)).getIpAddress();
            int port = siteHashMap.get(siteIDMap.get(sender)).getRandomPort();
            MessagingClient mClient = new MessagingClient(destinationAddress, port);


            mClient.send(ack);
            mClient.close();
        }
        catch (IOException e){
            e.printStackTrace();
        }

    }

    public void processPrepareRequest(PrepareMessage message) {
        Proposer proposer = Proposer.getInstance(null,null,null);
        int sender = message.getFrom();
        String proposalNumber[] = message.getProposalNumber().split("-");
        String proposed = proposalNumber[0] + proposalNumber[1];


        PrepareAck ackmessage = new PrepareAck();
        //if the log entry is empty for acceptor too
        //create a new entry in hashmap with max prepare as proposed and accnum and acc val as -1
        System.out.println("Accepting for Log Position " + message.getLogPosition());
        if(!acceptedEntries.containsKey(message.getLogPosition())) {
            System.out.println("adding a new entry for " + message.getLogPosition());

            acceptedEntries.put(message.getLogPosition(), new AcceptedRequest(Integer.parseInt(proposed)));
        }
        int prep = acceptedEntries.get(message.getLogPosition()).getMaxPrepare();

        if (Integer.parseInt(proposed) < prep) {
            ackmessage.setAck(false);
        }
        else
            ackmessage.setAck(true);

        ackmessage.setAccNum(String.valueOf(acceptedEntries.get(message.getLogPosition()).getAccNum()));
        ackmessage.setAccValue(acceptedEntries.get(message.getLogPosition()).getAccVal());
        ackmessage.setFrom(site.getSiteNumber());
        ackmessage.setLogPosition(message.getLogPosition());

        ackmessage.setMessageType(3);

        if(sender == site.getSiteNumber()){
            proposer.processProposalAcks(ackmessage,true);
        }
        else {
            sendAckMessages(sender, ackmessage);
        }
        maxPrepare = Integer.parseInt(proposed);

    }

    public void processAcceptRequest(AcceptMessage message) {

        Proposer proposer = Proposer.getInstance(null,null,null);

        int sender = message.getFrom();
        String proposalNumber[] = message.getCompleteProposalNumber().split("-");
        String proposed = proposalNumber[0] + proposalNumber[1];

        PrepareAck ackmessage = new PrepareAck();

        if(!acceptedEntries.containsKey(message.getLogPosition())) {
            System.out.println("adding a new entry for " + message.getLogPosition());

            acceptedEntries.put(message.getLogPosition(), new AcceptedRequest(Integer.parseInt(proposed)));
        }

        int prep = acceptedEntries.get(message.getLogPosition()).getMaxPrepare();
        if (Integer.parseInt(proposed) < prep) {
            //sending max prepare in case of Nack
            ackmessage.setaccNum(String.valueOf(maxPrepare));
            ackmessage.setMessageType(5);
            ackmessage.setAck(false);

            if(sender == site.getSiteNumber()){
                //TODO for nack
            }else {
                sendAckMessages(sender, ackmessage);
            }

        }
        else{
            try {
                accNum = proposed;
                accValue = message.getProposedValue();
                maxPrepare = Integer.parseInt(proposed);


                AcceptedRequest acceptedRequest = acceptedEntries.get(message.getLogPosition());
                acceptedRequest.setMaxPrepare(maxPrepare);
                acceptedRequest.setAccNum(Integer.parseInt(accNum));
                acceptedRequest.setAccVal(accValue);
                acceptedEntries.replace(message.getLogPosition(), acceptedRequest);

                ackmessage.setaccNum(accNum);
                ackmessage.setAccValue(accValue);
                ackmessage.setAck(true);
                ackmessage.setMessageType(6);
                ackmessage.setLogPosition(message.getLogPosition());
                //sending acceptance message to proposer
                if(sender == site.getSiteNumber()){

                    System.out.println("sending to myself");

                }else {
                    sendAckMessages(sender, ackmessage);
                }
            }
            catch (Exception e){
                System.out.println(e);
            }

            //sending acceptance to learners
            System.out.println("Sending message to learner");
            sendCommitToLearner(sender,message.getLogPosition());
        }

    }
}
