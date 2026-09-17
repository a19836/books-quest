package com.booksquest.shared.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
public class HttpRequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Set<String> SENSITIVE_HEADERS = new HashSet<>(Arrays.asList(
            "authorization", "cookie", "set-cookie", "x-api-key", "x-auth-token", 
            "x-access-token", "x-refresh-token", "x-csrf-token", "password"
    ));

    private static final Set<String> BINARY_CONTENT_TYPES = new HashSet<>(Arrays.asList(
            "image", "video", "audio", "application/octet-stream", "application/pdf"
    ));

    private static final String[] DEFAULT_EXCLUDED_PATHS = {
            "/actuator", "/health", "/metrics", "/swagger", "/webjars"
    };

    @Value("${app.http-logging.enabled:true}")
    private boolean loggingEnabled;

    @Value("${app.http-logging.log-headers:true}")
    private boolean logHeaders;

    @Value("${app.http-logging.log-bodies:true}")
    private boolean logBodies;

    @Value("${app.http-logging.log-query-params:true}")
    private boolean logQueryParams;

    @Value("${app.http-logging.max-body-length:5000}")
    private int maxBodyLength;

    @Value("${app.http-logging.excluded-paths:/actuator/*,/health,/metrics,/swagger-ui,/v3/api-docs,/webjars}")
    private String excludedPathsConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!loggingEnabled || isExcludedPath(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!log.isDebugEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        long startTime = System.currentTimeMillis();

        ContentCachingRequestWrapper cachedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper cachedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(cachedRequest, cachedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logRequestResponse(cachedRequest, cachedResponse, duration);
            cachedResponse.copyBodyToResponse();
        }
    }

    private boolean isExcludedPath(String requestUri) {
        String[] excludedPaths = excludedPathsConfig.split(",");
        for (String excludedPath : excludedPaths) {
            excludedPath = excludedPath.trim();
            if (matchPath(requestUri, excludedPath)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchPath(String uri, String pattern) {
        if (pattern.endsWith("*")) {
            return uri.startsWith(pattern.substring(0, pattern.length() - 1));
        }
        return uri.equals(pattern);
    }

    private void logRequestResponse(ContentCachingRequestWrapper request, ContentCachingResponseWrapper response, long duration) {
        try {
            StringBuilder logMessage = new StringBuilder();
            logMessage.append("\n").append("=".repeat(100)).append("\n");
            logMessage.append("HTTP REQUEST/RESPONSE\n");
            logMessage.append("-".repeat(100)).append("\n");

            logMessage.append(String.format("REQUEST: %s %s\n", request.getMethod(), request.getRequestURI()));

            if (logQueryParams && request.getQueryString() != null) {
                logMessage.append(String.format("Query Params: %s\n", request.getQueryString()));
            }

            if (logHeaders) {
                logMessage.append("Request Headers:\n");
                Enumeration<String> headerNames = request.getHeaderNames();
                while (headerNames.hasMoreElements()) {
                    String headerName = headerNames.nextElement();
                    String headerValue = request.getHeader(headerName);
                    if (isSensitiveHeader(headerName)) {
                        logMessage.append(String.format("  %s: [REDACTED]\n", headerName));
                    } else {
                        logMessage.append(String.format("  %s: %s\n", headerName, headerValue));
                    }
                }
            }

            if (logBodies) {
                String contentType = request.getContentType();
                if (!isBinaryContent(contentType)) {
                    byte[] requestBody = request.getContentAsByteArray();
                    if (requestBody.length > 0) {
                        String bodyStr = new String(requestBody, StandardCharsets.UTF_8);
                        logMessage.append(String.format("Request Body: %s\n", truncateBody(bodyStr)));
                    }
                } else if (contentType != null) {
                    logMessage.append(String.format("Request Body: [BINARY CONTENT - %s]\n", contentType));
                }
            }

            logMessage.append("-".repeat(100)).append("\n");
            logMessage.append(String.format("RESPONSE: %d (%dms)\n", response.getStatus(), duration));

            if (logHeaders) {
                logMessage.append("Response Headers:\n");
                for (String headerName : response.getHeaderNames()) {
                    String headerValue = response.getHeader(headerName);
                    if (isSensitiveHeader(headerName)) {
                        logMessage.append(String.format("  %s: [REDACTED]\n", headerName));
                    } else {
                        logMessage.append(String.format("  %s: %s\n", headerName, headerValue));
                    }
                }
            }

            if (logBodies) {
                String contentType = response.getContentType();
                if (!isBinaryContent(contentType)) {
                    byte[] responseBody = response.getContentAsByteArray();
                    if (responseBody.length > 0) {
                        String bodyStr = new String(responseBody, StandardCharsets.UTF_8);
                        logMessage.append(String.format("Response Body: %s\n", truncateBody(bodyStr)));
                    }
                } else if (contentType != null) {
                    logMessage.append(String.format("Response Body: [BINARY CONTENT - %s]\n", contentType));
                }
            }

            logMessage.append("=".repeat(100));
            log.debug(logMessage.toString());

        } catch (Exception e) {
            log.debug("Error logging HTTP request/response", e);
        }
    }

    private boolean isSensitiveHeader(String headerName) {
        return SENSITIVE_HEADERS.contains(headerName.toLowerCase());
    }

    private boolean isBinaryContent(String contentType) {
        if (contentType == null) {
            return false;
        }
        String lowerContentType = contentType.toLowerCase();
        return BINARY_CONTENT_TYPES.stream().anyMatch(lowerContentType::contains);
    }

    private String truncateBody(String body) {
        if (body.length() <= maxBodyLength) {
            return body;
        }
        return body.substring(0, maxBodyLength) + "... [truncated]";
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        return !loggingEnabled || !log.isDebugEnabled() || isExcludedPath(request.getRequestURI());
    }
}
