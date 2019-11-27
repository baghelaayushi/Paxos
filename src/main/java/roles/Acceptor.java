package roles;


import com.google.gson.*;

import helpers.AcceptedRequest;

import messaging.helpers.*;
import helpers.Site;
import messaging.MessagingClient;

import java.io.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


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


    static void saveState(){

        try(FileWriter fw = new FileWriter("current_log.json")){
            Gson gson = new Gson();
            JsonArray arr = new JsonArray();
            for(Map.Entry<Integer,AcceptedRequest> entry: instance.acceptedEntries.entrySet()){
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


    }
    public static void getState(){
        try {
            File f = new File("current_log.json");
            if(!f.exists())
                return;
            //convert the json string back to object
            BufferedReader backup = new BufferedReader(new FileReader("current_log.json"));
            JsonParser parser = new JsonParser();
            JsonArray parsed = parser.parse(backup).getAsJsonArray();
            if(!parsed.isJsonNull()){
                Gson gson = new Gson();
                for(JsonElement ob: parsed) {
                    JsonObject temp = ob.getAsJsonObject();
                    System.out.println(temp);
                    Set<String> id = temp.keySet();
                    String myId = "";
                    for (String s : id)
                        myId = s;

                    JsonArray array = temp.getAsJsonArray(myId);
                    JsonElement obj = array.get(0);
                    AcceptedRequest request = gson.fromJson(obj.getAsString(), AcceptedRequest.class);
                    instance.acceptedEntries.put(Integer.parseInt(myId), request);
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

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
                    MessagingClient mClient = new MessagingClient(destinationAddress, site.getRandomPort());
                    mClient.send(learnmessage, port);
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
            String destinationAddress = siteHashMap.get(siteIDMap.get(sender)).getIpAddress();
            int port = siteHashMap.get(siteIDMap.get(sender)).getRandomPort();
            MessagingClient mClient = new MessagingClient(destinationAddress, site.getRandomPort());


            mClient.send(ack, port);
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
//        System.out.println("Accepting for Log Position " + message.getLogPosition());
        if(!acceptedEntries.containsKey(message.getLogPosition())) {
//            System.out.println("adding a new entry for " + message.getLogPosition());

            acceptedEntries.put(message.getLogPosition(), new AcceptedRequest(Integer.parseInt(proposed)));
            saveState();
        }
        AcceptedRequest request = acceptedEntries.get(message.getLogPosition());
        request.setMaxPrepare(Integer.parseInt(proposed));
        acceptedEntries.replace(message.getLogPosition(),request);
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

        int sender = message.getFrom();
        String proposalNumber[] = message.getProposalNumber().split("-");
        String proposed = proposalNumber[0] + proposalNumber[1];

        PrepareAck ackmessage = new PrepareAck();

        if(!acceptedEntries.containsKey(message.getLogPosition())) {

//            System.out.println("new accepted entry created");

            acceptedEntries.put(message.getLogPosition(), new AcceptedRequest(Integer.parseInt(proposed)));
            saveState();
        }

        int prep = acceptedEntries.get(message.getLogPosition()).getMaxPrepare();

        if (Integer.parseInt(proposed) < prep) {
            //sending max prepare in case of Nack
            ackmessage.setAccNum(String.valueOf(maxPrepare));
            ackmessage.setAccValue(acceptedEntries.get(message.getLogPosition()).getAccVal());
//            ackmessage.setLogPosition(message.getLogPosition());
            ackmessage.setMessageType(5);
            ackmessage.setAck(false);
            ackmessage.setFrom(site.getSiteNumber());
            ackmessage.setAccValue(acceptedEntries.get(message.getLogPosition()).getAccVal());

            if(sender == site.getSiteNumber()){

            }else {
                sendAckMessages(sender, ackmessage);
            }

        }
        else{
            try {
                accNum = proposed;
                accValue = message.getProposedValue();
                maxPrepare = Integer.parseInt(proposed);

                if(acceptedEntries.get(message.getLogPosition()).getAccVal() == null ||acceptedEntries.get(message.getLogPosition()).getAccVal().equals(accValue)) {
                    AcceptedRequest acceptedRequest = acceptedEntries.get(message.getLogPosition());
                    acceptedRequest.setMaxPrepare(maxPrepare);
                    acceptedRequest.setAccNum(Integer.parseInt(accNum));
                    acceptedRequest.setAccVal(accValue);
                    acceptedEntries.replace(message.getLogPosition(), acceptedRequest);
                    saveState();

                    ackmessage.setAccNum(accNum);
                    ackmessage.setAccValue(accValue);
                    ackmessage.setAck(true);
                    ackmessage.setMessageType(6);
                    ackmessage.setFrom(site.getSiteNumber());
                    ackmessage.setLogPosition(message.getLogPosition());
                    //sending acceptance message to proposer
                    if (sender == site.getSiteNumber()) {

                        System.err.println("% self- received ack(" + ackmessage.getAccNum() + "," + ackmessage.getAccValue() + "" +
                                ") from site " + ackmessage.getFrom());
                        Proposer.valueLearned.add(sender);
                    } else {
                        Proposer.wonLastRound = false;
                        Proposer.valueLearned = new HashSet<>();
                        System.err.println("% received ack(" + ackmessage.getAccNum() + "," + ackmessage.getAccValue() + "" +
                                ") from site " + ackmessage.getFrom());
                        sendAckMessages(sender, ackmessage);
                    }
                    //sending acceptance to learners


                    sendCommitToLearner(sender,message.getLogPosition());

                }
            }
            catch (Exception e){
                System.out.println(e);
            }

        }

    }
}
