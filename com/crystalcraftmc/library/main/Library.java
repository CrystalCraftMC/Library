package com.crystalcraftmc.library.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.World.Environment;

public class Library extends JavaPlugin implements Listener {
	
	/**Holds library area*/
	int[] libraryArea = new int[6];
	
	/**Holds different types of inventories*/
	public enum InventoryResult {
		CLEAR, POLLUTED, ARMOR_POLLUTION
	}
	
	/**Lists of ppl who have library perms*/
	ArrayList<String> libraryPerms = new ArrayList<String>();
	
	/**Items to de-powertool*/
	ArrayList<ItemStack> noPT = new ArrayList<ItemStack>();
	
	/**List of valid commands inside library*/
	String[] validCommands = {"spawn", "home", "warp"};
	
	public void onEnable() {
		this.initializeLibraryArea();
		this.initializeLibraryPerms();
		this.getServer().getPluginManager().registerEvents(this, this);
		noPT.add(new ItemStack(Material.COOKED_BEEF, 1));
		noPT.add(new ItemStack(Material.WRITTEN_BOOK, 1));
		
	}
	public void onDisable() {
		
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(sender instanceof Player) {
			Player p = (Player)sender;
			if(this.hasLibraryPerms(p)) {
				if(label.equalsIgnoreCase("createlibrary")) {
					if(args.length == 6) {
						for(int i = 0; i < 6; i++) {
							if(!this.isInt(args[i])) {
								p.sendMessage(ChatColor.RED + "Error; your 6 arguments were not all valid " +
										"int values");
								return false;
							}
						}
						for(int i = 0; i < 6; i++) {
							libraryArea[i] = Integer.parseInt(args[i]);
						}
						this.updateLibraryArea();
						p.sendMessage("Library area updated.");
						this.showLibrary(p);
						return true;
					}
					else {
						return false;
					}
				}
				else if(label.equalsIgnoreCase("deletelibrary")) {
					if(args.length == 0) {
						for(int i = 0; i < 6; i++)
							libraryArea[i] = 1234567899;
						this.updateLibraryArea();
						return true;
					}
					else {
						return false;
					}
				}
				else if(label.equalsIgnoreCase("showlibrary")) {
					this.showLibrary(p);
					return true;
				}
				else if(label.equalsIgnoreCase("libraryperms")) {
					if(args.length == 0) {
						p.sendMessage(ChatColor.DARK_AQUA + "List of players with Library Perms:");
						for(int i = 0; i < libraryPerms.size(); i++) {
							if(i%2 == 0)
								p.sendMessage(ChatColor.AQUA + libraryPerms.get(i));
							else
								p.sendMessage(ChatColor.BLUE + libraryPerms.get(i));
						}
						return true;
					}
					else if(args.length == 2) {
						if(args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove")) {
							boolean isAdd = args[0].equalsIgnoreCase("add") ? true : false;
							if(isAdd) {
								for(String check : libraryPerms) {
									if(check.equalsIgnoreCase(args[1])) {
										p.sendMessage(ChatColor.RED + "Error; " + ChatColor.GOLD + args[1] +
											ChatColor.RED + " is already in the Library Perms list.");
										p.sendMessage(ChatColor.DARK_AQUA + "Use /libraryperms to list players " +
											"who currently have library perms.");
										return true;
									}
								}
								libraryPerms.add(args[1]);
								p.sendMessage(ChatColor.GOLD + args[1] + ChatColor.DARK_AQUA + " added to " +
										"the library perms list.");
								this.updatePerms();
								return true;
							}
							else {
								for(int i = 0; i < libraryPerms.size(); i++) {
									if(libraryPerms.get(i).equalsIgnoreCase(args[1])) {
										libraryPerms.remove(i);
										p.sendMessage(ChatColor.DARK_AQUA + "Player successfully removed.");
										this.updatePerms();
										return true;
									}
								}
								p.sendMessage(ChatColor.RED + "Error; " + ChatColor.GOLD + args[1]+
										ChatColor.RED + " was not found in the Library Perms list.");
								p.sendMessage(ChatColor.DARK_AQUA + "Use /libraryperms to list players " +
											"who currently have library perms.");
								return true;
							}
						}
						else {
							p.sendMessage(ChatColor.RED + "Error; your first argument was not " +
									"\'add\' or \'remove\'.");
							return false;
						}
					}
					return false;
				}
			}
		}
		return false;
	}
	
