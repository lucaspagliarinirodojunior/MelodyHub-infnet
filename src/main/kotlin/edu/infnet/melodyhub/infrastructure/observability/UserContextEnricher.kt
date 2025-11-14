package edu.infnet.melodyhub.infrastructure.observability

import org.slf4j.MDC
import org.springframework.stereotype.Component

/**
 * Infrastructure Layer - Observability
 *
 * Utility component to enrich MDC with user context information.
 * This allows tracking user actions across all log entries.
 *
 * Following DDD principles, this component provides infrastructure support
 * for observability without coupling to domain logic.
 */
@Component
class UserContextEnricher {

    /**
     * Adds user context to MDC for the current request/operation.
     * This information will be automatically included in all subsequent logs.
     */
    fun enrichWithUserContext(userId: String?, userEmail: String?, userRole: String?) {
        userId?.let { MDC.put(MdcFilter.USER_ID, it) }
        userEmail?.let { MDC.put(MdcFilter.USER_EMAIL, it) }
        userRole?.let { MDC.put(MdcFilter.USER_ROLE, it) }
    }

    /**
     * Adds transaction context to MDC for tracking financial operations.
     */
    fun enrichWithTransactionContext(transactionId: String) {
        MDC.put("transactionId", transactionId)
    }

    /**
     * Adds music context to MDC for tracking music-related operations.
     */
    fun enrichWithMusicContext(musicId: String) {
        MDC.put("musicId", musicId)
    }

    /**
     * Adds playlist context to MDC for tracking playlist operations.
     */
    fun enrichWithPlaylistContext(playlistId: String) {
        MDC.put("playlistId", playlistId)
    }

    /**
     * Adds domain event context to MDC for tracking business events.
     */
    fun enrichWithEventContext(eventType: String) {
        MDC.put("eventType", eventType)
    }

    /**
     * Clears specific context from MDC.
     * Use when context is no longer relevant to avoid polluting logs.
     */
    fun clearContext(vararg keys: String) {
        keys.forEach { MDC.remove(it) }
    }
}
