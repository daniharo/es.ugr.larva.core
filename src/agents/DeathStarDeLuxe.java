/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package agents;

import data.Ole;
import data.OleFile;
import java.util.HashMap;
import messaging.ACLMessageTools;
import swing.LARVAAirTrafficControl3D;
import swing.LARVAAirTrafficControlTiles;
import swing.LARVAMiniDash;

/**
 *
 * @author Anatoli Grishenko Anatoli.Grishenko@gmail.com
 */
public class DeathStarDeLuxe extends LARVAFirstAgent {

    enum Status {
        CHECKIN, CHECKOUT, IDLE, HASSESSION, UPDATE, EXIT
    }
    Status myStatus;

    protected HashMap<String, LARVAMiniDash> AgentDash;
    protected LARVAAirTrafficControl3D TheMap;
    protected String sessionKey = "";

    @Override
    public void setup() {
        super.setup();
        logger.offEcho();
        logger.onTabular();
        myStatus = Status.CHECKIN;
        TheMap = new LARVAAirTrafficControl3D();
        TheMap.setTitle("DEATH STAR DE LUXE");
        Info("Setting Death Star up");
        exit = false;
    }

    @Override
    public void Execute() {
        Info("Status: " + myStatus.name());
        switch (myStatus) {
            case CHECKIN:
                myStatus = MyCheckin();
                break;
            case IDLE:
                myStatus = myIdle();
                break;
            case CHECKOUT:
                myStatus = MyCheckout();
                break;
            case EXIT:
            default:
                exit = true;
                break;
        }

    }

    @Override
    public void takeDown() {
        Info("Taking down and deleting agent");
        super.takeDown();
    }

    public Status MyCheckin() {
        Info("Loading passport and checking-in to LARVA");
        if (!loadMyPassport("passport/MyPassport.passport")) {
            Error("Unable to load passport file");
            return Status.EXIT;
        }
        if (!doLARVACheckin()) {
            Error("Unable to checkin");
            return Status.EXIT;
        }
        this.DFSetMyServices(new String[]{"DEATHSTAR " + userID});
        this.setTitle();
        TheMap.clear();
        return Status.IDLE;
    }

    public Status MyCheckout() {
        this.doLARVACheckout();
        return Status.EXIT;
    }

    public Status myIdle() {
        inbox = this.LARVAblockingReceive();
        Info("Received: " + ACLMessageTools.fancyWriteACLM(inbox, false));
        if (inbox.getContent().contains("filedata")) {
            this.sessionKey = inbox.getConversationId();
            Ole ocontent = new Ole().set(inbox.getContent());
            OleFile ofile = new OleFile(ocontent.getOle("surface"));
            int maxlevel = ocontent.getInt("maxflight");
            doSwingWait(() -> {
                TheMap.clear();

                TheMap.setWorldMap(ofile.toString(), maxlevel, ocontent.getField("palette"));
                this.setTitle();
            });
        } else if (inbox.getContent().contains("perceptions")) {
            doSwingWait(() -> {
                TheMap.feedPerception(inbox.getContent());
            });
        } else if (inbox.getContent().contains("goals")) {
            doSwingWait(() -> {
                TheMap.feedGoals(inbox.getContent());
            });
        }
        return Status.IDLE;
    }

    protected void setTitle() {
        this.TheMap.setTitle("| DEATH STAR |" + userName + " | " + sessionKey + " |");
    }
}
