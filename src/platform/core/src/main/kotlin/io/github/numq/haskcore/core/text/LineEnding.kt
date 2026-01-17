package io.github.numq.haskcore.core.text

enum class LineEnding {
    LF, CRLF, CR;

    val text: String
        get() = when (this) {
            LF -> "\n"

            CRLF -> "\r\n"

            CR -> "\r"
        }

    data class DetectionResult(val dominant: LineEnding, val isMixed: Boolean)

    companion object {
        fun getSystemLineEnding() = when {
            System.getProperty("os.name").contains("win", ignoreCase = true) -> CRLF

            else -> LF
        }

        fun analyze(text: String): DetectionResult {
            var lfCount = 0

            var crlfCount = 0

            var crCount = 0

            var hasLF = false

            var hasCRLF = false

            var hasCR = false

            var i = 0

            val n = text.length

            while (i < n) {
                val char = text[i]

                if (char == '\r') {
                    if (i + 1 < n && text[i + 1] == '\n') {
                        crlfCount++

                        hasCRLF = true

                        i += 2

                        continue
                    } else {
                        crCount++

                        hasCR = true
                    }
                } else if (char == '\n') {
                    lfCount++

                    hasLF = true
                }

                i++
            }

            val dominant = when {
                lfCount > 0 && lfCount >= crlfCount && lfCount >= crCount -> LF

                crlfCount > 0 && crlfCount >= crCount -> CRLF

                crCount > 0 -> CR

                else -> LF
            }

            val isMixed = listOf(hasLF, hasCRLF, hasCR).count() > 1

            return DetectionResult(dominant = dominant, isMixed = isMixed)
        }
    }
}