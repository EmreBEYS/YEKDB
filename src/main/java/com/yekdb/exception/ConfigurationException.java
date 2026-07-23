package com.yekdb.exception;

public class ConfigurationException extends  YekdbException{
    public ConfigurationException(String message){
        super(message);
    }
    public ConfigurationException(String message,Throwable cause){
        super(message,cause);
    }
}
