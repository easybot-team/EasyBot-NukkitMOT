package com.springwater.easybot.nukkitmot.util;

import com.springwater.easybot.bridge.message.AtSegment;
import com.springwater.easybot.bridge.message.FileSegment;
import com.springwater.easybot.bridge.message.ImageSegment;
import com.springwater.easybot.bridge.message.Segment;

import java.util.List;

public final class MessageRenderer {
    private MessageRenderer() {
    }

    public static String render(List<Segment> segments, String fallback) {
        if (segments == null || segments.isEmpty()) {
            return fallback == null ? "" : fallback;
        }

        StringBuilder output = new StringBuilder();
        for (Segment segment : segments) {
            if (segment == null) {
                continue;
            }
            if (segment instanceof AtSegment at) {
                append(output, renderAt(at));
            } else if (segment instanceof ImageSegment image) {
                append(output, image.getText());
                appendUrl(output, image.getUrl());
            } else if (segment instanceof FileSegment file) {
                append(output, file.getRawText());
                appendUrl(output, file.getFileUrl());
            } else {
                append(output, segment.getRawText());
            }
        }
        return output.isEmpty() ? (fallback == null ? "" : fallback) : output.toString();
    }

    private static void append(StringBuilder output, String value) {
        if (value != null && !value.isEmpty()) {
            output.append(value);
        }
    }

    private static void appendUrl(StringBuilder output, String url) {
        if (url != null && !url.isBlank()) {
            if (!output.isEmpty() && !Character.isWhitespace(output.charAt(output.length() - 1))) {
                output.append(' ');
            }
            output.append(url);
        }
    }

    private static String renderAt(AtSegment segment) {
        if ("0".equals(segment.getAtUserId())) {
            return "@全体成员";
        }
        String[] playerNames = segment.getAtPlayerNames();
        if (playerNames != null && playerNames.length > 0 && playerNames[0] != null && !playerNames[0].isBlank()) {
            return "@" + playerNames[0];
        }
        String userId = segment.getAtUserId();
        return userId == null || userId.isBlank() ? "@未知用户" : "@" + userId;
    }
}
