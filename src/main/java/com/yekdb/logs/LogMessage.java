package com.yekdb.logs;

import java.time.LocalDateTime;

public final class LogMessage {

    private final LocalDateTime timestamp;

    private final LogLevel level;

    private final String message;

    public LogMessage(
            LocalDateTime timestamp,
            LogLevel level,
            String message
    ){
        this.level=level;
        this.timestamp=timestamp;
        this.message=message;
    }
    public LocalDateTime getTimestamp(){
        return timestamp;
    }
    public LogLevel getLevel(){
        return level;
    }
    public String getMessage(){
        return message;
    }

}
