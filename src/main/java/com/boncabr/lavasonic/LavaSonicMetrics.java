package com.boncabr.lavasonic;

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Thread-safe metrics for the PCM stage before Discord Opus encoding. */
@Service
public final class LavaSonicMetrics {
    private final ConcurrentMap<String, Entry> entries = new ConcurrentHashMap<>();

    public Handle open(String guildId, String trackId, String mode, AudioDataFormat format) {
        String id = UUID.randomUUID().toString();
        Entry entry = new Entry(id, guildId, trackId, mode, format);
        entries.put(id, entry);
        return new Handle(entry, this);
    }

    private void close(String id) {
        Entry entry = entries.get(id);
        if (entry != null) {
            synchronized (entry) {
                entry.closed = true;
                entry.updatedAt = System.currentTimeMillis();
            }
        }
    }

    public Map<String, Object> health() {
        int active = 0;
        for (Entry entry : entries.values()) {
            synchronized (entry) {
                if (!entry.closed) active++;
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("plugin", "lavasonic");
        result.put("version", "0.1.0");
        result.put("lavalink", "v4");
        result.put("stage", "pre-opus-pcm");
        result.put("activeFilters", active);
        result.put("trackedFilters", entries.size());
        return result;
    }

    public Map<String, Object> metrics() {
        List<Entry> sorted = new ArrayList<>(entries.values());
        sorted.sort(Comparator.comparingLong(Entry::updatedAt).reversed());
        List<Map<String, Object>> snapshots = new ArrayList<>();
        for (Entry entry : sorted) {
            synchronized (entry) {
                snapshots.add(entry.snapshot());
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("plugin", "lavasonic");
        result.put("version", "0.1.0");
        result.put("stage", "pre-opus-pcm");
        result.put("filters", snapshots);
        return result;
    }

    public static final class Handle {
        private final Entry entry;
        private final LavaSonicMetrics owner;

        private Handle(Entry entry, LavaSonicMetrics owner) {
            this.entry = entry;
            this.owner = owner;
        }

        public void record(long sampleCount, double squareSum, double peak,
                           long clippedSamples, long nonFiniteSamples,
                           boolean silentBlock, long limitedSamples) {
            synchronized (entry) {
                entry.blocks++;
                entry.samples += sampleCount;
                entry.squareSum += squareSum;
                entry.peak = Math.max(entry.peak, peak);
                entry.clippedSamples += clippedSamples;
                entry.nonFiniteSamples += nonFiniteSamples;
                entry.limitedSamples += limitedSamples;
                if (silentBlock) entry.silentBlocks++;
                entry.updatedAt = System.currentTimeMillis();
            }
        }

        public void close() {
            owner.close(entry.id);
        }
    }

    private static final class Entry {
        private final String id;
        private final String guildId;
        private final String trackId;
        private final String mode;
        private final int channels;
        private final int sampleRate;
        private final int chunkSampleCount;
        private final String codec;
        private final long startedAt = System.currentTimeMillis();
        private long updatedAt = startedAt;
        private boolean closed;
        private long blocks;
        private long samples;
        private double squareSum;
        private double peak;
        private long clippedSamples;
        private long nonFiniteSamples;
        private long silentBlocks;
        private long limitedSamples;

        private Entry(String id, String guildId, String trackId, String mode, AudioDataFormat format) {
            this.id = id;
            this.guildId = guildId;
            this.trackId = trackId;
            this.mode = mode;
            this.channels = format.channelCount;
            this.sampleRate = format.sampleRate;
            this.chunkSampleCount = format.chunkSampleCount;
            this.codec = format.codecName();
        }

        private long updatedAt() {
            return updatedAt;
        }

        private Map<String, Object> snapshot() {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", id);
            result.put("guildId", guildId);
            result.put("trackId", trackId);
            result.put("mode", mode);
            result.put("active", !closed);
            result.put("channels", channels);
            result.put("sampleRate", sampleRate);
            result.put("chunkSampleCount", chunkSampleCount);
            result.put("codec", codec);
            result.put("blocks", blocks);
            result.put("samples", samples);
            result.put("rmsDbfs", rmsDbfs());
            result.put("peakDbfs", dbfs(peak));
            result.put("clippedSamples", clippedSamples);
            result.put("nonFiniteSamples", nonFiniteSamples);
            result.put("silentBlocks", silentBlocks);
            result.put("limitedSamples", limitedSamples);
            result.put("startedAt", startedAt);
            result.put("updatedAt", updatedAt);
            return result;
        }

        private double rmsDbfs() {
            if (samples == 0) return -144.0;
            return dbfs(Math.sqrt(squareSum / samples));
        }

        private static double dbfs(double amplitude) {
            if (amplitude <= 0.0000001) return -140.0;
            return 20.0 * Math.log10(amplitude);
        }
    }
}