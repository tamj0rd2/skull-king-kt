
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.EvaluatorFilter
import ch.qos.logback.core.spi.FilterReply

class LogFilter : EvaluatorFilter<ILoggingEvent>() {
    private val allowedTerms = listOf("wsHandler", "httpHandler", "wsClient", "TEST")

    override fun decide(event: ILoggingEvent): FilterReply {
        if (event.level.isGreaterOrEqual(Level.WARN)) return FilterReply.NEUTRAL

        return if (allowedTerms.any { event.loggerName.contains(it) }) FilterReply.ACCEPT else FilterReply.DENY
    }
}