package com.studytimer.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that prevents browsers from showing the Basic Auth popup by
 * stripping any WWW-Authenticate header from responses.
 */
@Component
public class NoBrowserAuthPopupFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        HttpServletResponseWrapper wrapped = new HttpServletResponseWrapper(response) {
            @Override
            public void setHeader(String name, String value) {
                if ("WWW-Authenticate".equalsIgnoreCase(name)) {
                    // ignore
                    return;
                }
                super.setHeader(name, value);
            }

            @Override
            public void addHeader(String name, String value) {
                if ("WWW-Authenticate".equalsIgnoreCase(name)) {
                    return;
                }
                super.addHeader(name, value);
            }
        };

        filterChain.doFilter(request, wrapped);
    }
}
