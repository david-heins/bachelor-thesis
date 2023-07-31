import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object Validator {
    private val mutex = Mutex()
    private val store = mutableSetOf<String>()

    var addCount = 0
        private set
    var removeCount = 0
        private set

    suspend fun validate(value: String?, validateExisting: Boolean): Boolean {
        if (value == null) return false

        return when (validateExisting) {
            false -> mutex.withLock {
                store.add(value).also { successful ->
                    if (successful) addCount++
                    else if (Config.general.debug) println("Duplicate -> $value")
                }
            }

            true -> mutex.withLock {
                store.remove(value).also { successful ->
                    if (successful) removeCount++
                    else if (Config.general.debug) println("Missing -> $value")
                }
            }
        }
    }
}
