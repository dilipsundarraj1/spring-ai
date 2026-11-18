package com.mcp.logging;

import java.util.Arrays;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * Decorates a {@link ToolCallback} so every tool invocation the LLM makes is logged with
 * its input, result, and timing. The underlying MCP tools/call is untouched — this only
 * adds visibility around the delegate.
 */
public class LoggingToolCallback implements ToolCallback {

	private static final Logger log = LoggerFactory.getLogger(LoggingToolCallback.class);

	private static final int MAX_LOGGED_RESULT_LENGTH = 500;

	private final ToolCallback delegate;

	public LoggingToolCallback(ToolCallback delegate) {
		this.delegate = delegate;
	}

	public static ToolCallback[] wrapAll(ToolCallback[] toolCallbacks) {
		return Arrays.stream(toolCallbacks).map(LoggingToolCallback::new).toArray(ToolCallback[]::new);
	}

	@Override
	public ToolDefinition getToolDefinition() {
		return delegate.getToolDefinition();
	}

	@Override
	public ToolMetadata getToolMetadata() {
		return delegate.getToolMetadata();
	}

	@Override
	public String call(String toolInput) {
		return logAround(toolInput, () -> delegate.call(toolInput));
	}

	@Override
	public String call(String toolInput, ToolContext toolContext) {
		return logAround(toolInput, () -> delegate.call(toolInput, toolContext));
	}

	private String logAround(String toolInput, Supplier<String> invocation) {
		var toolName = getToolDefinition().name();
		log.info("Invoking MCP tool [{}] with input: {}", toolName, toolInput);
		var start = System.nanoTime();
		try {
			var result = invocation.get();
			log.info("MCP tool [{}] completed in {} ms with result: {}", toolName, elapsedMillis(start),
					abbreviate(result));
			return result;
		}
		catch (RuntimeException ex) {
			log.warn("MCP tool [{}] failed after {} ms: {}", toolName, elapsedMillis(start), ex.getMessage());
			throw ex;
		}
	}

	private static long elapsedMillis(long startNanos) {
		return (System.nanoTime() - startNanos) / 1_000_000;
	}

	private static String abbreviate(String result) {
		if (result == null || result.length() <= MAX_LOGGED_RESULT_LENGTH) {
			return result;
		}
		return result.substring(0, MAX_LOGGED_RESULT_LENGTH) + "... (" + result.length() + " chars)";
	}

}
