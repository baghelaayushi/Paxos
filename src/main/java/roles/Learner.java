package roles;

import com.google.gson.*;
import helpers.Site;

import java.io.*;
import java.util.*;

import messaging.MessagingClient;
import messaging.helpers.*;

import java.util.HashMap;
import java.util.List;

public class Learner {
    static Learner learner = null;
    static String accNum;
    static String accValue;
    HashMap<String, Site> siteHashMap = null;
    HashMap<Integer,String> siteIDMap = null;
    HashMap<Integer,HashMap<String,Integer>> logMap  = null;
    String[] log = new String[1000];
    boolean[] logCheck = new boolean[1000];
    Site site = null;

    static TreeMap<String, String> reservationMap = new TreeMap<>();


    public Learner(Site siteInformation, HashMap<String, Site> siteMap,HashMap<Integer,String> siteIDMap){
        this.site = siteInformation;
        this.siteHashMap = siteMap;
        this.siteIDMap = siteIDMap;
        this.logMap = new HashMap<>();
        accNum = null;
        accValue = null;
    }

    public static Learner getInstance(Site siteInformation, HashMap<String, Site> siteMap,HashMap<Integer,String> siteIDMap){
        if (learner == null){
            learner = new Learner(siteInformation, siteMap,siteIDMap);
        }
        return learner;
    }
    public static Learner getInstance(){
        return learner;
    }

