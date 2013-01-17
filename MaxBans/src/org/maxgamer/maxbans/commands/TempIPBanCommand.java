package org.maxgamer.maxbans.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.maxgamer.maxbans.MaxBans;
import org.maxgamer.maxbans.banmanager.IPBan;
import org.maxgamer.maxbans.banmanager.TempIPBan;
import org.maxgamer.maxbans.util.Formatter;
import org.maxgamer.maxbans.util.Util;

public class TempIPBanCommand implements CommandExecutor{
    private MaxBans plugin;
    public TempIPBanCommand(MaxBans plugin){
        this.plugin = plugin;
    }
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(!sender.hasPermission("maxbans.tempipban")){
			sender.sendMessage(Formatter.secondary + "You don't have permission to do that");
			return true;
		}
		String usage = Formatter.secondary + "Usage: /tempipban <player> <time> <timeform> [-s] <reason>";
		
		if(args.length > 2){
			boolean silent = Util.isSilent(args);
			String name = args[0];
			if(name.isEmpty()){
				sender.sendMessage(Formatter.primary + " No name given.");
				return true;
			}
			
			//Get expirey time
			long time = Util.getTime(args);
			if(time <= 0){
				sender.sendMessage(usage);
				return true;
			}
			time += System.currentTimeMillis();
			
			//Build the reason
			String reason = Util.buildReason(args);
			String banner = Util.getName(sender);
			
			String ip;
			if(!Util.isIP(name)){
				name = plugin.getBanManager().match(name);
				if(name == null){
					name = args[0]; //Use exact name then.
				}
				//Fetch their IP address from history
				ip = plugin.getBanManager().getIP(name);
				
				if(ip == null){
					sender.sendMessage(Formatter.secondary + "No IP recorded for " + name + " - Try ban them normally instead?");
					return true;
				}
				
				plugin.getBanManager().tempban(name, reason, banner, time); //User
				
				//Kick them
				Player player = Bukkit.getPlayerExact(name);
				if(player != null && player.isOnline()){
					player.kickPlayer("You have been Temporarily IP Banned for: \n"+reason+"\nBy " + banner + ". Expires in " + Util.getTimeUntil(time));
				}
			}
			else{
				ip = name;
			}
			
			//Make sure the ban isnt redundant
			IPBan ban = plugin.getBanManager().getIPBan(ip);
			if(ban != null){
				if(ban instanceof TempIPBan){
					//They're already tempbanned!
					
					TempIPBan tBan = (TempIPBan) ban;
					if(tBan.getExpires() > time){
						//Their old ban lasts longer than this one!
						sender.sendMessage(Formatter.secondary + "That player has a tempban which will last longer than the one you supplied!");
						return true;
					}
					else{
						//Increasing a previous ban, remove the old one first.
						plugin.getBanManager().unbanip(ip);
					}
				}
				else{
					//Already perma banned
					sender.sendMessage(Formatter.secondary + "That player is already banned.");
					return true;
				}
			}
			
			//Ban them
			plugin.getBanManager().tempipban(ip, reason, banner, time); //IP
			
			//Notify online players
			if(!silent){
				//Announce
				plugin.getBanManager().announce(Formatter.secondary + name + Formatter.primary + " has been temp IP banned ("+Util.getTimeUntil(time)+") by " + Formatter.secondary + banner + Formatter.primary + ". Reason: " + Formatter.secondary + reason + ".");
			}
			else{
				//Silent
				sender.sendMessage(ChatColor.ITALIC + "" + Formatter.secondary + name + Formatter.primary + " has been silently temp IP banned ("+Util.getTimeUntil(time/1000*1000)+") by " + Formatter.secondary + banner + Formatter.primary + ". Reason: " + Formatter.secondary + reason + ".");
			}
			
			return true;
		}
		else{
			sender.sendMessage(usage);
			return true;
		}
	}
}
