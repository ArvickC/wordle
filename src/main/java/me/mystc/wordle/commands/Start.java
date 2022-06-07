package me.mystc.wordle.commands;

import me.mystc.wordle.Wordle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class Start implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player)) return false;
        if(Wordle.isGame) return false;

        Wordle.start((Player)sender);
        Wordle.move = 1;
        return true;
    }
}
