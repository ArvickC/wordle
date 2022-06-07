package me.mystc.wordle.commands;

import me.mystc.wordle.Wordle;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.IOException;

public class Stop implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player)) return false;
        if(!Wordle.isGame) return false;

        try {
            Wordle.stop();
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[&aW&8or&6d&al&8e&7] Stopping wordle!"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
}