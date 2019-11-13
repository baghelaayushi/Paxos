import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import helpers.Site;
import messaging.MessagingServer;
import roles.Acceptor;
import roles.Proposer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;

public class Server {

    private static int number_of_hosts = -1;
    private static HashMap<String, Site> siteHashMap = new HashMap<>();
    private static Site mySite = null;
    private static List<String> log = new ArrayList<>();

    private static Proposer proposer = null;
    private static Acceptor acceptor = null;

    public static void main(String[] args) {

        bootstrapProject(args.length == 0 ? "apple" : args[0]);
        acceptUserInput();

    }



    public static void bootstrapProject(String selfIdentifier){

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
        int site_number =0 ;

        for (Map.Entry<String, JsonElement> host : hostsObject.entrySet()){
            JsonObject siteInfo = host.getValue().getAsJsonObject();
            Site site = new Site(siteInfo.get("ip_address").getAsString(),
                    siteInfo.get("udp_start_port").getAsString(),
                    siteInfo.get("udp_end_port").getAsString(),site_number++);

            siteHashMap.put(host.getKey(), site);

            if(host.getKey().equalsIgnoreCase(self)){
                mySite = site;
            }
        }

    }

    private static void initialize() throws Exception{

        MessagingServer server = new MessagingServer(mySite.getRandomPort());

        proposer = Proposer.getInstance(mySite,log, siteHashMap);

        acceptor = Acceptor.getInstance(mySite,siteHashMap);

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

            String input[] = in.nextLine().split(" ");
            String command = input[0];
            String response;

            switch (command) {
                case "reserve":
                    //TODO: Reserve seats
                    proposer.initiateProposal(command);
                    break;
                case "cancel":
                    //TODO: Cancel seats
                    break;
                case "view":
                    //TODO: view
                    break;
                case "log":
                    //TODO: log
                    break;
                case "exit":
                    System.exit(0);
                default:
            }

        }
    }


}
