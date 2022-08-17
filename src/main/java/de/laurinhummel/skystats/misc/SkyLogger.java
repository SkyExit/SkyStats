package de.laurinhummel.skystats.misc;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class SkyLogger {
    public static void sendMessage(CommandSender receiver, String message) {
        receiver.sendMessage(message);
    }

    public static void sendPrefixMessage(CommandSender receiver, String message) {
        sendMessage(receiver, SkyStrings.SKYSTATSCONSOLE + message);
    }

    public static void sendConsole(String message) {
        sendPrefixMessage((CommandSender)Bukkit.getConsoleSender(), message);
    }
}
