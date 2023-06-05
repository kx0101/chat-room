package com.elijahkx.utils.messages;

import java.text.SimpleDateFormat;
import java.util.Date;

public class MessagesUtils {
    public enum MessageType {
        NORMAL, JOIN, LEAVE
    }

    public static String getMessageContent(String message, MessageType messageType, String nickname) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = sdf.format(new Date());
        String prefix = getMessagePrefix(messageType);

        return "[" + timestamp + "] " + nickname + prefix + message;
    }

    private static String getMessagePrefix(MessageType messageType) {
        switch (messageType) {
            case NORMAL:
                return ": ";
            default:
                return " ";
        }
    }
}
