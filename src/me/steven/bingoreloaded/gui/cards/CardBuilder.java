package me.steven.bingoreloaded.gui.cards;

import me.steven.bingoreloaded.BingoGameMode;
import me.steven.bingoreloaded.util.FlexibleColor;
import org.bukkit.Material;
import org.bukkit.scoreboard.Team;

public class CardBuilder
{
    public static BingoCard fromMode(BingoGameMode mode, CardSize size)
    {
        return switch (mode)
                {
                    case LOCKOUT -> new LockoutBingoCard(size);
                    case COMPLETE -> new CompleteBingoCard(size);
                    default -> new BingoCard(size);
                };
    }

    public static Material completeColor(Team team)
    {
        FlexibleColor color = FlexibleColor.fromChatColor(team.getColor());
        if (color == null) return Material.BLACK_STAINED_GLASS_PANE;
        return color.glassPane;
    }
}
