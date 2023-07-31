import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

val CPU: Int = Runtime.getRuntime().availableProcessors()
val OS: String = System.getProperty("os.name", "")

val outerLoopDelayMs = { 3L * Random.nextLong(22L, 34L) /* stay between 66 and 99 */ }
val innerLoopDelayMs = { 2L * Random.nextLong(16L, 33L) /* stay between 32 and 64 */ }

suspend fun <T> retryGeneratorUntilConditionOrNull(
    timeout: Duration,
    generatorLambda: suspend () -> T?,
    conditionLambda: suspend (T?) -> Boolean,
): T? = withTimeoutOrNull(timeout) {
    var firstIter = true
    var result: T?
    do {
        if (firstIter) firstIter = false
        else delay(50.milliseconds)
        result = generatorLambda()
    } while (!conditionLambda(result))
    result
}

fun calculateJobAmount(index: Int): Int {
    val perJobAmount = Config.p2p.amount / CPU
    val remainingAmount = Config.p2p.amount % CPU
    return perJobAmount + if (index == 0) remainingAmount else 0
}

fun <T> throwConfigMiddleware(expected: T, provided: Middleware?): Nothing {
    throw IllegalArgumentException("Invalid value in config.middleware: Expected \"${expected}\" but got \"${provided}\"")
}

fun <T> throwConfigMiddleware(expected: Array<T>, provided: Middleware?): Nothing {
    throwConfigMiddleware(expected.asList(), provided)
}

fun <T> throwConfigPattern(expected: T, provided: Pattern?): Nothing {
    throw IllegalArgumentException("Invalid value in config.pattern: Expected \"${expected}\" but got \"${provided}\"")
}

fun <T> throwConfigPattern(expected: Array<T>, provided: Pattern?): Nothing {
    throwConfigPattern(expected.asList(), provided)
}
