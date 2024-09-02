package com.dimon.catanbackend.utils.api;

import lombok.Getter;

@Getter
public class ApiMetrics {
    private long totalCalls;
    private long totalSuccessCalls;
    private long totalFailureCalls;
    private long totalDuration;
    private long maxDuration;

    public synchronized void addCall(long duration, boolean success) {
        totalCalls++;
        totalDuration += duration;
        if(success) {
            totalSuccessCalls++;
        } else {
            totalFailureCalls++;
        }
        if(duration > maxDuration) {
            maxDuration = duration;
        }
    }

    public long getAverageDuration() {
        return totalCalls > 0 ? totalDuration / totalCalls : 0;
    }
}
