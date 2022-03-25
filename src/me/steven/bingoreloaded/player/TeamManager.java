package me.steven.bingoreloaded.player;

import me.steven.bingoreloaded.BingoGame;
import me.steven.bingoreloaded.BingoReloaded;
import me.steven.bingoreloaded.gui.AbstractGUIInventory;
import me.steven.bingoreloaded.gui.ItemPickerUI;
import me.steven.bingoreloaded.gui.cards.BingoCard;
import me.steven.bingoreloaded.gui.cards.LockoutBingoCard;
import me.steven.bingoreloaded.item.InventoryItem;
import me.steven.bingoreloaded.util.FlexibleColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.scoreboard.*;

import java.util.*;

public class TeamManager
{
    private final Map<Team, BingoCard> activeTeams;
    private final BingoGame game;
    private final Scoreboard scoreboard;

    public TeamManager(BingoGame game, Scoreboard scoreboard)
    {
        this.activeTeams = new HashMap<>();
        this.game = game;
        this.scoreboard = scoreboard;

        createTeams();
    }

    public Team getTeamOfPlayer(Player player)
    {
        for (Team t : activeTeams.keySet())
        {
            for (String entry : t.getEntries())
            {
                if (entry.equals(player.getName()))
                {
                    return t;
                }
            }
        }
        return null;
    }

    public void openTeamSelector(Player player, AbstractGUIInventory parentUI)
    {
        if (game.isGameInProgress())
        {
            BingoReloaded.print(ChatColor.RED + "You cannot join an ongoing game!", player);
            return;
        }

        List<InventoryItem> optionItems = new ArrayList<>();
        for (Team t : scoreboard.getTeams())
        {
            FlexibleColor color = FlexibleColor.fromChatColor(t.getColor());
            if (color != null)
            {
                optionItems.add(new InventoryItem(color.concrete, color.chatColor + t.getDisplayName()));
            }
        }

        ItemPickerUI teamPicker = new ItemPickerUI(optionItems, "Pick A Team", parentUI)
        {
            @Override
            public void onOptionClickedDelegate(InventoryClickEvent event, InventoryItem clickedOption, Player player)
            {
                FlexibleColor color = FlexibleColor.fromConcrete(clickedOption.getType());
                if (color == null)
                    return;

                addPlayerToTeam(player, color.displayName);
                close(player);
            }
        };
        teamPicker.open(player);
    }

    public void addPlayerToTeam(Player player, String teamName)
    {
        Team team = scoreboard.getTeam(teamName);
        if (team == null)
        {
            BingoReloaded.broadcast(ChatColor.RED + "Could not add you to team '" + teamName + "', since it just doesn't exist!");
            return;
        }
        removePlayerFromAllTeams(player);

        activateTeam(teamName);

        team.addEntry(player.getName());
        BingoReloaded.print("You successfully joined team " + team.getColor() + team.getDisplayName(), player);
    }

    public void removePlayerFromAllTeams(Player player)
    {
        if (!getParticipants().contains(player))
            return;
        for (Team team : scoreboard.getTeams())
        {
            team.removeEntry(player.getName());
        }
    }

    public void removeEmptyTeams()
    {
        for (Team t : activeTeams.keySet())
        {
            if (t.getEntries().size() <= 0)
            {
                activeTeams.remove(t);
            }
        }
    }

    public BingoCard getCardForTeam(Team team)
    {
        return activeTeams.get(team);
    }

    public void initializeCards(BingoCard masterCard)
    {
        if (masterCard instanceof LockoutBingoCard lockoutCard)
        {
            lockoutCard.teamCount = activeTeams.size();
        }
        activeTeams.replaceAll((t, v) -> masterCard.copy());
    }

    public void setCardForTeam(Team team, BingoCard card)
    {
        activeTeams.put(team, card);
    }

    public Map<Team, BingoCard> getActiveTeams()
    {
        return activeTeams;
    }


    public Set<Player> getParticipants()
    {
        Set<Player> players = new HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers())
        {
            boolean found = false;
            for (Team team : activeTeams.keySet())
            {
                for (String entry : team.getEntries())
                {
                    if (entry.equals(p.getName()))
                    {
                        players.add(p);
                        found = true;
                        break;
                    }
                }
                if (found)
                {
                    break;
                }
            }
        }

        return players;
    }

    public void updateActivePlayers()
    {
        for (Team team : activeTeams.keySet())
        {
            for (String entry : team.getEntries())
            {
                Player p = Bukkit.getPlayer(entry);
                if (p != null)
                    if (p.isOnline())
                        break;

                removePlayerFromAllTeams(p);
            }
        }
    }

    public Team getTeamByName(String name)
    {
        for (Team team : scoreboard.getTeams())
        {
            if (team.getDisplayName().equals(name))
            {
                return team;
            }
        }

        return null;
    }

    public void activateTeam(Team team)
    {
        if (!activeTeams.containsKey(team))
        {
            activeTeams.put(team, null);
        }
    }

    public void activateTeam(String teamName)
    {
        activateTeam(getTeamByName(teamName));
    }

    private void createTeams()
    {
        for (ChatColor c : ChatColor.values())
        {
            if (c.isColor())
            {
                FlexibleColor flexColor = FlexibleColor.fromChatColor(c);
                if (flexColor == null)
                    return;

                String name = flexColor.displayName;
                scoreboard.registerNewTeam(name);
                scoreboard.getTeam(name).setColor(flexColor.chatColor);
            }
        }

        BingoReloaded.broadcast(ChatColor.GREEN + "Successfully created " + scoreboard.getTeams().size() + " teams");
        BingoReloaded.print(ChatColor.GREEN + "Successfully created " + scoreboard.getTeams().size() + " teams");
    }
}
