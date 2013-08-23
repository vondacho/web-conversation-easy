package com.obya.common.web.conversation;

import java.util.Date;

import org.hibernate.Session;

/**
 * Conversation - Access from ConversationManager
 * timestamp - Will be refreshed when using the Conversation. A job deletes Conversations with an expired timestamp.
 * id - HTTP Session ID is used as Conversation-ID
 */
public class Conversation {
    private String id;
    private Date timestamp;
    private Session session;

    public Conversation(String id, Session session) {
        this.id = id;
        this.session = session;
        this.timestamp = new Date();
    }

    public String getId() {
        return id;
    }

    public Session getSession() {
        return session;
    }

	public void setSession(Session session) {
		this.session = session;
    }

	public Date getTimestamp() {
        return timestamp;
    }

    public void refreshTimestamp(){
        this.timestamp = new Date();
    }

}
