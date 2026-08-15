package com.infoworks.mcp.webapp.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

@Component
public class RouteExecutionLogger extends GenericFilterBean {

    private static final Logger LOG = LoggerFactory.getLogger(RouteExecutionLogger.class);

    @Override
    public void doFilter(ServletRequest servletRequest
            , ServletResponse servletResponse
            , FilterChain filterChain) throws IOException, ServletException {
        //Elapsed time between request and response:
        final StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        filterChain.doFilter(servletRequest, servletResponse);
        stopWatch.stop();
        //Get the info from request & response:
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        HttpServletResponse res = (HttpServletResponse) servletResponse;
        LOG.info("{}:{} took {}ms and returned {}"
                , req.getMethod(), req.getRequestURI(), stopWatch.getTotalTimeMillis(), res.getStatus());
    }
}
