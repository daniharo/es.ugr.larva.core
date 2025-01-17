/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools || Templates
 * and open the template in the editor.
 */
package messaging;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import static disk.Logger.trimString;
import jade.core.AID;
import jade.lang.acl.ACLMessage;
import java.util.Iterator;
import swing.LARVADash;

/**
 *
 * @author lcv
 */
public class ACLMessageTools {

    public static enum ACLMEncoding {
        NONE, JSON
    }

    public static final String _NULLVAL = "";

    public static String getAllReceivers(ACLMessage msg) {
        String res = "";
        for (Iterator iterator = msg.getAllReceiver();
                iterator.hasNext();) {
            AID r = (AID) iterator.next();
            res += r.getLocalName() + ",";
        }

        return res;
    }

    public static JsonObject getJsonContentACLM(ACLMessage m) {
        JsonObject res = new JsonObject();
        if (isJsonACLM(m)) {
            res = Json.parse(m.getContent()).asObject();
        }
        return res;
    }

    public static boolean isJsonString(String s) {
        boolean res = false;

        if (s == null) {
            return false;
        }
        try {
            res = s.length() > 0 && s.charAt(0) == '{' && Json.parse(s) != null;
        } catch (Exception ex) {
            System.err.println("isJsonString() " + ex.toString() + " " + s);
        }
        return res;

    }

    public static boolean isJsonACLM(ACLMessage m) {
        boolean res = false;

        if (m == null || m.getContent() == null) {
            return false;
        }
        return isJsonString(m.getContent());
    }

    public static JsonObject toJsonACLM(ACLMessage msg) {
        JsonObject res = null;
        String receivers = getAllReceivers(msg);

//        ArrayList <AID> receivers = msg.getAllReceiver()
//        for (Iterator iterator = msg.getAllReceiver();
//                iterator.hasNext();) {
//            AID r = (AID) iterator.next();
//            receivers += r.getLocalName() + " ";
//        }
        res = new JsonObject().add(
                "performative", ACLMessage.getPerformative(msg.getPerformative())).add(
                "sender", (msg.getSender() == null ? _NULLVAL : msg.getSender().getLocalName())).add(
                "receiver", (!msg.getAllReceiver().hasNext() ? _NULLVAL : receivers)).add(
                "protocol", (msg.getProtocol() == null ? _NULLVAL : msg.getProtocol())).add(
                "encoding", (msg.getLanguage() == null ? _NULLVAL : msg.getLanguage())).add(
                //                "encoding", (msg.getLanguage() == null ? _NULLVAL : msg.getLanguage().substring(0, 5)+"...")).add(
                "conversation", (msg.getConversationId() == null ? _NULLVAL : msg.getConversationId())).add(
                "in-reply-to", (msg.getInReplyTo() == null ? _NULLVAL : msg.getInReplyTo())).add(
                "reply-with", (msg.getReplyWith() == null ? _NULLVAL : msg.getReplyWith()));
        if (isJsonACLM(msg)) {
            res.add("content", (msg.getContent() == null ? new JsonObject() : Json.parse(msg.getContent()).asObject()));
        } else {
            res.add("content", msg.getContent());
        }
        return res;
    }

    public static String fancyWriteACLM(ACLMessage original, boolean simple) {
        String res = "", sep = "|";
        ACLMessage msg = (ACLMessage) original.clone();
        res += (msg.getSender() == null ? _NULLVAL : "||SND" + sep + msg.getSender().getLocalName());
        Iterator it;
        it = msg.getAllReceiver();
        res += "||RCV" + sep;
        while (it.hasNext()) {
            res += ((AID) it.next()).getLocalName() + " ";
        }
        res = res + "||CNT" + sep + (isJsonACLM(msg) ? trimString(msg.getContent(), 255) : msg.getContent()) + "||";
        if (!simple) {
            it = msg.getAllReplyTo();
            res = "||PFM" + sep + ACLMessage.getPerformative(msg.getPerformative()) + res;
            res += "RPT" + sep;
            while (it.hasNext()) {
                res += ((AID) it.next()).getLocalName() + " ";
            }
            res += (msg.getProtocol() == null ? _NULLVAL : "||PRT" + sep + msg.getProtocol())
                    + (msg.getConversationId() == null ? _NULLVAL : "||CNV" + sep + msg.getConversationId())
                    + (msg.getEncoding() == null ? _NULLVAL : "||ENC" + sep + msg.getEncoding())
                    + (msg.getReplyWith() == null ? _NULLVAL : "||RPW" + sep + msg.getReplyWith())
                    + (msg.getInReplyTo() == null ? _NULLVAL : "||IRT" + sep + msg.getInReplyTo())
                    + (msg.getLanguage() == null ? _NULLVAL : "||LAN" + sep + trimString(msg.getLanguage(), 10))
                    + (msg.getOntology() == null ? _NULLVAL : "||ONT" + sep + msg.getOntology());
            res += "||";
        }
        return res;
    }

