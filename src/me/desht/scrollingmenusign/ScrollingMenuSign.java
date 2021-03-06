package me.desht.scrollingmenusign;

import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.desht.register.payment.Method;
import me.desht.register.payment.Methods;
import me.desht.scrollingmenusign.commands.AddItemCommand;
import me.desht.scrollingmenusign.commands.AddMacroCommand;
import me.desht.scrollingmenusign.commands.AddViewCommand;
import me.desht.scrollingmenusign.commands.CommandManager;
import me.desht.scrollingmenusign.commands.CreateMenuCommand;
import me.desht.scrollingmenusign.commands.DebugCommand;
import me.desht.scrollingmenusign.commands.DefaultCmdCommand;
import me.desht.scrollingmenusign.commands.DeleteMenuCommand;
import me.desht.scrollingmenusign.commands.GetConfigCommand;
import me.desht.scrollingmenusign.commands.GiveMapCommand;
import me.desht.scrollingmenusign.commands.ItemUseCommand;
import me.desht.scrollingmenusign.commands.ListMacroCommand;
import me.desht.scrollingmenusign.commands.ListMenusCommand;
import me.desht.scrollingmenusign.commands.MenuTitleCommand;
import me.desht.scrollingmenusign.commands.PageCommand;
import me.desht.scrollingmenusign.commands.ReloadCommand;
import me.desht.scrollingmenusign.commands.RemoveItemCommand;
import me.desht.scrollingmenusign.commands.RemoveMacroCommand;
import me.desht.scrollingmenusign.commands.RemoveViewCommand;
import me.desht.scrollingmenusign.commands.SaveCommand;
import me.desht.scrollingmenusign.commands.SetConfigCommand;
import me.desht.scrollingmenusign.commands.ShowMenuCommand;
import me.desht.scrollingmenusign.commands.SortMenuCommand;
import me.desht.scrollingmenusign.listeners.SMSBlockListener;
import me.desht.scrollingmenusign.listeners.SMSEntityListener;
import me.desht.scrollingmenusign.listeners.SMSPlayerListener;
import me.desht.util.MessagePager;
import me.desht.util.MiscUtil;
import me.desht.util.PermissionsUtils;

import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;


public class ScrollingMenuSign extends JavaPlugin {
	private static PluginDescriptionFile description;

	private final SMSPlayerListener playerListener = new SMSPlayerListener(this);
	private final SMSBlockListener blockListener = new SMSBlockListener(this);
	private final SMSEntityListener entityListener = new SMSEntityListener(this);
	private final SMSHandlerImpl handler = new SMSHandlerImpl();
	private final CommandManager cmds = new CommandManager(this);

	private static Method economy = null;