	/**Tests whether a player has library perms
	 * @param p, the Player we're testing
	 * @return boolean, true if they have permissions
	 */
	public boolean hasLibraryPerms(Player p) {
		if(p.isOp())
			return true;
		String name = p.getName();
		for(String id : libraryPerms) {
			if(name.equals(id))
				return true;
		}
		return false;
	}
	
	/**Tests that a String is a valid int value
	 * @param String the string we're testing
	 * @return boolean; true if the string is an int
	 */
	public boolean isInt(String str) {
		try{
			Integer.parseInt(str);
			return true;
		}catch(NumberFormatException e) { return false; }
	}
	
	/**Initializes the library area from the file*/
	public void initializeLibraryArea() {
		if(!new File("LibraryFiles").exists())
			new File("LibraryFiles").mkdir();
		File file = new File("LibraryFiles\\Library.txt");
		try{
			if(!file.exists()) {
				PrintWriter pw = new PrintWriter("LibraryFiles\\Library.txt");
				for(int i = 0; i < 6; i++) {
					pw.println("1234567899");
					libraryArea[i] = 1234567899;
				}
				pw.close();
			}
			else {
				Scanner in = new Scanner(file);
				for(int i = 0; i < 6; i++) {
					libraryArea[i] = Integer.parseInt(in.nextLine());
				}
				in.close();
			}
		}catch(IOException e) { e.printStackTrace(); }
	}
	
	/**Updates the library area file*/
	public void updateLibraryArea() {
		try{
			if(!new File("LibraryFiles").exists())
				new File("LibraryFiles").mkdir();
			PrintWriter pw = new PrintWriter("LibraryFiles\\Library.txt");
			for(int i = 0; i < 6; i++)
				pw.println(String.valueOf(libraryArea[i]));
			pw.close();
		}catch(IOException e) { e.printStackTrace(); }
	}
	
	/**This initializes the library permissions file*/
	public void initializeLibraryPerms() {
		try{
			if(!new File("LibraryFiles").exists())
				new File("LibraryFiles").mkdir();
			File file = new File("LibraryFiles\\LibraryPerms.ser");
			if(file.exists()) {
				FileInputStream fis = new FileInputStream(file);
				ObjectInputStream ois = new ObjectInputStream(fis);
				libraryPerms = (ArrayList<String>)ois.readObject();
				ois.close();
				fis.close();
			}
		}catch(IOException e) { e.printStackTrace(); 
		}catch(ClassNotFoundException e) { e.printStackTrace(); }
	}
	
	/**This updates the library permissions file*/
	public void updatePerms() {
		try{
			if(!new File("LibraryFiles").exists())
				new File("LibraryFiles").mkdir();
			File file = new File("LibraryFiles\\LibraryPerms.ser");
			if(file.exists())
				file.delete();
			FileOutputStream fos = new FileOutputStream(file);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(libraryPerms);
			oos.close();
			fos.close();
		}catch(IOException e) { e.printStackTrace(); }
	}
	
	/**Displays the current library area
	 * @param player, the player we're showing the library area to
	 */
	public void showLibrary(Player p) {
		p.sendMessage(ChatColor.BLUE + "Coordinates are formatted as (x, y, z)");
		p.sendMessage(ChatColor.DARK_AQUA + "Corner 1: " + ChatColor.DARK_PURPLE + 
				"(" + ChatColor.GOLD + String.valueOf(libraryArea[0]) +
					", " + String.valueOf(libraryArea[1]) + ", " + String.valueOf(libraryArea[2]) +
					ChatColor.DARK_PURPLE + ")");
		p.sendMessage(ChatColor.DARK_AQUA + "Corner 2: " + ChatColor.DARK_PURPLE + 
				"(" + ChatColor.GOLD + String.valueOf(libraryArea[3]) +
					", " + String.valueOf(libraryArea[4]) + ", " + String.valueOf(libraryArea[5]) +
					ChatColor.DARK_PURPLE + ")");
		p.sendMessage(ChatColor.GOLD + "Remember, the library has to be in the overworld.");
	}
	
