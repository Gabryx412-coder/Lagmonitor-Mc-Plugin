package com.github.gabrycoder.lagmonitor.util;

import java.util.Arrays;

public class RollingOverHistory {

    private final float[] samples;
    private float total;

    private int currentPosition;
    private int currentSize = 1;

    public RollingOverHistory(int size, float firstValue) {
        this.samples = new float[size];
        reset(firstValue);
    }

    public void add(float sample) {
        currentPosition++;
        if (currentPosition >= samples.length) {
            //we reached the end - go back to the beginning
            currentPosition = 0;
        }

        if (currentSize < samples.length) {
            //array is not full yet
            currentSize++;
        }

        //delete the latest sample which wil be overridden
        total -= samples[currentPosition];

        total += sample;
        samples[currentPosition] = sample;
    }

    public float getAverage() {
        return total / currentSize;
    }

    public int getCurrentPosition() {
        return currentPosition;
    }

    public int getCurrentSize() {
        return currentSize;
    }

    public float getLastSample() {
        int lastPos = currentPosition;
        if (lastPos < 0) {
            lastPos = samples.length - 1;
        }

        return samples[lastPos];
    }

    public float[] getSamples() {
        return Arrays.copyOf(samples, samples.length);
    }

    public void reset(float firstVal) {
        samples[0] = firstVal;
        total = firstVal;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + '{' +
                "samples=" + Arrays.toString(samples) +
                ", total=" + total +
                ", currentPosition=" + currentPosition +
                ", currentSize=" + currentSize +
                '}';
    }
}
