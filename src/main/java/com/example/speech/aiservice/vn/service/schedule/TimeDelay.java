package com.example.speech.aiservice.vn.service.schedule;

import org.springframework.stereotype.Component;

@Component
public class TimeDelay {

    /**
     * Defaults to 60 if no input parameter is given.
     */
    private int second = 5;

    public int getSecond() {
        return second;
    }

    public void setSecond(int second) {
        this.second = second;
    }
}
