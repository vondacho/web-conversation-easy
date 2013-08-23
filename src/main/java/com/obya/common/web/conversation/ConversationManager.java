package com.obya.common.web.conversation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import org.springframework.orm.hibernate3.SessionHolder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 
 */
public class ConversationManager {
	static final Logger log = LoggerFactory.getLogger(ConversationManager.class);

	private SessionFactory sessionFactory;
	
	static final long DEFAULT_TIMEOUT_MILLISECONDS = 30*60*1000;
	
	private long timeoutMilliseconds = DEFAULT_TIMEOUT_MILLISECONDS;
	
	// Register for the active conversations. One conversation for one web session
	private Map<String, Conversation> conversations;
	
	private List<String> recycledConversations;

    public ConversationManager(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        conversations = new ConcurrentHashMap<String, Conversation>();
        recycledConversations = Collections.synchronizedList(new ArrayList<String>());
    }

    public void setTimeoutMilliseconds(long timeoutMilliseconds) {
		this.timeoutMilliseconds = timeoutMilliseconds;
	}

	/**
     * Check if one conversation is active for the web session identified by the given identifier
     */
    public boolean hasConversation(String httpSessionId) {
		return conversations.containsKey(httpSessionId);
	}

    /**
     * Initialize a new conversation and associate it to the web session identified by the given identifier
     * End any conversation associated to the web session before
     */
    public Conversation beginConversation(String httpSessionId) {
		endConversation(httpSessionId);
		
		log.debug("Begin conversation for session:" + httpSessionId);
		// Spring facilities
		// to open a new extended persistence context
		Session session = SessionFactoryUtils.getSession(sessionFactory, true);
		session.setFlushMode(FlushMode.COMMIT);
		// Spring transaction management
		// to synchronize the session with the current thread
		// so that getCurrentSession() calls work inside DAOs
		if (!TransactionSynchronizationManager.hasResource(sessionFactory))
			TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session));
		// to register the started conversation and associate it with the web session
		Conversation conversation = new Conversation(httpSessionId, session);
		conversations.put(conversation.getId(), conversation);	
		return conversation;
	}
    
    /**
     * To release the JDBC resources to prevent starvation
     */
    public void disconnectDatabase(String httpSessionId) {
		Conversation conversation = conversations.get(httpSessionId);
		if (conversation != null) {
			//TODO to test and maybe improve
			conversation.getSession().disconnect();
		}
    }

    /**
     * End any conversation associated to the web session identified by the given identifier
     */
	public void endConversation(String httpSessionId) {
		log.debug("End conversation for session:" + httpSessionId);
		Conversation conversation = conversations.get(httpSessionId);
		if (conversation != null) {
			log.debug("Conversation found with timestamp:" + conversation.getTimestamp());
			endConversation(conversation);
		}
	}
	
	private void endConversation(Conversation conversation) {
		// Spring transaction management
		// to desynchronize the session from the current thread
		if (TransactionSynchronizationManager.hasResource(sessionFactory))
			TransactionSynchronizationManager.unbindResource(sessionFactory);
		// to empty and close the extended persistence context
		conversation.getSession().clear();
		// Spring facilities
		SessionFactoryUtils.releaseSession(conversation.getSession(), sessionFactory);
		// no more conversation associated to the web session
		conversations.remove(conversation.getId());
	}

    /**
     * Continue any conversation associated to the web session identified by the given identifier
     * Care that the resources are correctly initialized in order to handle the next request-response
     */
	public void continueConversation(String httpSessionId) {
		log.debug("Continue conversation for session:" + httpSessionId);
		Conversation conversation = conversations.get(httpSessionId);
        if(conversation != null) {
			log.debug("Conversation found with timestamp:" + conversation.getTimestamp());
	        conversation.refreshTimestamp();

			// check if session is still opened
			if (!conversation.getSession().isOpen()) {
				log.debug("Session has been closed during a conversion and will be reopened");

				Session session = SessionFactoryUtils.getSession(sessionFactory, true);
				session.setFlushMode(FlushMode.COMMIT);
				conversation.setSession(session);

			}
			// Spring transaction management
	        // since the conversation may continue inside an other thread...
	        // to restore the synchronization of the session with the current thread
	        // so that getCurrentSession() calls work inside DAOs
			if (!TransactionSynchronizationManager.hasResource(sessionFactory))
				TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(conversation.getSession()));
        }
        else if (hasBeenRecycled(httpSessionId)) {
        	throw new ConversationTimeoutException();
        }
    }
	
	private boolean hasBeenRecycled(String httpSessionId) {
		return recycledConversations.contains(httpSessionId);
	}

	/**
	 * Asynchronously end the conversations which are not or no more active since a long time
	 * The period is every 5 minutes
	 */
	@Scheduled(fixedDelay = 300000)
	void recycleConversations() {
		log.debug("Recycling of conversations");
		for (Map.Entry<String, Conversation> conversation : conversations.entrySet()) {
			Date now = new Date();
			if (now.getTime() - conversation.getValue().getTimestamp().getTime() > timeoutMilliseconds) {
				log.debug("Recyclable conversation found with timestamp:" + conversation.getValue().getTimestamp());
				endConversation(conversation.getValue());
				recycledConversations.add(conversation.getKey());
			}
		}
	}
}
