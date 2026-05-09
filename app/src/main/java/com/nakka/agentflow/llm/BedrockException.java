package com.nakka.agentflow.llm;

public class BedrockException extends RuntimeException {

    public BedrockException(String message, Throwable cause) {
        super(message, cause);
    }
}