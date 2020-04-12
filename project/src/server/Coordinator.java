package server;


import java.util.ArrayList;
import java.util.Date;

/**
 * This class contains methods that handles two-phase-commit
 * trasnsactions between participants
 */
public class Coordinator {

    private ArrayList<ClientHandler> participants = new ArrayList<>();
    private String tractionStatement;
    private String status;
    private int timeout = 40;

    /**
     * constructor that sets the participants list
     *
     * @param participants      An ArrayList of ClientHandler objects
     */
    public Coordinator(ArrayList<ClientHandler> participants){
        this.participants = participants;
    }

    /**
     * This method checks if all participants areready to commit a new transaction
     *
     * @param query     The description of the transaction
     * @return          true if all participants are ready to commit. else false
     */
    private boolean initTransaction(String query){

        this.tractionStatement = query;
        System.out.println("Initiating transaction:\n" + this.tractionStatement + "\n");
        messageAll("NEW TRANSACTION--" + this.tractionStatement + "--READY TO COMMIT?");

        long start = System.currentTimeMillis();
        long timer = 0L;
        int resCount = 0;
        String answer = "";
        while(resCount < participants.size()){
            for(ClientHandler party: participants){
                answer = party.readFromParticipant();
                if(answer.equals("YES")){
                    resCount++;
                    System.out.println("participant nr. " + party.getId() + " is ready to commit\n");
                }else if(answer.equals("NO")){
                    System.out.println("Transaction aborted by participant nr. " + party.getId() + "\n");
                    messageAll("TRANSACTION--" + this.tractionStatement + "--Rollback");
                    return false;
                }
            }
            timer = (new Date().getTime() - start) / 1000;
            if(timer >= this.timeout){
                System.out.println("Transaction aborted due to timeout\n");
                messageAll("TRANSACTION--" + this.tractionStatement + "--Rollback");
                return false;
            }
        }
        System.out.println("transcation successfully initiated\n");
        return true;
    }

    /**
     * This method instructs all participants to commit, and checks if they
     * are all successfull
     * @return      true if all participants commit successfully. else false
     */
    private boolean commitTransaction(){

        System.out.println("Commiting transaction");
        messageAll("TRANSACTION--" + this.tractionStatement + "--COMMIT");

        long start = System.currentTimeMillis();
        long timer = 0L;
        int resCount = 0;
        String answer = "";
        while(resCount < participants.size()){
            for(ClientHandler party: participants){
                answer = party.readFromParticipant();
                if(answer.equals("COMMITTED")){
                    resCount++;
                    System.out.println("participant nr. " + party.getId() + " is ready to commit\n");
                }else if(answer.equals("ABORTED")){
                    System.out.println("Transaction aborted by participant nr. " + party.getId() + "\n");
                    messageAll("TRANSACTION--" + this.tractionStatement + "--ROLLBACK");
                    return false;
                }
            }
            timer = (new Date().getTime() - start) / 1000;
            if(timer >= this.timeout){
                System.out.println("Transaction aborted due to timeout");
                messageAll("TRANSACTION--" + this.tractionStatement + "--ROLLBACK");
                return false;
            }
        }
        System.out.println("Transaction commited\n");
        messageAll("TRANSACTION--" + this.tractionStatement + "--SUCCESS");
        return true;
    }

    /**
     * This method sends a message to all participants
     *
     * @param query     the message to be sent
     */
    private void messageAll(String query){
        String message = query;
        for(ClientHandler party: participants){
            party.sendToParticipant(message);
        }
    }

    /**
     * This method starts a loop that handles incoming transactions
     */
    public void start(){
        String query = "";
        boolean waiting = true;
        while(true){
            System.out.println("Waiting for transaction request\n");
            while(waiting){
                for(ClientHandler party: participants){
                    query = party.readFromParticipant();
                    if(!query.equals("")){
                        System.out.println("Got request\n");
                        query = query.split("--")[1];
                        System.out.println("query: " + query);
                        waiting = false;
                        break;
                    }
                }
            }
            if(initTransaction(query)){
                commitTransaction();
            }
            waiting = true;
        }
    }

}
