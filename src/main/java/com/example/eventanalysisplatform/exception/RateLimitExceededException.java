package com.example.eventanalysisplatform.exception;

public class RateLimitExceededException extends RuntimeException{

    public RateLimitExceededException(String source){
        super("Rate limit exceeded for source: "+source);
    }
}
