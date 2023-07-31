import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

const val rxGroupRest = "rest"

private val rxOptions = setOf(RegexOption.IGNORE_CASE)
private val rxIsoDate = Regex("""\d{1,4}-\d{1,2}-\d{1,2}""", rxOptions)
private val rxIsoTime = Regex("""T?\d{1,2}:\d{1,2}:\d{1,2}(?<${rxGroupRest}>\.\d{1,3})?Z?""", rxOptions)
private val rxIsoDateTime = Regex("${rxIsoDate.pattern}${rxIsoTime.pattern}", rxOptions)

val getIsoDateTimeNow = {
    Instant
        .now()
        .truncatedTo(ChronoUnit.MILLIS)
        .atOffset(ZoneOffset.UTC)
        .format(DateTimeFormatter.ISO_DATE_TIME)!!
}

data class IsoDateTime internal constructor(
    val date: IsoDate,
    val time: IsoTime
) {
    override fun toString(): String = "${date}T${time}Z"

    override fun equals(other: Any?): Boolean {
        return if (other !is IsoDateTime) false
        else date == other.date && time == other.time
    }

    override fun hashCode(): Int {
        val prime = 31
        var result = date.hashCode()
        result = prime * result + time.hashCode()
        return result
    }

    companion object {
        fun parse(string: String): IsoDateTime? {
            rxIsoDateTime.matchEntire(string) ?: return null
            val (dateString, timeString) = string
                .split('Z', ignoreCase = true)
                .first()
                .split('T', ignoreCase = true)
            val date = IsoDate.parse(dateString) ?: return null
            val time = IsoTime.parse(timeString) ?: return null
            return IsoDateTime(date, time)
        }
    }

    data class IsoDate internal constructor(
        val year: Int,
        val month: Int,
        val day: Int
    ) {
        override fun toString(): String {
            val yyyy = year.coerceIn((0..9999)).toString().padStart(4, '0')
            val mm = month.coerceIn((1..12)).toString().padStart(2, '0')
            val dd = day.coerceIn((1..31)).toString().padStart(2, '0')
            return "${yyyy}-${mm}-${dd}"
        }

        override fun equals(other: Any?): Boolean {
            return if (other !is IsoDate) false
            else year == other.year && month == other.month && day == other.day
        }

        override fun hashCode(): Int {
            val prime = 31
            var result = year.hashCode()
            result = prime * result + month.hashCode()
            result = prime * result + day.hashCode()
            return result
        }

        companion object {
            fun parse(string: String): IsoDate? {
                rxIsoDate.matchEntire(string) ?: return null
                val (year, month, day) = string
                    .split('Z', ignoreCase = true)
                    .first()
                    .split('T', ignoreCase = true)
                    .first()
                    .split('-', ignoreCase = true)
                return IsoDate(
                    year.toInt(),
                    month.toInt(),
                    day.toInt(),
                )
            }
        }
    }

    data class IsoTime internal constructor(
        val hour: Int,
        val minute: Int,
        val second: Int,
        val rest: String
    ) {
        override fun toString(): String {
            val hh = hour.coerceIn((0..23)).toString().padStart(2, '0')
            val mm = minute.coerceIn((0..59)).toString().padStart(2, '0')
            val ss = second.coerceIn((0..59)).toString().padStart(2, '0')
            val rrr = ".${rest.take(3)}".trimEnd('0').takeIf { it != "." } ?: ""
            return "${hh}:${mm}:${ss}${rrr}"
        }

        override fun equals(other: Any?): Boolean {
            return if (other !is IsoTime) false
            else hour == other.hour && minute == other.minute && second == other.second
        }

        override fun hashCode(): Int {
            val prime = 31
            var result = hour.hashCode()
            result = prime * result + minute.hashCode()
            result = prime * result + second.hashCode()
            return result
        }

        companion object {
            fun parse(string: String): IsoTime? {
                val match = rxIsoTime.matchEntire(string) ?: return null
                val (hour, minute, second, rest) = string
                    .let {
                        return@let if (match.groups[rxGroupRest]?.value != null) it
                        else it.trimEnd('z', 'Z') + ".0Z"
                    }
                    .split('Z', ignoreCase = true)
                    .first()
                    .split('T', ignoreCase = true)
                    .last()
                    .split(':', '.', ignoreCase = true)
                return IsoTime(
                    hour.toInt().coerceIn((0..23)),
                    minute.toInt().coerceIn((0..59)),
                    second.toInt().coerceIn((0..59)),
                    rest
                )
            }
        }
    }
}
