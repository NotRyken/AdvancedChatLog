/*
 * Copyright (C) 2021-2025 DarkKronicle
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.github.darkkronicle.advancedchatlog.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.github.darkkronicle.advancedchatcore.chat.ChatMessage;
import io.github.darkkronicle.advancedchatcore.interfaces.IJsonSave;
import io.github.darkkronicle.advancedchatlog.AdvancedChatLog;
import io.github.darkkronicle.advancedchatlog.config.ChatLogConfigStorage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class LogChatMessageSerializer implements IJsonSave<LogChatMessage> {
    private final Text.Serializer serializer = new Text.Serializer(DynamicRegistryManager.EMPTY);
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public LogChatMessageSerializer() {}

    private Style cleanStyle(Style style) {
        if (!ChatLogConfigStorage.General.CLEAN_SAVE.config.getBooleanValue()) {
            return style;
        }
        style = style.withClickEvent(null);
        style = style.withHoverEvent(null);
        style = style.withInsertion(null);
        return style;
    }

    private Text transfer(Text text) {
        // Using the built in serializer LiteralText is required
        Text base = Text.empty();
        for (Text t : text.getSiblings()) {
            Text newT = Text.literal(t.getString()).fillStyle(cleanStyle(t.getStyle()));
            base.getSiblings().add(newT);
        }
        return base;
    }

    private Style forceCleanStyle(Style style) {
        style = style.withClickEvent(null);
        style = style.withHoverEvent(null);
        style = style.withInsertion(null);
        return style;
    }

    private Text forceTransfer(Text text) {
        // Using the built in serializer LiteralText is required
        Text base = Text.empty();
        for (Text t : text.getSiblings()) {
            Text newT = Text.literal(t.getString()).fillStyle(forceCleanStyle(t.getStyle()));
            base.getSiblings().add(newT);
        }
        return base;
    }

    @Override
    public LogChatMessage load(JsonObject obj) {
        LocalDateTime dateTime = LocalDateTime.from(formatter.parse(obj.get("time").getAsString()));
        LocalDate date = dateTime.toLocalDate();
        LocalTime time = dateTime.toLocalTime();
        Text display = serializer.deserialize(obj.get("display"), Text.class, null);
        Text original = serializer.deserialize(obj.get("original"), Text.class, null);
        int stacks = obj.get("stacks").getAsByte();
        ChatMessage message =
                ChatMessage.builder()
                        .time(time)
                        .displayText(display)
                        .originalText(original)
                        .build();
        return new LogChatMessage(message, date);
    }

    @Override
    public JsonObject save(LogChatMessage message) {
        JsonObject json = new JsonObject();
        ChatMessage chat = message.getMessage();
        LocalDateTime dateTime = LocalDateTime.of(message.getDate(), chat.getTime());
        try {
            json.addProperty("time", formatter.format(dateTime));
            json.addProperty("stacks", chat.getStacks());
            json.add("display", serializer.serialize(transfer(chat.getDisplayText()), Text.class, null));
            json.add("original", serializer.serialize(transfer(chat.getOriginalText()), Text.class, null));
        } catch (JsonParseException e) {
            try {
                AdvancedChatLog.LOGGER.warn("[AdvancedChatLog] Save Error 1: {}", e.getMessage());
                AdvancedChatLog.LOGGER.warn("Original Message:");
                AdvancedChatLog.LOGGER.warn(chat.getOriginalText().getString());
                AdvancedChatLog.LOGGER.warn(chat.getOriginalText().toString());
                json = new JsonObject();
                json.addProperty("time", formatter.format(dateTime));
                json.addProperty("stacks", chat.getStacks());
                json.add("display", serializer.serialize(forceTransfer(chat.getDisplayText()), Text.class, null));
                json.add("original", serializer.serialize(forceTransfer(chat.getOriginalText()), Text.class, null));
            } catch (Exception e2) {
                AdvancedChatLog.LOGGER.warn("[AdvancedChatLog] Save Error 2", e2);
                return new JsonObject();
            }
        }
        return json;
    }
}
