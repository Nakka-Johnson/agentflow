# AgentFlow

AgentFlow is a Java 21 and Spring Boot 3.5 agentic developer-assistance service. It exposes a single HTTP API that accepts a developer query, selects a tool, retrieves grounded context when needed, and returns a structured response. The project is intentionally designed to stay small at the orchestration layer while still showing a realistic multi-service architecture: Spring Boot backend, Python retrieval sidecar, AWS Bedrock generation, and pluggable tools.

## Technical Overview

The main API is `POST /agent/run`, implemented in the Spring controller layer. Requests carry a `query` and optional `conversationId`, and responses include the generated answer, the conversation ID, tool call summaries, and end-to-end latency. This makes the service easy to integrate while still exposing enough trace data to understand how an answer was produced.

The runtime is organized around a small agent flow:

1. The request reaches the API layer.
2. The agent service chooses an execution mode.
3. The orchestrator or Bedrock-backed path handles planning and generation.
4. Tools are invoked when retrieval or code-review grounding is needed.
5. The final response is returned with latency and tool metadata.

Two agent modes are already present in the codebase:

- **Stub / local mode** uses a small enum-based orchestrator with keyword-based planning.
- **Bedrock mode** uses AWS Bedrock for direct generation through the AWS SDK.

The tool layer is pluggable through a registry. The current tools are:

- **DocSearch** - semantic retrieval over engineering documentation using a Python FAISS sidecar.
- **CodeReviewAssist** - retrieves review guidance via DocSearch and produces structured review feedback.

The retrieval sidecar is a separate FastAPI service that loads a FAISS index and metadata at startup, computes embeddings with SentenceTransformers, and exposes `POST /search` for top-K similarity search. The Java service calls it over HTTP, which keeps retrieval isolated and easy to swap later.

## Architecture

See [ARCHITECTURE.md](./ARCHITECTURE.md) for design decisions, trade-offs, and what's intentionally out of scope.

## Stack

Java 21, Spring Boot 3.5, AWS Bedrock, FastAPI, FAISS, SentenceTransformers, Maven, JUnit 5, and Spring Boot test support.

## Status

The core wiring is in place: API, orchestrator, tool registry, retrieval sidecar, and Bedrock client. The current planner is still heuristic, the code-review tool is still partially stubbed, and the longer-term roadmap is to replace the naive planner with model-driven tool selection, add evaluation harnesses, and expand production hardening.

## Running locally

See [ARCHITECTURE.md](./ARCHITECTURE.md) for the current build order and the intended local setup shape.

## Author

Nakka Johnson · [LinkedIn](https://linkedin.com/in/nakka-johnson) · [GitHub](https://github.com/Nakka-Johnson)