	@EventHandler
	public void libraryTP(PlayerTeleportEvent e) {
		if(this.hasLibraryPerms(e.getPlayer()))
				return;
		//1. clear inventory if tping out of library
		//2. permit tping into library only if clear inventory
		if(isInsideLibrary(e.getPlayer().getLocation()) && 
				e.getPlayer().getWorld().getEnvironment() == Environment.NORMAL) {
			e.getPlayer().getInventory().clear();
		}
		else if(isInsideLibrary(e.getTo())) {
			Player p = e.getPlayer();
			InventoryResult ir = this.testInventory(p);
			if(ir == InventoryResult.ARMOR_POLLUTION) {
				p.sendMessage(ChatColor.GOLD + "Error; you need a clear inventory " +
						"to enter the library (armor slots included)");
				e.setCancelled(true);
			}
			else if(ir == InventoryResult.POLLUTED) {
				p.sendMessage(ChatColor.GOLD + "Error; you need a clear inventory " +
						"to enter the library.");
				e.setCancelled(true);
			}
			else if(ir == InventoryResult.CLEAR) {
				for(ItemStack clear : noPT) {
					e.getPlayer().setItemInHand(clear);
					e.getPlayer().performCommand("pt");
					e.getPlayer().getInventory().clear();
				}	
			}
		}
	}
	
