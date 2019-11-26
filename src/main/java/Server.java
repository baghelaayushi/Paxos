import com.google.gson.*;
import helpers.Site;
import messaging.MessagingServer;
import roles.Acceptor;
import roles.Learner;
import roles.Proposer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

public class Server {

    private static int number_of_hosts = -1;
    private static HashMap<String, Site> siteHashMap = new HashMap<>();
    private static HashMap<Integer,String> siteIDMap = new HashMap<>();
    private static Site mySite = null;
    private static List<String> log = new ArrayList<>();

    private static Proposer proposer = null;
    private static Acceptor acceptor = null;
    private static Learner learner = null;

    public static void main(String[] args) {

        bootstrapProject(args.length == 0 ? "apple" : args[0]);
        acceptUserInput();

    }



    public static void bootstrapProject(String selfIdentifier){

//        System.out.println("Starting as " + selfIdentifier);
        try {
            processHosts(selfIdentifier);
            initialize();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void processHosts(String self) throws FileNotFoundException {

        BufferedReader hosts = new BufferedReader(new FileReader("./knownhosts.json"));
        Gson gson =new Gson();
        JsonParser parser = new JsonParser();
        JsonObject hostsObject = parser.parse(hosts).getAsJsonObject().get("hosts").getAsJsonObject();

        number_of_hosts = hostsObject.keySet().size();
        int site_number = 1 ;

        for (Map.Entry<String, JsonElement> host : hostsObject.entrySet()){
            JsonObject siteInfo = host.getValue().getAsJsonObject();
            siteIDMap.put(site_number,host.getKey());
//            siteInfo.add("ip_address",new JsonPrimitive("127.0.0.1"));
            Site site = new Site(siteInfo.get("ip_address").getAsString(),
                    siteInfo.get("udp_start_port").getAsString(),
                    siteInfo.get("udp_end_port").getAsString(),site_number++);

            siteHashMap.put(host.getKey(), site);
//            System.out.println("Adding to SHM" + host.getKey() + " " + site.getRandomPort());

            if(host.getKey().equalsIgnoreCase(self)){
//                System.out.println("Myself " + site.getIpAddress() + " " + site.getRandomPort());
                mySite = site;
            }
        }

    }



    private static void initialize() throws Exception{

//        System.out.println("Starting local server at "+ mySite.getRandomPort());
        MessagingServer server = new MessagingServer(mySite.getRandomPort());


        new Thread(()-> proposer = Proposer.getInstance(mySite,log, siteHashMap)).start();
        new Thread(()-> acceptor = Acceptor.getInstance(mySite,siteHashMap,siteIDMap)).start();
        new Thread(()-> learner = Learner.getInstance(mySite,siteHashMap,siteIDMap)).start();

        Acceptor.getState();
        Learner.getState();




        new Thread(() -> {
            try {
                server.listen();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();


    }

    private static void acceptUserInput() {

        Scanner in = new Scanner(System.in);
        while (true){


            String inp = in.nextLine();
            String input[] = inp.split(" ");
            String command = input[0];
            String response;

            switch (command) {
                case "reserve":
                    //TODO: Reserve seats
                    proposer.initiateProposal(inp,"reserve",0);
                    break;
                case "cancel":
                    proposer.initiateProposal(inp,"cancel",0);
                    break;
                case "view":
                    learner.viewDictionary();
                    break;
                case "log":
                    learner.viewLog();
                    break;
                case "exit":
                    File savedLog = new File("./saved_log.json");
                    File currentLog = new File("./current_log.json");
                    try{
                        savedLog.delete();
                        currentLog.delete();
                    }catch (Exception e){

                    }
                    System.exit(0);
                default:
            }

        }
    }


}
