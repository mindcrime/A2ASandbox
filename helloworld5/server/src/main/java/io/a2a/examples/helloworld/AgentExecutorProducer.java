package io.a2a.examples.helloworld;

import java.io.IOException;
import java.util.List;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.retry.support.RetryTemplate;

import io.a2a.A2A;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.spec.A2AError;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;
import io.a2a.spec.UnsupportedOperationError;
import io.micrometer.observation.ObservationRegistry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class AgentExecutorProducer 
{

    @Produces
    public AgentExecutor agentExecutor() 
    {
        return new AgentExecutor() 
        {
            OpenAiApi openAiApi = OpenAiApi.builder()
                    .apiKey( System.getenv("OPENAI_API_KEY") )
                    .build();
                
            OpenAiChatOptions openAiChatOptions = OpenAiChatOptions.builder()
                    .model("gpt-4o")
                    .temperature(0.4)
                    .maxTokens(200)
                    .build();

            ToolCallingManager toolCallingManager = ToolCallingManager.builder().build();

            RetryTemplate retryTemplate = RetryTemplate.builder()
                    .maxAttempts(3)
                    .fixedBackoff(1000) // 1 second
                    .retryOn(IOException.class) // Retry on specific exceptions
                    .build();

            ObservationRegistry observationRegistry = ObservationRegistry.create();
                
            OpenAiChatModel chatModel = new OpenAiChatModel(openAiApi, openAiChatOptions, toolCallingManager, retryTemplate, observationRegistry );
        	
        	
            @Override
            public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError 
            {
            	
            	List<Part<?>> parts = context.getMessage().getParts();
            	
            	StringBuilder sb = new StringBuilder();
            	
            	for( Part<?> part : parts ) 
            	{
            		System.out.println("Received part: " + part);
            		
            		if( part instanceof TextPart ) 
					{
						System.out.println("Text content: " + ((TextPart) part).getText());
						sb.append(((TextPart) part).getText());
					}
            	}
            	
            	String receivedMessage = sb.toString();
            	
            	ChatResponse response = chatModel.call(new Prompt(receivedMessage));
            	
                eventQueue.enqueueEvent(A2A.toAgentMessage( response.getResult().getOutput().getText() ));
            }

            @Override
            public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError 
            {
                throw new UnsupportedOperationError();
            }
        };
    }
}
