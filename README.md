# Android String Resource Translator

A command-line tool that leverages the OpenAI API to automatically translate Android string resource files (`strings.xml`). Built with Kotlin and Gradle, this utility reads your base resource file, processes existing translations (if available), and generates updated translated files for one or more target languages.

## Features

- **Automated Translation**: Converts your Android string resources into multiple languages.
- **Multi-Language Support**: Specify a comma-separated list of target language tags.
- **Incremental Translation**: Reuses previous translations where applicable.
- **Customizable Settings**: Configure your API key, model, and timeout via command-line options.

## Requirements

- **JDK 17** or later
- **Gradle 8.10** (the project includes a Gradle wrapper, so a local Gradle installation is not required)
- An **OpenAI API Key** for accessing the translation service

## Usage

This application is designed to be used from the command line. The following options are available:

```bash
java -jar android-string-resource-translator.jar \
  --target-languages <language-tags> \
  --base-dir <resource-directory> \
  --api-key <your-api-key> \
  --original-language <language-tag> \
  --model <model-id> \
  --timeout <minutes> 
```

### Options

- **`--original-language`**  
  - *Description*: Specifies the language tag for the source resource file.  
  - *Default*: An empty string (which corresponds to the default `values/strings.xml` folder).

- **`--target-languages`** (required)  
  - *Description*: A comma-separated list of language tags for target resources (e.g., `ja,ko` for Japanese and Korean).

- **`--base-dir`** (required)  
  - *Description*: The base directory where your Android resource folders are located.  
  - *Example*: If your resource files are in `app/src/main/res/`, supply that path.

- **`--api-key`** (required)  
  - *Description*: Your OpenAI API key used to authenticate translation requests.

- **`--model`**  
  - *Description*: The OpenAI Model ID to use for translation.  
  - *Default*: `"o3-mini"`

- **`--timeout`**  
  - *Description*: Timeout (in minutes) for the OpenAI API requests.  
  - *Default*: `3`

### Example

Assume you have your base resource directory at `/Users/username/Projects/MyApp/res` and you want to translate your default strings to Japanese and Korean. Run:

```bash
java -jar android-string-resource-translator.jar \
  --target-languages="ja,ko" \
  --base-dir="/Users/username/Projects/MyApp/res" \
  --api-key="YOUR_OPENAI_API_KEY" \
  --model="o3-mini" 
  --timeout=3 
```

This command will:
- Read the source strings from `/Users/username/Projects/MyApp/res/values/strings.xml`
- Translate them into Japanese and Korean
- Save the translated files to `/Users/username/Projects/MyApp/res/values-ja/strings.xml` and `/Users/username/Projects/MyApp/res/values-ko/strings.xml`

## License

This project is licensed under the GNU General Public License. See the [LICENSE](LICENSE) file for details.