package roles;

import com.google.gson.*;
import helpers.Site;

import java.io.*;
import java.util.*;

import messaging.MessagingClient;
import messaging.helpers.*;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
    int logPositionMax = Integer.MIN_VALUE;

    static TreeMap<String, String> reservationMap = new TreeMap<>();

    static HashMap<Integer,Integer> flight = new HashMap<>();


    public Learner(Site siteInformation, HashMap<String, Site> siteMap,HashMap<Integer,String> siteIDMap){
        logPositionMax = Integer.MIN_VALUE;
        this.site = siteInformation;
        this.siteHashMap = siteMap;
        this.siteIDMap = siteIDMap;
        this.logMap = new HashMap<>();
        this.flight = new HashMap<>();
        for(int i = 1;i<20;i++)
            flight.put(i,2);

        flight.put(-1,1000);
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

    public HashMap<Integer,Integer> getFlights(){
        return flight;
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

        try(FileWriter fw = new FileWriter("saved_flight.json")){
            Gson gson = new Gson();
            JsonArray arr = new JsonArray();
            for(Map.Entry<Integer,Integer> res: flight.entrySet()){
                JsonObject temp = new JsonObject();
                JsonArray tempArray = new JsonArray();
                String ob = gson.toJson(res.getValue());
                tempArray.add(ob);
                temp.add(res.getKey().toString(),tempArray);
                arr.add(temp);
            }
            fw.append(gson.toJson(arr));
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void lastLogPointer(Message message){

        int position = -1;
        for (int i = log.length - 1; i >=0 ; i--){
            if(log[i] != null){
                position = i;
                break;
            }
        }

        try{
            Site messageFrom = siteHashMap.get(siteIDMap.get(message.getFrom()));
            System.out.println("Sending to "+ messageFrom.getIpAddress() + " " + messageFrom.getRandomPort());
            MessagingClient client = new MessagingClient(messageFrom.getIpAddress(), site.getRandomPort());
            client.send(new LogPositionMessage(position, site.getSiteNumber()), messageFrom.getRandomPort());
            client.close();
        }catch (Exception e){
            System.err.println(e.getStackTrace());
        }

    }

    public void findPointer(){
        Message message = new Message();
        message.setFrom(site.getSiteNumber());
        message.setMessageType(9);
        for(Map.Entry<String, Site> client :siteHashMap.entrySet()){


            try {
                String destinationAddress = client.getValue().getIpAddress();
                int port = client.getValue().getRandomPort();

                if(client.getValue().getSiteNumber() != site.getSiteNumber()){
                    MessagingClient mClient = new MessagingClient(destinationAddress, site.getRandomPort());
                    mClient.send(message, port);
                    mClient.close();
                }
            }
            catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public void setPointer(LogPositionMessage message){
        this.logPositionMax = Integer.max(logPositionMax,message.getLogPosition());
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


    public static void getLogState(){
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
                    if(ob != null && !ob.toString().equals("null"))
                        learner.log[i] = ob.getAsString();
                    i++;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void getDictionary(){
        try {
            //convert the json string back to object
            File f = new File("saved_dictionary.json");
            if(!f.exists())
                return;
            BufferedReader backup = new BufferedReader(new FileReader("saved_dictionary.json"));
            JsonParser parser = new JsonParser();
            JsonElement parsed = parser.parse(backup);
            Gson gson = new Gson();
            int i =0;
            if(!parsed.isJsonNull()) {
                for (JsonElement ob : parsed.getAsJsonArray()) {
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
    public static int getCheckPoint(){
        int checkpoint = 0;
        try {
            //convert the json string back to object
            File f = new File("saved_checkpoint.json");
            if(!f.exists())
                return -1;
            BufferedReader backup = new BufferedReader(new FileReader("saved_checkpoint.json"));
            JsonParser parser = new JsonParser();
            JsonArray parsed = parser.parse(backup).getAsJsonArray();
            Gson gson = new Gson();
            int i =0;
            if(!parsed.isJsonNull()) {
                checkpoint = Integer.parseInt(parsed.get(0).toString());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return checkpoint;
    }
    public static void getStoredFlights(){
        try {
            //convert the json string back to object
            File f = new File("saved_flight.json");
            if(!f.exists())
                return;
            BufferedReader backup = new BufferedReader(new FileReader("saved_flight.json"));
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
                    flight.replace(Integer.parseInt(myId), Integer.parseInt(obj.getAsString()));
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void getState(){

        learner.findPointer();
        getLogState();
        getDictionary();
        getStoredFlights();
        int checkpoint = getCheckPoint();
        if(checkpoint == -1) {
            learner.learnLogsRecovery(0, this.logPositionMax);
        }else{
            learner.learnLogsRecovery(checkpoint, this.logPositionMax);
        }
    }

    private void learnLogsRecovery(int currentPosition, int tillPosition){

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(new Tasks());
        try{
            future.get(50, TimeUnit.MILLISECONDS);
        }catch (Exception e){
            future.cancel(true);
        }
        executor.shutdownNow();

        //Run the synod algorithm for all positions
//        System.out.println("Running from "+ currentPosition +" " + tillPosition);
        for (int i = currentPosition; i <= tillPosition; i++){
            if(log[i] == null){
                //There's a hole, run synod
                System.err.println("% Filling a hole at"+ i);
                Proposer.getInstance(null, null, null)
                        .initiateProposal("reserve test -1,-1","",i);
                Proposer.wonLastRound = false;
                System.err.println("%%%%%%%%%%%%%%%%%%%%%%");

            }
        }
        for (int i = currentPosition; i <= tillPosition; i++){
            if(log[i] != null){
                updateDictionary(log[i]);
            }
        }

    }


    private void learnLogs(int currentPosition){

        //Run the synod algorithm for all positions
        int start = getStart(currentPosition);
        for (int i = start; i <= currentPosition; i++){
            if(log[i] == null){
                //There's a hole, run synod
                System.err.println("% Filling a hole at"+ i);
                Proposer.valueLearned = new HashSet<>();
                Proposer.approvalFrom = new HashSet<>();
                Proposer.wonLastRound = false;
                Proposer.getInstance(null, null, null)
                        .initiateProposal("reserve test -1,-1","",i);
                Proposer.wonLastRound = false;

                System.err.println("%%%%%%%%%%%%%%%%%%%%%%");;

            }
        }

    }

    private int getStart(int currentPosition) {
        int start = 0;
        int offSet = currentPosition % 5;
        int lowerBound = currentPosition - offSet;
        if(log[lowerBound] != null){
            start = lowerBound;
        }
        return start;
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
                        System.err.println("% checkpointing at log position" + requestedLogPosition);
                        new Thread(()->learnLogs(requestedLogPosition)).start();
                        saveDictionary(requestedLogPosition);

                    }
                    else if(log[getStart(requestedLogPosition+1)].equals("null")){
                        System.err.println("% checkpointing at log position" + requestedLogPosition);
                        new Thread(()->learnLogs(getStart(requestedLogPosition+1))).start();
                        saveDictionary(requestedLogPosition);
                    }
                    new Thread(()-> updateDictionary(accVal)).start();
                    System.err.println("% committing " + accVal + " at log position" + requestedLogPosition);
                    new Thread(Learner::saveState).start();
                    System.err.println("% Finished saving states");

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
            String flightNumbers[] = flights.split(",");
            for(String x : flightNumbers){
                flight.replace(Integer.parseInt(x),flight.get(Integer.parseInt(x))-1);
            }
        }
        else {
            String flights = reservationMap.get(reservationFor);
            String flightNumbers[] = flights.split(",");
            for(String x : flightNumbers){
                flight.replace(Integer.parseInt(x),flight.get(Integer.parseInt(x))+1);
            }
            reservationMap.remove(reservationFor);
        }

    }

    public void viewDictionary(){
        for (Map.Entry record: reservationMap.entrySet()){
            System.out.println(record.getKey() + " " + record.getValue());
        }
    }

    public void viewLog(){
        for(int i =0;i<15;i++){
            System.out.println(learner.log[i]);

        }
    }

    public static  String[] getLog(){
        return learner.log;
    }




}
