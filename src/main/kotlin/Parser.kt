import kotlinx.serialization.Serializable
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File

@Serializable
sealed interface Content {

    @Serializable
    data class StringResource(
        val name: String,
        val value: String,
        val isTranslatable: Boolean = true,
    ) : Content {
        companion object {
            val schema = """
            {
              []"type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "name": {
                    "type": "string",
                    "description": "Unique identifier for the text resource"
                  },
                  "value": {
                    "type": "string",
                    "description": "The translated text value"
                  },
                  "isTranslatable": {
                    "type": "boolean",
                    "description": "Indicates if the text is translatable"
                  }
                },
                "required": ["name", "value", "isTranslatable"]
              }
            }
            """.trimIndent()
        }
    }

    @Serializable
    data object Whitespace : Content

    @Serializable
    data class Comment(val value: String) : Content
}

class Parser(file: File) {
    private val factory = XmlPullParserFactory.newInstance()

    private val parser = factory.newPullParser()

    init {
        parser.setInput(file.inputStream(), "UTF-8")
    }

    private fun parseElement(xpp: XmlPullParser): String = buildString {
        while (true) {
            when (xpp.nextToken()) {
                XmlPullParser.START_TAG -> {
                    // 子タグ
                    append("<${xpp.name}")
                    for (i in 0 until xpp.attributeCount) {
                        val attrName = xpp.getAttributeName(i)
                        val attrValue = xpp.getAttributeValue(i)
                        append(" $attrName=\"$attrValue\"")
                    }
                    append(">")
                    append(parseElement(xpp))
                    append("</${xpp.name}>")
                }

                XmlPullParser.TEXT -> {
                    append(xpp.text)
                }

                XmlPullParser.END_TAG,
                XmlPullParser.END_DOCUMENT -> {
                    break
                }
            }
        }
    }

    fun loadContents(): List<Content> {
        val resources = mutableListOf<Content>()
        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "string") {
                        val name = parser.getAttributeValue(null, "name").orEmpty()
                        val isTranslatable = parser.getAttributeValue(null, "translatable")?.toBoolean() ?: true
                        val value = parseElement(parser)

                        resources.add(Content.StringResource(name, value, isTranslatable))
                    }
                }

                XmlPullParser.COMMENT -> {
                    resources.add(Content.Comment(parser.text))
                }

                XmlPullParser.IGNORABLE_WHITESPACE -> {
                    resources.add(Content.Whitespace)
                }
            }

            eventType = parser.nextToken()
        }

        return resources
    }
}