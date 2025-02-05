
import com.aallam.openai.api.chat.*
import com.aallam.openai.api.core.Parameters
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*
import java.io.File
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class Translator: CliktCommand() {
    private val originalLanguage by option(help = "Language tag for source resources.").default("")
    private val targetLanguages by option(help = "Language tags for target resources.").required()
    private val baseDir by option(help = "Directory to generate base resources").required()

    private val apiKey by option(help = "OpenAI API Key").required()
    private val model by option(help = "OpenAI Model ID").default("o3-mini")

    private val timeout by option(help = "Timeout for OpenAI API (min)").int().default(3)

    private val formatter = Json {
        ignoreUnknownKeys = true
    }

    override fun run() {
        println("Original Launguage: \"$originalLanguage\"")
        println("Target Languages: $targetLanguages")

        val originalContents = loadFile(originalLanguage, true)
            .filterIsInstance<Content.StringResource>()
            .filter { it.isTranslatable }

        println("Original resources loaded: ${originalContents.size}")

        val client = OpenAI(
            token = apiKey,
            timeout = Timeout(timeout.minutes, timeout.minutes, timeout.minutes),
            logging = LoggingConfig(LogLevel.None),
        )

        for (targetLanguage in targetLanguages.split(",").map { it.trim() }) {
            print("Translating to \"$targetLanguage\"... ")
            val targetContents = loadFile(targetLanguage)
                .filterIsInstance<Content.StringResource>()
                .filter { it.isTranslatable }

            val translatedContents = runBlocking {
                translate(
                    client = client,
                    original = originalContents,
                    current = targetContents,
                    targetLanguage = targetLanguage,
                )
            }

            saveFile(targetLanguage, generateStringsXml(translatedContents))
            println("Done")
        }

        println("Completed.")
        exitProcess(0)
    }

    private fun loadFile(language: String, requireExist: Boolean = false): List<Content> {
        val filePath = "$baseDir/values${if (language.isEmpty()) "" else "-$language"}/strings.xml"
        val file = File(filePath)

        if (!file.exists()) {
            if (requireExist) {
                error("File not found: $filePath")
            } else {
                file.parentFile.mkdirs()
                file.createNewFile()
            }
        }

        return Parser(file).loadContents()
    }

    private fun saveFile(language: String, contents: String) {
        val filePath = "$baseDir/values${if (language.isEmpty()) "" else "-$language"}/strings.xml"
        val file = File(filePath)

        require(file.exists()) { "File not found: $filePath" }

        file.writeText(contents)
    }

    private suspend fun translate(
        client: OpenAI,
        original: List<Content.StringResource>,
        current: List<Content.StringResource>,
        targetLanguage: String
    ): List<Content.StringResource> {
        val originalJson = formatter.encodeToString(ListSerializer(Content.StringResource.serializer()), original)
        val currentJson = formatter.encodeToString(ListSerializer(Content.StringResource.serializer()), current)

        val translatePrompt = """
            You are an API that translates JSON to $targetLanguage.
            All responses must be in JSON.
        """.trimIndent()

        val userPrompt = """
            Source JSON:
            ```
            $originalJson
            ```

            Also, place the JSON you have previously translated for reference only.
            Use your previous translation as is, unless there are significant differences from the original.
            ```
            $currentJson
            ```
        """.trimIndent()

        val params = Parameters.buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("results") {
                    put("type", "array")
                    put("description", "A list of results.")
                    putJsonObject("items") {
                        put("type", "object")
                        putJsonObject("properties") {
                            putJsonObject("name") {
                                put("type", "string")
                                put("description", "The name of the result.")
                            }
                            putJsonObject("value") {
                                put("type", "string")
                                put("description", "The value associated with the result.")
                            }
                            putJsonObject("isTranslatable") {
                                put("type", "boolean")
                                put("description", "Indicates if the result is translatable.")
                            }
                        }
                        putJsonArray("required") {
                            add("name")
                            add("value")
                            add("isTranslatable")
                        }
                        put("additionalProperties", false)
                    }
                }
            }
            putJsonArray("required") {
                add("results")
            }
        }

        val request = ChatCompletionRequest(
            model = ModelId(model),
            messages = listOf(
                ChatMessage.System(translatePrompt),
                ChatMessage.User(userPrompt),
            ),
            tools = listOf(
                Tool.function(
                    name = "translatedResult",
                    description = "The value is translated and returned with the same type structure as the source JSON.",
                    parameters = params,
                )
            ),
            toolChoice = ToolChoice.Auto,
        )

        val completion = client.chatCompletion(request)
        val results = completion.choices.first().message.toolCalls?.mapNotNull {
            (it as ToolCall.Function).getResults()
        }

        return results?.firstOrNull() ?: emptyList()
    }

    private fun ToolCall.Function.getResults(): List<Content.StringResource>? {
        return runCatching {
            val json = function.argumentsAsJson()
            val results = formatter.decodeFromJsonElement<TranslationResponse>(json)

            results.results
        }.onFailure {
            println("Error: ${it.message}")
        }.getOrNull()
    }

    @Serializable
    private data class TranslationResponse(
        val results: List<Content.StringResource>,
    )
}
