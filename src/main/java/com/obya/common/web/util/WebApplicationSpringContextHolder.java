package com.obya.common.web.util;

import org.springframework.context.ApplicationContext;

public class WebApplicationSpringContextHolder {

	private static ApplicationContext applicationContext = null;

	public static boolean isConfigured() {
		return applicationContext != null;
	}

	public static ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	public static void setApplicationContext(ApplicationContext ctx) {
		applicationContext = ctx;
	}

	@SuppressWarnings("unchecked")
	public static <E> E getBean(String name, Class<E> expectedType) {
		return isConfigured() ? (E) getApplicationContext().getBean(name) : null;
	}

}
