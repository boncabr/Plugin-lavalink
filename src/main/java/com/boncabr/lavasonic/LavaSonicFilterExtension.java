package com.boncabr.lavasonic;

import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import dev.arbjerg.lavalink.api.AudioFilterExtension;
import kotlinx.serialization.json.JsonElement;
import kotlinx.serialization.json.JsonObject;
import kotlinx.serialization.json.JsonPrimitive;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Registers the lavasonic key under Lavalink v4 pluginFilters. */
@Service
public final class LavaSonicFilterExtension implements AudioFilterExtension {
    private static final Logger LOG = LoggerFactory.getLogger(LavaSonicFilterExtension.class);
    private final LavaSonicMetrics metrics;

    public LavaSonicFilterExtension(LavaSonicMetrics metrics) {
        this.metrics = metrics;
        LOG.info("Loaded Lavalink v4 audio filter: lavasonic");
    }

    @Override
    @NotNull
    public String getName() {
        return "lavasonic";
    }

    @Override
    public boolean isEnabled(@NotNull JsonElement data) {
        JsonObject object = asObject(data);
        return object != null && booleanValue(object, "enabled", true);
    }

    @Override
    @Nullable
    public FloatPcmAudioFilter build(@NotNull JsonElement data,
                                     @Nullable AudioDataFormat format,
                                     @Nullable FloatPcmAudioFilter output) {
        JsonObject object = asObject(data);
        if (object == null || format == null || output == null || !isEnabled(data)) {
            return null;
        }

        String mode = stringValue(object, "mode", "max-detail");
        String guildId = stringValue(object, "guildId", null);
        String trackId = stringValue(object, "trackId", null);
        double gain = clamp(numberValue(object, "gain", 1.0), 0.1, 4.0);
        boolean limiter = booleanValue(object, "limiter", true);
        double ceilingDb = clamp(numberValue(object, "ceilingDb", -1.0), -24.0, -0.1);
        double ceiling = Math.pow(10.0, ceilingDb / 20.0);

        LavaSonicMetrics.Handle handle = metrics.open(guildId, trackId, mode, format);
        return new LavaSonicFilter(output, format, handle, gain, limiter, ceiling);
    }

    private static JsonObject asObject(JsonElement data) {
        return data instanceof JsonObject ? (JsonObject) data : null;
    }

    private static boolean booleanValue(JsonObject object, String key, boolean fallback) {
        JsonElement value = object.get(key);
        if (!(value instanceof JsonPrimitive)) return fallback;
        return Boolean.parseBoolean(((JsonPrimitive) value).getContent());
    }

    private static String stringValue(JsonObject object, String key, String fallback) {
        JsonElement value = object.get(key);
        if (!(value instanceof JsonPrimitive)) return fallback;
        String content = ((JsonPrimitive) value).getContent();
        return content == null || content.isBlank() ? fallback : content;
    }

    private static double numberValue(JsonObject object, String key, double fallback) {
        JsonElement value = object.get(key);
        if (!(value instanceof JsonPrimitive)) return fallback;
        try {
            return Double.parseDouble(((JsonPrimitive) value).getContent());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) return min;
        return Math.max(min, Math.min(max, value));
    }
}