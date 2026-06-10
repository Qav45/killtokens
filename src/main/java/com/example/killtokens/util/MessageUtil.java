package com.example.killtokens.util;

import org.bukkit.ChatColor;

public class MessageUtil {
    public static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }
}