    public static String fancyWriteACLM(ACLMessage original) {
        return fancyWriteACLM(original, false);
    }

    public static String getDetailsLARVA(ACLMessage msg) {
        String res = "";
        if (msg != null && msg.getContent() != null) {
            if (isJsonACLM(msg)) {
                res = getJsonContentACLM(msg).getString("details", msg.getContent());
            } else {
                res = msg.getContent();
            }
        }
        return res;
    }

    public static ACLMessage respondTo(ACLMessage incoming) {
        ACLMessage outgoing = new ACLMessage();
        outgoing.addReceiver(incoming.getSender());
        outgoing.setSender(((AID) incoming.getAllReceiver().next()));
        outgoing.setContent("");
        if (incoming.getConversationId() != null) {
            outgoing.setConversationId(incoming.getConversationId());
        }
        if (incoming.getReplyWith() != null) {
            outgoing.setInReplyTo(incoming.getReplyWith());
        }
        if (incoming.getProtocol() != null) {
            outgoing.setProtocol(incoming.getProtocol());
        }
        return outgoing;
    }

    public static boolean isAnswerTo(ACLMessage incoming, ACLMessage sent) {
        try {
            return incoming.getConversationId().equals(sent.getConversationId())
                    && incoming.getProtocol().equals(sent.getProtocol())
                    && incoming.getInReplyTo().equals(sent.getReplyWith());
        } catch (Exception Ex) {
            return false;
        }
    }

    public static boolean isBadRecipient(ACLMessage msg) {
        String c = msg.getContent().toUpperCase();
        return (c.contains("MTS-ERROR") && c.contains("FAILED TO FIND AGENT"));
    }

    public static ACLMessage secureACLM(ACLMessage incoming) {
        incoming.setContent((incoming.getContent() == null ? _NULLVAL : incoming.getContent()));
        incoming.setLanguage(incoming.getLanguage() == null ? _NULLVAL : incoming.getLanguage());
        incoming.setConversationId((incoming.getConversationId() == null ? _NULLVAL : incoming.getConversationId()));
        incoming.setOntology((incoming.getOntology() == null ? _NULLVAL : incoming.getOntology()));
        incoming.setReplyWith((incoming.getReplyWith() == null ? _NULLVAL : incoming.getReplyWith()));
        incoming.setInReplyTo((incoming.getInReplyTo() == null ? _NULLVAL : incoming.getInReplyTo()));
        incoming.setProtocol((incoming.getProtocol() == null ? _NULLVAL : incoming.getProtocol()));
        return incoming;
    }

    public static boolean isDashACL(ACLMessage msg) {
        return msg.getReplyWith() != null && msg.getReplyWith().contains(LARVADash.MARK);
    }

    public static ACLMessage cleanDashMark(ACLMessage msg) {
        msg.setReplyWith(msg.getReplyWith().replace(LARVADash.MARK, ""));
        return msg;
    }

    public static ACLMessage addDashMark(ACLMessage msg) {
        if (msg.getReplyWith() == null) {
            msg.setReplyWith("");
        }
        msg.setReplyWith(msg.getReplyWith() + " " + LARVADash.MARK);
        return msg;
    }

    public static AID getMainReceiver(ACLMessage msg) {
        Iterator it;
        it = msg.getAllReceiver();
        if (it.hasNext()) {
            return ((AID) it.next());
        } else {
            return null;
        }
    }

}
