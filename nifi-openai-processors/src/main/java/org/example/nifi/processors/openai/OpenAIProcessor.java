package org.example.nifi.processors.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.SupportsBatching;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;

import java.io.*;
import java.util.*;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;

@SupportsBatching
@InputRequirement(InputRequirement.Requirement.INPUT_REQUIRED)
@Tags({"OpenAI", "GPT", "ChatGPT", "AI", "text", "generate"})
@CapabilityDescription("Sends FlowFile content as input to OpenAI API and returns the generated response.")
public class OpenAIProcessor extends AbstractProcessor {

    public static final PropertyDescriptor API_KEY = new PropertyDescriptor.Builder()
        .name("OpenAI API Key")
        .description("Your OpenAI API key.")
        .required(true)
        .sensitive(true)
        .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
        .build();

    public static final PropertyDescriptor MODEL = new PropertyDescriptor.Builder()
        .name("Model")
        .description("The OpenAI model to use, e.g., gpt-3.5-turbo")
        .required(true)
        .defaultValue("gpt-3.5-turbo")
        .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
        .build();

    public static final Relationship REL_SUCCESS = new Relationship.Builder()
        .name("success")
        .description("Successful responses from OpenAI.")
        .build();

    public static final Relationship REL_FAILURE = new Relationship.Builder()
        .name("failure")
        .description("Failed requests or processing errors.")
        .build();

    private List<PropertyDescriptor> descriptors;
    private Set<Relationship> relationships;
    private OkHttpClient httpClient;
    private ObjectMapper objectMapper;

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> descriptors = new ArrayList<>();
        descriptors.add(API_KEY);
        descriptors.add(MODEL);
        this.descriptors = Collections.unmodifiableList(descriptors);

        final Set<Relationship> relationships = new HashSet<>();
        relationships.add(REL_SUCCESS);
        relationships.add(REL_FAILURE);
        this.relationships = Collections.unmodifiableSet(relationships);

        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Set<Relationship> getRelationships() {
        return relationships;
    }

    @Override
    public boolean isStateful(ProcessContext context) {
        return super.isStateful(context);
    }

    @Override
    public List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {
        // Nothing needed here for now
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        final ComponentLog logger = getLogger();
        FlowFile flowFile = session.get();
        if (flowFile == null) return;

        final String apiKey = context.getProperty(API_KEY).getValue();
        final String model = context.getProperty(MODEL).getValue();

        try {
            // Read input content
            final StringBuilder inputText = new StringBuilder();
            session.read(flowFile, in -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        inputText.append(line).append("\n");
                    }
                }
            });

            // Prepare OpenAI request
            String requestBodyJson = objectMapper.writeValueAsString(Map.of(
                "model", model,
                "messages", List.of(
                    Map.of("role", "user", "content", inputText.toString())
                )
            ));

            Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(requestBodyJson, MediaType.parse("application/json")))
                .build();

            Response response = httpClient.newCall(request).execute();

            if (!response.isSuccessful()) {
                logger.error("OpenAI request failed: {}", new Object[]{response.message()});
                session.transfer(flowFile, REL_FAILURE);
                return;
            }

            String responseBody = response.body().string();
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            String replyText = jsonResponse
                .path("choices").get(0)
                .path("message")
                .path("content")
                .asText();

            // Replace FlowFile content with the response
            FlowFile updatedFlowFile = session.write(flowFile, out -> {
                out.write(replyText.getBytes());
            });

            session.transfer(updatedFlowFile, REL_SUCCESS);

        } catch (Exception e) {
            logger.error("Failed to process flow file due to {}", new Object[]{e.getMessage()}, e);
            session.transfer(flowFile, REL_FAILURE);
        }
    }


}
