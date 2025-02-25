package io.github.steaf23.bingoreloaded.command;

import io.github.steaf23.bingoreloaded.BingoReloaded;
import io.github.steaf23.bingoreloaded.data.BingoStatData;
import io.github.steaf23.bingoreloaded.data.BingoTranslation;
import io.github.steaf23.bingoreloaded.data.ConfigData;
import io.github.steaf23.bingoreloaded.data.PlayerData;
import io.github.steaf23.bingoreloaded.gameloop.BingoGame;
import io.github.steaf23.bingoreloaded.gameloop.SessionManager;
import io.github.steaf23.bingoreloaded.gameloop.BingoSession;
import io.github.steaf23.bingoreloaded.gui.AdminBingoMenu;
import io.github.steaf23.bingoreloaded.gui.PlayerBingoMenu;
import io.github.steaf23.bingoreloaded.gui.TeamEditorMenu;
import io.github.steaf23.bingoreloaded.gui.TeamSelectionMenu;
import io.github.steaf23.bingoreloaded.gui.creator.BingoCreatorMenu;
import io.github.steaf23.bingoreloaded.player.BingoParticipant;
import io.github.steaf23.bingoreloaded.player.BingoPlayer;
import io.github.steaf23.bingoreloaded.settings.PlayerKit;
import io.github.steaf23.bingoreloaded.util.Message;
import io.github.steaf23.bingoreloaded.util.TranslatedMessage;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BingoCommand implements TabExecutor
{
    private final ConfigData config;
    private final SessionManager gameManager;
    private final PlayerData playerData;

    public BingoCommand(ConfigData config, SessionManager gameManager) {
        this.config = config;
        this.gameManager = gameManager;
        this.playerData = new PlayerData();
    }

    @Override
    public boolean onCommand(@NonNull CommandSender commandSender, @NonNull Command command, @NonNull String alias, String[] args) {
        if (!(commandSender instanceof Player player) || !player.hasPermission("bingo.player")) {
            return false;
        }

        BingoSession session = gameManager.getSession(BingoReloaded.getWorldNameOfDimension(player.getWorld()));
        if (session == null)
            return false;

        if (!BingoReloaded.getWorldNameOfDimension(player.getWorld()).equals(session.worldName))
            return false;

        if (args.length == 0) {
            if (player.hasPermission("bingo.admin")) {
                new AdminBingoMenu(gameManager.getMenuManager(), session, config).open(player);
            } else if (player.hasPermission("bingo.player")) {
                new PlayerBingoMenu(gameManager.getMenuManager(), session).open(player);
            }
            return true;
        }

        switch (args[0]) {
            case "join" -> {
                TeamSelectionMenu menu = new TeamSelectionMenu(gameManager.getMenuManager(), session.teamManager);
                menu.open(player);
            }
            case "leave" -> {
                BingoParticipant participant = session.teamManager.getPlayerAsParticipant(player);
                if (participant != null)
                    session.removeParticipant(participant);
            }
            case "start" -> {
                if (player.hasPermission("bingo.settings")) {
                    if (args.length > 1) {
                        int seed = Integer.parseInt(args[1]);
                        session.settingsBuilder.cardSeed(seed);
                    }

                    session.startGame();
                    return true;
                }
            }
            case "end" -> {
                if (player.hasPermission("bingo.settings"))
                    session.endGame();
            }
            case "getcard" -> {
                if (session.isRunning()) {
                    BingoParticipant participant = session.teamManager.getPlayerAsParticipant(player);
                    if (participant instanceof BingoPlayer bingoPlayer)
                        ((BingoGame) session.phase()).returnCardToPlayer(bingoPlayer);
                    return true;
                }
            }
            case "back" -> {
                if (session.isRunning()) {
                    if (config.teleportAfterDeath) {
                        ((BingoGame) session.phase()).teleportPlayerAfterDeath(player);
                        return true;
                    }
                }
            }
            case "deathmatch" -> {
                if (!player.hasPermission("bingo.settings"))
                    return false;

                if (!session.isRunning()) {
                    new TranslatedMessage(BingoTranslation.NO_DEATHMATCH).color(ChatColor.RED).send(player);
                    return false;
                }

                ((BingoGame) session.phase()).startDeathMatch(3);
                return true;
            }
            case "creator" -> {
                if (player.hasPermission("bingo.manager")) {
                    BingoCreatorMenu creatorMenu = new BingoCreatorMenu(gameManager.getMenuManager());
                    creatorMenu.open(player);
                }
            }
            case "stats" -> {
                if (!config.savePlayerStatistics) {
                    TextComponent text = new TextComponent("Player statistics are not being tracked at this moment!");
                    text.setColor(ChatColor.RED);
                    Message.sendDebug(text, player);
                    return true;
                }
                BingoStatData statsData = new BingoStatData();
                Message msg;
                if (args.length > 1 && player.hasPermission("bingo.admin")) {
                    msg = statsData.getPlayerStatsFormatted(args[1]);
                } else {
                    msg = statsData.getPlayerStatsFormatted(player.getUniqueId());
                }
                msg.send(player);
                return true;
            }
            case "kit" -> {
                if (!player.hasPermission("bingo.manager"))
                    return false;
                if (args.length <= 2)
                    return false;

                switch (args[1]) {
                    case "item" -> givePlayerBingoItem(player, args[2]);
                    case "add" -> {
                        if (args.length < 4) {
                            Message.sendDebug(ChatColor.RED + "Please specify a kit name for slot " + args[2], player);
                            return false;
                        }
                        addPlayerKit(args[2], Arrays.stream(args).collect(Collectors.toList()).subList(3, args.length), player);
                    }
                    case "remove" -> removePlayerKit(args[2], player);
                }
            }
            case "teams" -> {
                if (!player.hasPermission("bingo.admin"))
                    return false;

                new TeamEditorMenu(gameManager.getMenuManager()).open(player);
            }
            case "hologram" -> {

            }
            default ->
                    new TranslatedMessage(BingoTranslation.COMMAND_USAGE).color(ChatColor.RED).arg("/bingo [getcard | stats | start | end | join | back | leave | deathmatch | creator | teams]").send(player);
        }
        return true;
    }

    public void addPlayerKit(String slot, List<String> kitNameParts, Player commandSender) {
        PlayerKit kit = switch (slot) {
            case "1" -> PlayerKit.CUSTOM_1;
            case "2" -> PlayerKit.CUSTOM_2;
            case "3" -> PlayerKit.CUSTOM_3;
            case "4" -> PlayerKit.CUSTOM_4;
            case "5" -> PlayerKit.CUSTOM_5;
            default -> {
                Message.sendDebug(ChatColor.RED + "Invalid slot, please a slot from 1 through 5 to save this kit in", commandSender);
                yield null;
            }
        };

        StringBuilder kitName = new StringBuilder();
        for (int i = 0; i < kitNameParts.size() - 1; i++) {
            kitName.append(kitNameParts.get(i)).append(" ");
        }
        kitName.append(kitNameParts.get(kitNameParts.size() - 1));

        if (!PlayerKit.assignCustomKit(kitName.toString(), kit, commandSender)) {
            BaseComponent msg = new TextComponent("");
            msg.setColor(ChatColor.RED);
            msg.addExtra("Cannot add custom kit ");
            msg.addExtra(BingoTranslation.convertColors(kitName.toString()));
            msg.addExtra(" to slot " + slot + ", this slot already contains kit ");
            msg.addExtra(PlayerKit.getCustomKit(kit).getName());
            msg.addExtra(". Remove it first!");
            Message.sendDebug(msg, commandSender);
        } else {
            BaseComponent msg = new TextComponent("");
            msg.setColor(ChatColor.GREEN);
            msg.addExtra("Created custom kit ");
            msg.addExtra(BingoTranslation.convertColors(kitName.toString()));
            msg.addExtra(" in slot " + slot + " from your inventory");
            Message.sendDebug(msg, commandSender);
        }
    }

    public void removePlayerKit(String slot, Player commandSender) {
        PlayerKit kit = switch (slot) {
            case "1" -> PlayerKit.CUSTOM_1;
            case "2" -> PlayerKit.CUSTOM_2;
            case "3" -> PlayerKit.CUSTOM_3;
            case "4" -> PlayerKit.CUSTOM_4;
            case "5" -> PlayerKit.CUSTOM_5;
            default -> {
                Message.sendDebug(ChatColor.RED + "Invalid slot, please a slot from 1 through 5 to save this kit in", commandSender);
                yield null;
            }
        };

        if (PlayerKit.getCustomKit(kit) == null) {
            BaseComponent msg = new TextComponent("");
            msg.setColor(ChatColor.RED);
            msg.addExtra("Cannot remove kit from slot " + slot + " because no custom kit is assigned to this slot");
            Message.sendDebug(msg, commandSender);
        } else {
            BaseComponent msg = new TextComponent("");
            msg.setColor(ChatColor.GREEN);
            msg.addExtra("Removed custom kit ");
            msg.addExtra(PlayerKit.getCustomKit(kit).getName());
            msg.addExtra(" from slot " + slot);
            Message.sendDebug(msg, commandSender);
            PlayerKit.removeCustomKit(kit);
        }
    }

    public void givePlayerBingoItem(Player player, String itemName) {
        if (itemName.equals("wand")) {
            player.getInventory().addItem(PlayerKit.WAND_ITEM);
        }
    }

    /**
     * @return Integer the string represents or defaultValue if a conversion failed.
     */
    public static int toInt(String in, int defaultValue) {
        try {
            return Integer.parseInt(in);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player player) || player.hasPermission("bingo.admin")) {
            if (args.length <= 1) {
                return List.of("join", "getcard", "back", "leave", "stats", "end", "kit", "deathmatch", "creator", "teams");
            }

            switch (args[0]) {
                case "kit" -> {
                    if (args.length == 2) {
                        return List.of("add", "remove", "item");
                    }
                    if (args.length == 3) {
                        switch (args[1]) {
                            case "add", "remove" -> {
                                return List.of("1", "2", "3", "4", "5");
                            }
                            case "item" -> {
                                return List.of("wand");
                            }
                        }
                    }
                }
            }
            return List.of();
        }

        switch (args.length) {
            case 1 -> {
                return List.of("join", "getcard", "back", "leave", "stats");
            }
        }
        return List.of();
    }
}
