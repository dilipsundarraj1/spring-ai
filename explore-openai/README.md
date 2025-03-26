<!-- TOC -->
* [Spring AI with OpenAI](#spring-ai-with-openai)
  * [application.yml](#applicationyml)
    * [ChatClientAutoConfiguration](#chatclientautoconfiguration)
  * [Custom ChatClient With Dynamic Options](#custom-chatclient-with-dynamic-options)
    * [Why is this needed ?](#why-is-this-needed-)
  * [System, User and Assistant Messages](#system-user-and-assistant-messages)
    * [Building User and System Messages using Prompt class](#building-user-and-system-messages-using-prompt-class)
    * [Building User and System Messages using Prompt and StringTemplate class - Approach 1](#building-user-and-system-messages-using-prompt-and-stringtemplate-class---approach-1)
    * [Building User and System Messages using Prompt class, StringTemplate - Approach 2](#building-user-and-system-messages-using-prompt-class-stringtemplate---approach-2-)
  * [Vision Functionality using ChatClient](#vision-functionality-using-chatclient)
    * [Passing image as a Resource to the chatClient - File System](#passing-image-as-a-resource-to-the-chatclient---file-system)
      * [application.yml](#applicationyml-1)
      * [Controller](#controller)
    * [Passing image as a Resource to the chatClient - MultiPart file](#passing-image-as-a-resource-to-the-chatclient---multipart-file)
      * [Controller](#controller-1)
  * [Image Functionality using OpenAiImageModel](#image-functionality-using-openaiimagemodel)
    * [application.yml](#applicationyml-2)
    * [ImageController](#imagecontroller)
  * [Audio Capabilities with OpenAI](#audio-capabilities-with-openai)
    * [Text To Speech](#text-to-speech)
    * [Transcriptions](#transcriptions)
      * [application.yml](#applicationyml-3)
      * [TranscriptionController](#transcriptioncontroller)
      * [OpenAiAudioTranscriptionOptions in Transcription](#openaiaudiotranscriptionoptions-in-transcription)
* [Advisors](#advisors)
  * [Adding Memory to the Conversation in Generative AI](#adding-memory-to-the-conversation-in-generative-ai)
    * [ChatClient with MemoryAdvisor](#chatclient-with-memoryadvisor)
    * [Conversation by Session-Id](#conversation-by-session-id)
  * [Logger Advisor](#logger-advisor)
    * [Adding Advisor to the ChatClient](#adding-advisor-to-the-chatclient)
    * [application.yml - Change the Logging](#applicationyml---change-the-logging)
    * [More About Advisors](#more-about-advisors)
  * [RAG](#rag)
    * [Reading docs using tika document reader](#reading-docs-using-tika-document-reader)
      * [Adding Dependency - build.gradle](#adding-dependency---buildgradle)
      * [Bean Configuration](#bean-configuration)
      * [Controller](#controller-2)
<!-- TOC -->


# Spring AI with OpenAI

## application.yml

- This is minimal config that's needed for interacting with OpenAI platform. 

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_KEY}
      chat:
        options:
          model: gpt-4o
```

### ChatClientAutoConfiguration

- This is the class that takes care of creating the beans needed to interact with the model.

## Custom ChatClient With Dynamic Options

- Below is the approach that we can use to dynamically supply the props to the chat client.

### Why is this needed ?

- You may need this for use-cases where you want to control or alter the behavior of the interaction with the open-ai.  

```java
    private String customChatClient(UserInput userInput) {
        var chatOptions = ChatOptionsBuilder
                .builder()
                        .withTemperature(2.0)
                                .withMaxTokens(100)
                .build();
        var chatClient1 = chatClient.mutate()
                .defaultOptions(chatOptions)
                .build();
        var requestSpec = chatClient1.prompt()
                .user(userInput.prompt());
        var responseSpec =  requestSpec.call();
        log.info("responseSpec : {} ", responseSpec);
        return  responseSpec.content();
```

## System, User and Assistant Messages


### Building User and System Messages using Prompt class

```java
        var systemMessage = new SystemMessage("""
                You are a helpful assistant, who can answer java based questions.
                For any other questions, please respond with I don't know in a funny way!
                """);

        var promptMessage = new Prompt(
                List.of(systemMessage,
                        new UserMessage(userInput.prompt())));
```

- Check the [PromptController](src/main/java/com/llm/prompt_engineering/PromptController.java)

### Building User and System Messages using Prompt and StringTemplate class - Approach 1


```java
@Value("classpath:/prompt-templates/java-coding-assistant.st")
private Resource systemTemplateMessage;

var systemMessage = new SystemMessage(systemTemplateMessage);

log.info("systemMessage : {} ", systemMessage);

var promptMessage = new Prompt(
        List.of(systemMessage,
                new UserMessage(userInput.prompt())));

```

- Check the [PromptController](src/main/java/com/llm/prompt_engineering/PromptController.java)


### Building User and System Messages using Prompt class, StringTemplate - Approach 2 

- Check the [CodingAssistantController](src/main/java/com/llm/prompt_engineering/CodingAssistantController.java)

```java
    @Value("classpath:/prompt-templates/coding-assistant.st")
    private Resource systemText;

    @PostMapping("/prompts/coding_assistant/{language}")
    public String promptsByLanguage(
            @PathVariable String language,
            @RequestBody UserInput userInput) {
        log.info("userInput : {} , language : {} ", userInput, language);

        SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemText);
        var systemMessage = systemPromptTemplate.createMessage(Map.of("language", language));

        log.info("systemMessage : {} ", systemMessage);

        var promptMessage = new Prompt(
                List.of(systemMessage,
                        new UserMessage(userInput.prompt())));

        var requestSpec = chatClient.prompt(promptMessage);

        var responseSpec = requestSpec.call();
        log.info("responseSpec : {} ", responseSpec.chatResponse());
        return responseSpec.content();
    }

```

## Vision Functionality using ChatClient

### Passing image as a Resource to the chatClient - File System

- [VisionController](src/main/java/com/llm/vision/VisionController.java)

#### application.yml

- This config is needed, if the files are going to be greater than 1 MB.

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
```

#### Controller

```java
    @PostMapping("/v1/vision")
public String vision(@RequestBody UserInput userInput) {
  log.info("userInput message : {} ", userInput);
  var imageResource = new ClassPathResource("/files/multimodal.test.png");

  var userMessage = new UserMessage(
          userInput.prompt(),
          new Media(MimeTypeUtils.IMAGE_PNG, imageResource)); // media


  var response = chatClient.prompt(new Prompt(userMessage)).call();
  log.info("response : {} ", response);
  return response.content();
}
```

### Passing image as a Resource to the chatClient - MultiPart file


#### Controller

- In this example, we read the image from the endpoint thats been passed as a multipart file.
```java
 @PostMapping(value = "/v2/vision", consumes = "multipart/form-data")
    public ResponseEntity<String> visionV2(
            @RequestParam("file") MultipartFile file,
            @RequestParam("prompt") String prompt
    ) {

        try {
            // Get the original filename
            String fileName = file.getOriginalFilename();
            log.info("Uploaded File name is :  {} " , fileName);

            // Create UserMessage
            var userMessage = new UserMessage(
                    //"Explain what do you see in this picture?", // content
                    prompt,
                    new Media(MimeTypeUtils.IMAGE_PNG, file.getResource())); // media
            var response = chatClient.prompt(new Prompt(userMessage)).call();
            log.info("response : {} ", response.chatResponse());
            return ResponseEntity.ok( response.content());
        } catch (Exception e) {
            log.error("Exception is : {} ", e.getMessage(), e);
            return ResponseEntity.status(500).body("File upload failed");
        }
    }
```

## Image Functionality using OpenAiImageModel

### application.yml

```yaml
spring:
    ai: 
        api-key: ${OPENAI_KEY}
        enabled: true
```

- enabled:
  - Manages the Autoconfiguration of **OpenAiImageModel** bean.
    - false : Disable auto configuration
    - true : Enable auto configuration

### ImageController

```java
@RestController
public class ImageController {
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    public OpenAiImageModel openAiImageModel;
    public ImageController(OpenAiImageModel openAiImageModel) {
        this.openAiImageModel = openAiImageModel;
    }


    @PostMapping("/v1/images")
    public ImageResponse chat(@RequestBody UserInput userInput){
        log.info("userInput message prompt is : {} ", userInput);
        var response = openAiImageModel.call(
                new ImagePrompt(userInput.prompt(),
                        OpenAiImageOptions.builder()
                                .withQuality("hd")
                                .withN(1)
                                .withHeight(1024)
                                .withWidth(1024).build())

        );
        log.info("response : {} ", response.getResult().getOutput());
        return  response;
    }
}
```


## Audio Capabilities with OpenAI

### Text To Speech

### Transcriptions

#### application.yml

- Below is the config for autoconfiguring the audio model for transcription.

```yaml
spring:
  ai:
    audio:
        transcription:
          enabled: true
#          api-key: ${OPENAI_KEY}
          options:
            prompt: Extract text from the file.
            response-format: JSON
```

#### TranscriptionController

- Check the [TranscriptionController](src/main/java/com/llm/audio/TranscriptionController.java) for this one.

- Using this approach we can pass the file as a resource and get the transcription.
- 
```java
    @PostMapping("/v1/transcription")
    public TranscriptionResponse transcription(
            @RequestParam("file") MultipartFile file
    ) {

        var audioFile = file.getResource();

        AudioTranscriptionPrompt transcriptionRequest = new AudioTranscriptionPrompt(audioFile);
        var response = openAiAudioTranscriptionModel.call(transcriptionRequest);
        log.info("AudioTranscription Result : {} ", response.getResult());
        return new TranscriptionResponse(response.getResult().getOutput(), file.getOriginalFilename());
    }

```

#### OpenAiAudioTranscriptionOptions in Transcription

- [response_format](https://chatgpt.com/share/672cd0c6-e4e8-8010-8914-09f768b86f7d)
  - When using the transcriptions endpoint, you can specify the format of the output using the format parameter. The available options are:
    - json - The default output, providing a structured JSON response.
    - text - Plain text format, ideal if you only need the transcribed text.
    - srt - Subtitle format, typically used for video captions.
    - verbose_json - A more detailed JSON output, including timestamps and other metadata.
    - vtt - WebVTT format, commonly used for captions and subtitles in web applications.
    - You can choose any of these formats depending on your requirements for handling the transcription output.
- [language](https://en.wikipedia.org/wiki/List_of_ISO_639_language_codes)
  - These are the languages this **API** supports.

# Advisors

## Adding Memory to the Conversation in Generative AI

- The conversation to the LLMs are always stateless.
- It does not remember the previous conversations.
- In order to achieve this we need to add memory



### ChatClient with MemoryAdvisor

```java
 public ChatMemoryController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultAdvisors(new MessageChatMemoryAdvisor(new InMemoryChatMemory()))
                .build();
    }
```
- By default the session id is **default**

### Conversation by Session-Id

- chat_memory_conversation_id
  - This property is a key value that is used by the **InMemoryChatMemory** implementation.

```java
    @PostMapping("/api/memory/chats/{session_id}")
    public String chat(@RequestBody UserInput userInput,
                       @PathVariable String session_id) {
        log.info("Input userInput : {} ", userInput);
        var requestSpec = chatClient.prompt()
                .advisors(advisor -> {
                    log.info("advisor : {}", advisor);
                    advisor.param("chat_memory_conversation_id",session_id );
                })
                .user(userInput.prompt());
        var responseSpec = requestSpec.call();
        log.info("responseSpec : {} ", responseSpec);
        return responseSpec.content();
    }
```

- Check **Chat with Memory** section in the [curl-commands](src/main/resources/curl-commands.txt) file for commands to test this.

## Logger Advisor

- This advisor is specifically used to log the request and response with the LLM.

### Adding Advisor to the ChatClient

```java
    var responseSpec = chatClient
        .prompt()
        .advisors(new SimpleLoggerAdvisor(ModelOptionsUtils::toJsonString, ModelOptionsUtils::toJsonString))
        .user(userInput.prompt())
        .system(systemMessage)
        .call();
```
### application.yml - Change the Logging

- This enables the logging to be in debug mode.

```yml
logging:
  level:
    org:
      springframework:
        ai.chat.client.advisor.SimpleLoggerAdvisor: DEBUG
```

### More About Advisors

- [Spring AI Advisors
  ](https://spring.io/blog/2024/10/02/supercharging-your-ai-applications-with-spring-ai-advisors)

## RAG

### Reading docs using tika document reader

#### Adding Dependency - build.gradle

- Add the below dependency.
- 
```groovy
    implementation("org.springframework.ai:spring-ai-tika-document-reader")
```

#### Bean Configuration

```java
@Bean
    ApplicationRunner go(@Qualifier("tikaSimpleVectorStore") VectorStore vectorStore){
        return args ->  {
            log.info("Read Docs using TikaDocumentReader");
            var documents= new TikaDocumentReader(fablieticsFaq).get();
            var fabFabDoc = documents.getFirst();
            var splitDocs = new TokenTextSplitter().split(fabFabDoc);
            log.info("splitDocs size: {} ", splitDocs.size());
            vectorStore.add(splitDocs);
        };
    }
```

#### Controller
