package edu.infnet.melodyhub.infrastructure.observability

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.*

/**
 * Infrastructure Layer - Observability
 *
 * Filter responsible for populating MDC (Mapped Diagnostic Context) with contextual information
 * that will be included in all log entries during the request lifecycle.
 *
 * This follows DDD principles by keeping cross-cutting concerns (observability)
 * in the infrastructure layer, separate from domain and application logic.
 */
@Component
@Order(1)
class MdcFilter : OncePerRequestFilter() {

    companion object {
        const val TRACE_ID = "traceId"
        const val REQUEST_URI = "requestUri"
        const val REQUEST_METHOD = "requestMethod"
        const val USER_EMAIL = "userEmail"
        const val USER_ID = "userId"
        const val USER_ROLE = "userRole"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            // Generate unique trace ID for request tracking
            val traceId = UUID.randomUUID().toString()
            MDC.put(TRACE_ID, traceId)

            // Add request context
            MDC.put(REQUEST_URI, request.requestURI)
            MDC.put(REQUEST_METHOD, request.method)

            // Add trace ID to response headers for distributed tracing
            response.addHeader("X-Trace-Id", traceId)

            filterChain.doFilter(request, response)
        } finally {
            // Always clear MDC to prevent memory leaks in thread pool
            MDC.clear()
        }
    }
}
