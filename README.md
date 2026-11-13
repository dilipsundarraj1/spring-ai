# Spring AI Masterclass: LLMs, RAG, MCP, Tools & Memory

The source code for the **Spring AI Masterclass**.

- **What you build:** real AI applications in Java with Spring AI 2.0 and Spring Boot 4.
- **What you need:** Spring Boot. No AI or machine learning background is needed.
- **Where it starts:** what an LLM is, and your first `ChatClient` call.
- **Where it goes:**
  - prompt engineering
  - structured outputs and tool calling
  - RAG with PGVector
  - images, vision and audio
  - observability with OpenTelemetry
  - MCP servers and clients
  - chat memory and long-term memory

New to the course? Start with the **[Course Roadmap & Objectives](COURSE.md)**: seven steps from LLM foundations to MCP and memory.

<!-- TOC -->
  * [Module Summary](#module-summary)
<!-- TOC -->

## Module Summary

| Module | What it covers |
|---|---|
| **explore-openai** | Chat, streaming, prompt engineering (zero-shot / few-shot / CoT / multi-step), structured outputs, tool calling, custom advisors, and prompt injection defence with OpenAI. |
| **multimodal-llms** | Image generation (DALL-E), vision, text-to-speech (TTS), and speech-to-text (Whisper) with OpenAI. |
| **chat-memory** | Making stateless LLMs remember: conversation IDs, `MessageWindowChatMemory`, in-memory vs. JDBC (Postgres) persistence, managing memory manually with `ChatModel`, and semantic chat memory with a vector store (PGVector). Then long-term memory: episodic (dated conversation summaries), semantic (a user profile) and procedural (procedures loaded on demand, with human approval), shown in a chat UI. |
| **explore-rag** | Document ingestion into a vector store and RAG-grounded queries using vector similarity search. |
| **mcp** | MCP servers (stdio, WebMVC, WebFlux, currency converter, inventory) and MCP clients (WebMVC + WebFlux). |
| **explore-llama** | Basic chat using a local Llama model via Ollama through the Spring AI abstraction. |
| **multiple-llms** | Configuring and routing between OpenAI, Llama (Ollama), Anthropic, and Gemini within a single Spring Boot app. |
