package me.desht.scrollingmenusign.commands;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.SMSUtils;
import me.desht.scrollingmenusign.ScrollingMenuSign;

import org.bukkit.entity.Player;

public class MenuTitleCommand extends AbstractCommand {

	public MenuTitleCommand() {
		super("sms t", 2);
		setPermissionNode("scrollingmenusign.commands.title");
		setUsage("/sms title <menu-name> <new-title>");
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {
		SMSMenu menu = plugin.getHandler().getMenu(args[0]);
		String title = combine(args, 1);
		menu.setTitle(SMSUtils.parseColourSpec(player, title));
		menu.updateSigns();
		
		SMSUtils.statusMessage(player, "Title for menu &e" + menu.getName() + "&- has been set to &f" + title + "&-.");

		return true;
	}

}
