package com.boncabr.lavasonic;

import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;

/** PCM quality gate with optional gain and bounded soft ceiling. */
public final class LavaSonicFilter implements FloatPcmAudioFilter {
    private static final double SILENCE_THRESHOLD = 0.000316227766;

    private final FloatPcmAudioFilter output;
    private final LavaSonicMetrics.Handle metrics;
    private final double gain;
    private final boolean limiter;
    private final double ceiling;

    public LavaSonicFilter(FloatPcmAudioFilter output, AudioDataFormat format,
                           LavaSonicMetrics.Handle metrics, double gain,
                           boolean limiter, double ceiling) {
        this.output = output;
        this.metrics = metrics;
        this.gain = gain;
        this.limiter = limiter;
        this.ceiling = ceiling;
    }

    @Override
    public void process(float[][] input, int offset, int length) throws InterruptedException {
        long sampleCount = 0;
        long clippedSamples = 0;
        long nonFiniteSamples = 0;
        long limitedSamples = 0;
        double squareSum = 0.0;
        double peak = 0.0;
        int endLimit = offset + length;

        for (float[] channel : input) {
            if (channel == null) continue;
            int end = Math.min(endLimit, channel.length);
            int start = Math.max(0, Math.min(offset, end));
            for (int index = start; index < end; index++) {
                double sample = channel[index];
                if (!Double.isFinite(sample)) {
                    sample = 0.0;
                    nonFiniteSamples++;
                }
                sample *= gain;
                if (limiter && Math.abs(sample) > ceiling) {
                    sample = softLimit(sample, ceiling);
                    limitedSamples++;
                }
                float processed = (float) sample;
                channel[index] = processed;
                double amplitude = Math.abs(processed);
                peak = Math.max(peak, amplitude);
                squareSum += processed * (double) processed;
                sampleCount++;
                if (amplitude >= 0.999f) clippedSamples++;
            }
        }

        metrics.record(sampleCount, squareSum, peak, clippedSamples,
                nonFiniteSamples, sampleCount > 0 && peak < SILENCE_THRESHOLD,
                limitedSamples);
        output.process(input, offset, length);
    }

    private static double softLimit(double sample, double ceiling) {
        double sign = Math.signum(sample);
        double magnitude = Math.abs(sample);
        if (magnitude <= ceiling) return sample;
        double denominator = Math.max(0.0001, 1.0 - ceiling);
        double excess = Math.min(1.0, (magnitude - ceiling) / denominator);
        double compressed = ceiling + denominator * (1.0 - Math.exp(-3.0 * excess));
        return sign * Math.min(0.9995, compressed);
    }

    @Override
    public void seekPerformed(long requestedTime, long providedTime) {
        output.seekPerformed(requestedTime, providedTime);
    }

    @Override
    public void flush() throws InterruptedException {
        output.flush();
    }

    @Override
    public void close() {
        metrics.close();
        output.close();
    }
}