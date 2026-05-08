# AgentFlow

An agentic developer-productivity service that retrieves engineering documentation and generates code review suggestions using LLM tool-calling.

Built as a portfolio project to explore how agentic workflows can be designed in Java without overengineering — small state machine, pluggable tools, retrieval grounded in real engineering corpora.

## What it does

AgentFlow exposes a single `/agent/run` endpoint that accepts a developer query and returns a grounded response. Internally, an orchestrator decides which tools to invoke:

- **DocSearch** — semantic search over Spring Boot reference and AWS SDK for Java documentation, backed by a FAISS vector index.
- **CodeReviewAssist** — given a diff, retrieves relevant style guidelines via DocSearch and generates structured review comments.

The agent runs on AWS Bedrock (Claude Haiku for generation, Titan v2 for embeddings), with Redis for conversation memory and a Python sidecar service hosting the FAISS index.

## Architecture

See [ARCHITECTURE.md](./ARCHITECTURE.md) for design decisions, trade-offs, and what's intentionally out of scope.

## Stack

Java 21, Spring Boot 3, AWS Bedrock, FAISS (Python sidecar), Redis, Docker Compose, JUnit 5, Testcontainers.

## Status

Work in progress. See the bottom of `ARCHITECTURE.md` for the roadmap.

## Running locally

> Setup instructions added as the components come online. See `ARCHITECTURE.md` for the current build order.

## Author

Nakka Johnson · [LinkedIn](https://linkedin.com/in/nakka-johnson) · [GitHub](https://github.com/Nakka-Johnson)