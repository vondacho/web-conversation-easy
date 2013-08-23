package com.obya.common.web.listener;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.obya.common.web.util.WebApplicationSpringContextHolder;

/**
 * Propagates the web application context by injecting it into the WebApplicationSpringContextHolder.
 */
public class WebApplicationContextPropagator implements ServletContextListener {
	private static Logger logger = LoggerFactory.getLogger(WebApplicationContextPropagator.class);

	public void contextInitialized(ServletContextEvent event) {
		ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(event.getServletContext());
		if (ctx != null) {
			logger.info("Register the web application context into the web application context holder");
			WebApplicationSpringContextHolder.setApplicationContext(ctx);
		} else {
			logger.error("No web application context configured");
		}
	}

	public void contextDestroyed(ServletContextEvent event) {
		WebApplicationSpringContextHolder.setApplicationContext(null);
	}
}