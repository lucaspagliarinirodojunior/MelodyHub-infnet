package edu.infnet.melodyhub.infrastructure.observability

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper

/**
 * Infrastructure Layer - Observability
 *
 * Filter responsible for logging HTTP requests and responses.
 * Captures request/response metadata and timing for observability.
 *
 * This is part of the infrastructure layer as it handles technical concerns
 * (HTTP logging) without mixing with business logic.
 */
@Component
@Order(2)
class RequestLoggingFilter : OncePerRequestFilter() {

    companion object {
        private val logger = LoggerFactory.getLogger(RequestLoggingFilter::class.java)
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Skip logging for actuator endpoints to reduce noise
        if (request.requestURI.startsWith("/actuator")) {
            filterChain.doFilter(request, response)
            return
        }

        val startTime = System.currentTimeMillis()
        val wrappedRequest = ContentCachingRequestWrapper(request)
        val wrappedResponse = ContentCachingResponseWrapper(response)

        try {
            logRequest(wrappedRequest)
            filterChain.doFilter(wrappedRequest, wrappedResponse)
        } finally {
            val duration = System.currentTimeMillis() - startTime
            logResponse(wrappedResponse, duration)
            wrappedResponse.copyBodyToResponse()
        }
    }

    private fun logRequest(request: ContentCachingRequestWrapper) {
        val queryString = request.queryString?.let { "?$it" } ?: ""
        val uri = "${request.requestURI}$queryString"

        logger.info(
            "HTTP Request: method={}, uri={}, contentType={}, remoteAddr={}",
            request.method,
            uri,
            request.contentType ?: "N/A",
            request.remoteAddr
        )
    }

    private fun logResponse(response: ContentCachingResponseWrapper, duration: Long) {
        logger.info(
            "HTTP Response: status={}, contentType={}, duration={}ms",
            response.status,
            response.contentType ?: "N/A",
            duration
        )
    }
}