	@Override
	public void onEnable() {
		description = this.getDescription();
		
		PluginManager pm = getServer().getPluginManager();
		
		if (!validateVersions(description.getVersion(), getServer().getVersion())) {		
			pm.disablePlugin(this);
			return;
		}	
		
		SMSPersistence.init();
		SMSConfig.init(this);
		PermissionsUtils.setup();
		SMSCommandSigns.setup();

		pm.registerEvent(Event.Type.PLAYER_INTERACT, playerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_ITEM_HELD, playerListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_DAMAGE, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_BREAK, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.BLOCK_PHYSICS, blockListener, Event.Priority.Normal, this);
		pm.registerEvent(Event.Type.ENTITY_EXPLODE, entityListener, Event.Priority.Normal, this);

		setupEconomy();

		registerCommands();

		loadMacros();

		MessagePager.setPageCmd("/sms page [#|n|p]");

		// delayed loading of saved menu files to ensure all worlds are loaded first
		if (getServer().getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
			@Override
			public void run() {
				loadMenusAndViews();
			}
		})==-1) {
			MiscUtil.log(Level.WARNING, "Couldn't schedule menu loading - multiworld support might not work.");
			loadMenusAndViews();
		}

		MiscUtil.log(Level.INFO, description.getName() + " version " + description.getVersion() + " is enabled!" );
	}

	private void setupEconomy() {
		Methods.setMethod(getServer().getPluginManager());

		if (Methods.getMethod() != null) {
			setEconomy(Methods.getMethod());
			MiscUtil.log(Level.INFO, String.format("Economy method found: %s v%s", getEconomy().getName(), getEconomy().getVersion()));
		}
	}

	private void registerCommands() {
		cmds.registerCommand(new AddItemCommand());
		cmds.registerCommand(new AddMacroCommand());
		cmds.registerCommand(new AddViewCommand());
		cmds.registerCommand(new CreateMenuCommand());
		cmds.registerCommand(new DebugCommand());
		cmds.registerCommand(new DefaultCmdCommand());
		cmds.registerCommand(new DeleteMenuCommand());
		cmds.registerCommand(new GetConfigCommand());
		cmds.registerCommand(new GiveMapCommand());
		cmds.registerCommand(new ItemUseCommand());
		cmds.registerCommand(new ListMacroCommand());
		cmds.registerCommand(new ListMenusCommand());
		cmds.registerCommand(new MenuTitleCommand());
		cmds.registerCommand(new PageCommand());
		cmds.registerCommand(new ReloadCommand());
		cmds.registerCommand(new RemoveItemCommand());
		cmds.registerCommand(new RemoveMacroCommand());
		cmds.registerCommand(new RemoveViewCommand());
		cmds.registerCommand(new SaveCommand());
		cmds.registerCommand(new SetConfigCommand());
		cmds.registerCommand(new ShowMenuCommand());
		cmds.registerCommand(new SortMenuCommand());
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		if (sender instanceof Player) {
			player = (Player) sender;
		}
		try {
			return cmds.dispatch(player, label, args);
		} catch (SMSException e) {
			MiscUtil.errorMessage(player, e.getMessage());
			return true;
		}
	}

	@Override
	public void onDisable() {
		saveMenus();
		saveMacros();
		MiscUtil.log(Level.INFO, description.getName() + " version " + description.getVersion() + " is disabled!" );
	}

	public void loadMenusAndViews() {
		SMSPersistence.loadAll();
	}

	public void loadMacros() {
		SMSMacro.loadCommands();
	}

	public void saveMenus() {
		SMSPersistence.saveAll();
	}

	public void saveMacros() {
		SMSMacro.saveCommands();
	}

	public SMSHandler getHandler() {
		return handler;
	}

	public static void setEconomy(Method economy) {
		ScrollingMenuSign.economy = economy;
	}

	public static Method getEconomy() {
		return economy;
	}
	
	boolean validateVersions(String pVer, String cbVer) {
		Pattern pat = Pattern.compile("b([0-9]+)jnks");
		Matcher m = pat.matcher(cbVer);
		int pluginBuild = getRelease(pVer);
		int bukkitBuild;
		if (m.find()) {
			bukkitBuild = Integer.parseInt(m.group(1));
		} else {
			MiscUtil.log(Level.WARNING, "Can't determine build number for CraftBukkit from " + cbVer);
			return true;
		}
		if (pluginBuild < 7000 && bukkitBuild >= 1093) {
			notCompatible(pVer, bukkitBuild, "0.7 or later");
			return false;
		} else if (pluginBuild >= 7000 && bukkitBuild < 1093) {
			notCompatible(pVer, bukkitBuild, "0.6.x earlier");
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * Get the internal version number for the given string version, which is
	 * <major> * 1,000,000 + <minor> * 1,000 + <release>.  This assumes minor and
	 * release each won't go above 999, hopefully a safe assumption!
	 * 
	 * @param oldVersion
	 * @return
	 */
	private static int getRelease(String ver) {
		String[] a = ver.split("\\.");
		try {
			int major = Integer.parseInt(a[0]);
			int minor;
			int rel;
			if (a.length < 2) {
				minor = 0;
			} else {
				minor = Integer.parseInt(a[1]);
			}
			if (a.length < 3) {
				rel = 0;
			} else {
				rel = Integer.parseInt(a[2]);
			}
			return major * 1000000 + minor * 1000 + rel;
		} catch (NumberFormatException e) {
			MiscUtil.log(Level.WARNING, "Version string [" + ver + "] doesn't look right!");
			return 0;
		}
	}

	private void notCompatible(String pVer, int bukkitBuild, String needed) {
		MiscUtil.log(Level.SEVERE, "ScrollingMenuSign v" + pVer + " is not compatible with CraftBukkit " + bukkitBuild + " - plugin disabled");
		MiscUtil.log(Level.SEVERE, "You need to use ScrollingMenuSign v" + needed);
	}
}
