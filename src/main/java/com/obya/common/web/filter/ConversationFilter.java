package com.obya.common.web.filter;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.obya.common.web.conversation.ConversationManager;
import com.obya.common.web.util.WebApplicationSpringContextHolder;

public class ConversationFilter implements Filter {
	private static Logger log = LoggerFactory.getLogger(ConversationFilter.class);

	private ConversationManager conversationManager;
	private String[] initiators;
	private String[] terminators;

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) request;

		String queryString = getQueryString(httpRequest);

		log.debug("requested URL is " + queryString);

		if (match(queryString, initiators)) {
			log.debug("start a new conversation");
			conversationManager.beginConversation(httpRequest.getSession().getId());
			log.debug("execute the request");
			chain.doFilter(request, response);
			log.debug("disconnect database connection to avoid starvation");
			conversationManager.disconnectDatabase(httpRequest.getSession().getId());
		} else if (match(queryString, terminators)) {
			log.debug("execute the request");
			chain.doFilter(request, response);
			log.debug("end the current conversation");
			conversationManager.endConversation(httpRequest.getSession().getId());
		} else {
			log.debug("continue the current conversation");
			conversationManager.continueConversation(httpRequest.getSession().getId());
			log.debug("execute the request");
			chain.doFilter(request, response);
			log.debug("disconnect database connection to avoid starvation");
			conversationManager.disconnectDatabase(httpRequest.getSession().getId());
		}
	}

	public void init(FilterConfig filterConfig) throws ServletException {
		ApplicationContext ctx = WebApplicationSpringContextHolder.getApplicationContext();
		Assert.notNull(ctx, "No web application context configured");
		conversationManager = ctx.getBean("conversationManager", ConversationManager.class);
		initiators = StringUtils.delimitedListToStringArray(filterConfig.getInitParameter("initiators"), ";");
		terminators = StringUtils.delimitedListToStringArray(filterConfig.getInitParameter("terminators"), ";");
		log.info("the filter has been initialized");
	}

	private boolean match(String path, String[] candidates) {
		for (String candidate : candidates) {
			if (path.matches(candidate))
				return true;
		}
		return false;
	}

	@SuppressWarnings("rawtypes")
	private String getQueryString(HttpServletRequest request) {
		StringBuilder sb = new StringBuilder(request.getServletPath());
		
		Enumeration parameterNames = request.getParameterNames();
		boolean firstParameter = true;
		while (parameterNames.hasMoreElements()) {
			String parameterName = parameterNames.nextElement().toString();
			String[] values = request.getParameterValues(parameterName);
			if (values != null && values.length > 0) {
				for (String value : values) {
					if (org.apache.commons.lang.StringUtils.isNotEmpty(value)) {
						sb.append(firstParameter ? "?" : "&");
						sb.append(parameterName);
						sb.append("=");
						sb.append(value);

						firstParameter = false;
					}
					break;
				}
			}
		}
		return sb.toString();
	}

	public void destroy() {
		conversationManager = null;
	}
}
