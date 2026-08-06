package org.group1.coffeeshopapi.common.exception;

public class InvalidOtpException extends RuntimeException{
    public InvalidOtpException(String message){
        super(message);
    }
}