	@EventHandler
	public void giveBook(PlayerInteractEvent e) {
		if(e.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;
		if(e.getClickedBlock() == null)
			return;
		if(e.getClickedBlock().getType() == null)
			return;
			if((e.getClickedBlock().getType() == Material.STONE_BUTTON ||
					  e.getClickedBlock().getType() == Material.WOOD_BUTTON)) {
				if(isInsideLibrary(e.getClickedBlock().getLocation())) {
				Block clicked = e.getClickedBlock();
				
				Block chestB = null;
				
				if(new Location(clicked.getWorld(), clicked.getX(),
								clicked.getY(), clicked.getZ()-2).getBlock().getType() == Material.CHEST) {
					chestB = new Location(clicked.getWorld(), clicked.getX(),
							clicked.getY(), clicked.getZ()-2).getBlock();
				}
				if(new Location(clicked.getWorld(), clicked.getX(),
						clicked.getY(), clicked.getZ()+2).getBlock().getType() == Material.CHEST) {
					chestB = new Location(clicked.getWorld(), clicked.getX(),
							clicked.getY(), clicked.getZ()+2).getBlock();
				}
				if(new Location(clicked.getWorld(), clicked.getX()+2,
						clicked.getY(), clicked.getZ()).getBlock().getType() == Material.CHEST) {
					chestB = new Location(clicked.getWorld(), clicked.getX()+2,
							clicked.getY(), clicked.getZ()).getBlock();
				}
				if(new Location(clicked.getWorld(), clicked.getX()-2,
						clicked.getY(), clicked.getZ()).getBlock().getType() == Material.CHEST) {
					chestB = new Location(clicked.getWorld(), clicked.getX()-2,
							clicked.getY(), clicked.getZ()).getBlock();
				}
				
				
				if(chestB != null) {
					Chest chest = (Chest)chestB.getState();
					Inventory inv = chest.getInventory();
					ItemStack[] slots = inv.getContents();
					ItemStack book = null;
					for(ItemStack test : slots) {
						if(test != null) {
							if(test.getType() != null) {
								if(test.getType() != Material.AIR) {
									if(test.getType() == Material.WRITTEN_BOOK) {
										book = test;
										break;
									}
								}
							}
						}
					}
					if(book != null) {
						final ItemStack book2 = book;
						final Player p = e.getPlayer();
						this.getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
							public void run() {
								p.getInventory().addItem(book2);
							}
						}, 2L);
					}
				}
			}
		}
	}
	
	/**Checks whether a given coordinate is inside of the libraryarea
	 * @param Location, the location we're teleporting to
	 * @return boolean, true if the coordinates are inside the area
	 */
	public boolean isInsideLibrary(Location loc) {
		if(loc.getWorld().getEnvironment() != Environment.NORMAL)
			return false;
		int x = (int)loc.getX();
		int y = (int)loc.getY();
		int z = (int)loc.getZ();
		int lowX, highX, lowY, highY, lowZ, highZ;
		if(libraryArea[0] < libraryArea[3]) {
			lowX = libraryArea[0];
			highX = libraryArea[3];
		}
		else {
			lowX = libraryArea[3];
			highX = libraryArea[0];
		}
		
		if(libraryArea[1] < libraryArea[4]) {
			lowY = libraryArea[1];
			highY = libraryArea[4];
		}
		else {
			lowY = libraryArea[4];
			highY = libraryArea[1];
		}
		
		if(libraryArea[2] < libraryArea[5]) {
			lowZ = libraryArea[2];
			highZ = libraryArea[5];
		}
		else {
			lowZ = libraryArea[5];
			highZ = libraryArea[2];
		}
		
		if(x >= lowX && x <= highX &&
				y >= lowY && y <= highY &&
				z >= lowZ && z <= highZ) {
			return true;
		}
		return false;
	}
	
	
	/**This method checks the inventory of the player; as to
	 * whether it's clear, or is polluted, or has armor pollution
	 * @param p the player we're testing
	 * @return InventoryResult result of this test
	 */
	public InventoryResult testInventory(Player p) {
		PlayerInventory pi = p.getInventory();
		if(pi.getHelmet() != null)
			return InventoryResult.ARMOR_POLLUTION;
		if(pi.getChestplate() != null)
			return InventoryResult.ARMOR_POLLUTION;
		if(pi.getLeggings() != null)
			return InventoryResult.ARMOR_POLLUTION;
		if(pi.getBoots() != null)
			return InventoryResult.ARMOR_POLLUTION;
		
		ItemStack is[] = pi.getContents();
		for(int i = 0; i < is.length; i++) {
			if(is[i] != null) {
				if(is[i].getType() != Material.AIR) {
					return InventoryResult.POLLUTED;
				}
			}
		}
		return InventoryResult.CLEAR;
	}
	
	@EventHandler
	public void noBreak(BlockBreakEvent e) {
		if(isInsideLibrary(e.getBlock().getLocation())) {
			if(!this.hasLibraryPerms(e.getPlayer())) {
				e.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void noPlace(BlockPlaceEvent e) {
		if(isInsideLibrary(e.getBlock().getLocation())) {
			if(!this.hasLibraryPerms(e.getPlayer())) {
				e.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void noCommand(PlayerCommandPreprocessEvent e) {
			if(!this.hasLibraryPerms(e.getPlayer()) &&
					this.isInsideLibrary(e.getPlayer().getLocation())) {
			String cmd = e.getMessage();
			if(cmd == null)
				return;
			cmd = cmd.indexOf(" ") != -1 ? cmd.substring(0, cmd.indexOf(" ")) : cmd;
			if(cmd.charAt(0) == '/' && cmd.length() > 1)
				cmd = cmd.substring(1);
			for(int i = 0; i < validCommands.length; i++) {
				if(cmd.equalsIgnoreCase(validCommands[i]))
					return;
			}
			e.setCancelled(true);
			e.getPlayer().sendMessage(ChatColor.DARK_AQUA + "You are only permitted to use /spawn, /home, and " +
					"/warp while in the library.");
		}
	}
	
	@EventHandler
	public void blastProt(EntityExplodeEvent e) {
		if(e.getLocation().getWorld().getEnvironment() == Environment.NORMAL){
			if(this.isInsideSnowball(e.getLocation()))
				e.setCancelled(true); 		//impenetrable against tnt cannons
		}
	}
	
}
