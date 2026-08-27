package com.mcp.exception;

/**
 * Thrown when a call to the openexchangerates.org backend fails (HTTP error, timeout,
 * unparseable response). The message is surfaced to MCP clients as the tool error.
 */
public class CurrencyApiException extends RuntimeException {

	public CurrencyApiException(String message, Throwable cause) {
		super(message, cause);
	}

}