    static void saveDictionary(int checkpoint){
        try(FileWriter fw = new FileWriter("saved_dictionary.json")){
            Gson gson = new Gson();
            JsonArray arr = new JsonArray();
            for(Map.Entry<String,String> res: reservationMap.entrySet()){
                JsonObject temp = new JsonObject();
                JsonArray tempArray = new JsonArray();
                String ob = gson.toJson(res.getValue());
                tempArray.add(ob);
                temp.add(res.getKey(),tempArray);
                arr.add(temp);
            }
            fw.append(gson.toJson(arr));
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        try(FileWriter fw = new FileWriter("saved_checkpoint.json")){
            Gson gson = new Gson();
            JsonArray arr = new JsonArray();
            arr.add(checkpoint);
            fw.append(gson.toJson(arr));
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

    static void saveState(){

        try(FileWriter fw = new FileWriter("saved_log.json")){
            Gson gson = new Gson();
            JsonArray arr = new JsonArray();
            for(String s:learner.log){
                arr.add(s);
            }
            fw.append(gson.toJson(arr));
        }
        catch (IOException e) {
            e.printStackTrace();
        }


    }
    public static void getState(){
        try {
            //convert the json string back to object
            File f = new File("saved_log.json");
            if(!f.exists())
                return;

            BufferedReader backup = new BufferedReader(new FileReader("saved_log.json"));
            JsonParser parser = new JsonParser();
            JsonArray parsed = parser.parse(backup).getAsJsonArray();
            Gson gson = new Gson();
            if(!parsed.isJsonNull()) {
                int i = 0;
                for (JsonElement ob : parsed) {
                    //System.out.println(ob);
                    if(ob != null)
                        learner.log[i] = ob.toString();
                    i++;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            //convert the json string back to object
            File f = new File("saved_dictionary.json");
            if(!f.exists())
                return;
            BufferedReader backup = new BufferedReader(new FileReader("saved_dictionary.json"));
            JsonParser parser = new JsonParser();
            JsonArray parsed = parser.parse(backup).getAsJsonArray();
            Gson gson = new Gson();
            int i =0;
            if(!parsed.isJsonNull()) {
                for (JsonElement ob : parsed) {
                    JsonObject temp = ob.getAsJsonObject();
                    Set<String> id = temp.keySet();
                    String myId = "";
                    for (String s : id)
                        myId = s;

                    JsonArray array = temp.getAsJsonArray(myId);
                    JsonElement obj = array.get(0);
                    reservationMap.put(myId, obj.getAsString());
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void sendLogValue(Message message){
        int sender = message.getFrom();
        try {
            String destinationAddress = siteHashMap.get(siteIDMap.get(sender)).getIpAddress();
            int port = siteHashMap.get(siteIDMap.get(sender)).getRandomPort();
            MessagingClient mClient = new MessagingClient(destinationAddress, port);
            Message logValue = new Message();
            logValue.setLogPosition(message.getLogPosition());
            logValue.setMessageType(10);
            logValue.setlogValue(log[message.getLogPosition()]);
            mClient.send(logValue);
            mClient.close();
        }
        catch (IOException e){
            e.printStackTrace();
        }

    }

    public void learnValue(Message message){
        if(message.getLogValue()!=null){
            log[message.getLogPosition()] = message.getLogValue();
        }
        logCheck[message.getLogPosition()] = true;
        updateDictionary(message.getLogValue());
        saveState();
    }

    private void sendMessages(int logPosition){


        for(Map.Entry<String, Site> client :siteHashMap.entrySet()){

            try {
                String destinationAddress = client.getValue().getIpAddress();
                int port = client.getValue().getRandomPort();

                Message message = new Message();
                message.setFrom(site.getSiteNumber());
                message.setMessageType(9);
                message.setLogPosition(logPosition);

                //to send a request to learn message to all processes
                if(client.getValue().getSiteNumber() != site.getSiteNumber()){
                        MessagingClient mClient = new MessagingClient(destinationAddress, port);

                        mClient.send(message);
                        mClient.close();

                }

            }
            catch (IOException e){
                e.printStackTrace();
            }
        }

    }

    private void learnLogs(int startIndex, int endIndex){
        if(endIndex == 0)
            return;

        for(int i = startIndex;i<endIndex;i++){
            if(log[i] == null)
                sendMessages(i);
        }


    }


    public void learner(LearnMessage message){

        String accNum = message.getAccNum();
        String accVal = message.getAccValue();

        int requestedLogPosition = message.getLogPosition();

        int siteQuorum = siteHashMap.size()/2+1;

        //Have we received anything for this log position?
        if(logMap.containsKey(requestedLogPosition)){

            //Have we received this same copy of accNum and accVal?
             if(logMap.get(requestedLogPosition).containsKey(accNum + '-' + accVal)){

                int count = logMap.get(requestedLogPosition).get(accNum + '-' + accVal);

                if(count+1>=siteQuorum && log[requestedLogPosition]==null){

                    log[requestedLogPosition] = accVal;
                    //logCheck[requestedLogPosition] = true;
                    if((requestedLogPosition+1)%5 == 0){
                      learnLogs(requestedLogPosition-5,requestedLogPosition);
                    }
                    updateDictionary(accVal);
                    System.err.println("% committing " + accVal + " at log position" + requestedLogPosition);

                    saveState();
                    Proposer.valueLearned = new HashSet<>();


                }

                HashMap<String,Integer> temp = logMap.get(requestedLogPosition);
                temp.replace(accNum + '-' + accVal,count+1);
                logMap.replace(requestedLogPosition,temp);
            }
            else {
                logMap.get(requestedLogPosition).put(accNum + '-' + accVal,1);
            }

        }
        else {
            HashMap<String,Integer> temp = new HashMap<>();
            temp.put(accNum + '-' + accVal,1);
            logMap.put(requestedLogPosition,temp);
        }

    }

    public static void updateDictionary(String value){

        String reservationFor = value.split(" ")[1];

        if (value.split(" ")[0].equals("reserve")){
            String flights = value.split(" ")[2];
            reservationMap.put(reservationFor, flights);
        }
        else reservationMap.remove(reservationFor);

    }

    public void viewDictionary(){
        for (Map.Entry record: reservationMap.entrySet()){
            System.out.println(record.getKey() + " " + record.getValue());
        }
    }

    public void viewLog(){
        for(int i =0;i<5;i++){
            System.out.println(learner.log[i]);

        }
    }

    public static  String[] getLog(){
        return learner.log;
    }




}
