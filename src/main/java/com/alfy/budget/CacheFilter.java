package com.alfy.budget;

import java.io.IOException;
import org.springframework.stereotype.Component;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CacheFilter implements Filter {

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;

		if (addNoCacheHeaders(request)) {
			response.addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
			response.addHeader("Pragma", "no-cache");
			response.addHeader("Expires", "0");
		}

		filterChain.doFilter(servletRequest, servletResponse);
	}

	private boolean addNoCacheHeaders(HttpServletRequest request) {
		String requestURI = request.getRequestURI();

		if (requestURI.startsWith("/rest")) {
			return true;
		}

		if (requestURI.endsWith(".html")
		    || requestURI.endsWith(".css")
		    || requestURI.endsWith(".js")
		    || requestURI.endsWith(".ico")
		    || requestURI.endsWith(".png")
		    || requestURI.endsWith(".svg")
		) {
			return false;
		}

		return true;
	}

}
