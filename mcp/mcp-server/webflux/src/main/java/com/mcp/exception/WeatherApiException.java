package com.mcp.exception;

/**
 * Thrown when a call to the weatherapi.com backend fails (HTTP error, timeout,
 * unparseable response). The message is surfaced to MCP clients as the tool error.
 */
public class WeatherApiException extends RuntimeException {

	public WeatherApiException(String message, Throwable cause) {
		super(message, cause);
	}

}
