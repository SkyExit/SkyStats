package de.laurinhummel.skystats.managers;

import org.bukkit.entity.Player;

public class CompareManager {

    private String player1Data;
    private String player2Data;
    private String enumType;
    private String enumName;

    public CompareManager(Object player1Data, Object player2Data, String enumType, String enumName) {
        this.player1Data = player1Data.toString();
        this.player2Data = player2Data.toString();
        this.enumType = enumType;
        this.enumName = enumName;
    }

    public String getPlayer1Data() { return player1Data; }
    public String getPlayer2Data() { return player2Data; }
    public String getEnumType() { return enumType; }
    public String getEnumName() { return enumName; }
}
