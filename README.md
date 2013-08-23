This Java stuf provides an implementation of the web conversation pattern using an extended persistence context.

Detail of the implementation
- It uses Spring Transaction Management.
- It uses Hibernate Session and SessionFactory
- It limits the number of conversations by web session to only one.
- The boudaries of the conversation are configured inside web.xml as URL patterns.
- To avoid starvation the connection to the database is disconnected at the end of a web request and reconnected at the beginning of each web request by the ConversationFilter class.
- The abandoned conversations are garbage collected by an asynchronous task defined in the ConversationManager class.

Advantages
- The use of an extended persistence context against one session by request pattern does not require any reattachement of detached entities.

Desadvantages
- Maybe more than one conversation by web session required
- Memory consum must be under control
- Abandoned conversations must be under control
