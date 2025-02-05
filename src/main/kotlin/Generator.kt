fun generateStringsXml(contents: List<Content>) = buildString {
    appendLine("""<?xml version="1.0" encoding="utf-8"?>""")
    appendLine("<resources>")

    contents.forEach { content ->
        when (content) {
            is Content.StringResource -> {
                append("    <string name=\"${content.name}\">")
                append(content.value)
                append("</string>")
                append("\n")
            }

            is Content.Comment -> {
                appendLine("    <!--${content.value}-->")
            }

            is Content.Whitespace -> {
                appendLine("\n")
            }
        }
    }

    appendLine("</resources>")
}