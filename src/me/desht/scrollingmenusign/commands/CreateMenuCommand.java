package me.desht.scrollingmenusign.commands;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import me.desht.scrollingmenusign.SMSException;
import me.desht.scrollingmenusign.SMSHandler;
import me.desht.scrollingmenusign.SMSMenu;
import me.desht.scrollingmenusign.ScrollingMenuSign;
import me.desht.scrollingmenusign.views.SMSMapView;
import me.desht.scrollingmenusign.views.SMSSignView;
import me.desht.util.MiscUtil;
import me.desht.util.PermissionsUtils;

public class CreateMenuCommand extends AbstractCommand {

	public CreateMenuCommand() {
		super("sms c", 1, 3);
		setPermissionNode("scrollingmenusign.commands.create");
		setUsage(new String[] { 
				"/sms create <menu> <title>",
				"/sms create <menu> from <other-menu>",
		});
	}

	@Override
	public boolean execute(ScrollingMenuSign plugin, Player player, String[] args) throws SMSException {
		String menuName = args[0];

		SMSHandler handler = plugin.getHandler();

		if (handler.checkMenu(menuName)) {
			throw new SMSException("A menu called '" + menuName + "' already exists.");
		}

		Location loc = null;
		short mapId = -1;
		String owner = "&console";	// dummy owner if menu created from console

		if (player != null) {
			owner = player.getName();
			Block b = player.getTargetBlock(null, 3);
			if (b.getType() == Material.SIGN_POST || b.getType() == Material.WALL_SIGN) {
				if (plugin.getHandler().getMenuNameAt(b.getLocation()) != null) {
					throw new SMSException("There is already a menu attached to that sign.");
				}
				owner = player.getName();
				loc = b.getLocation();
			} else if (player.getItemInHand().getTypeId() == 358) {
				PermissionsUtils.requirePerms(player, "scrollingmenusign.maps");
				mapId = player.getItemInHand().getDurability();
			}
		}

		SMSMenu menu = null;
		if (args.length == 3 && args[1].equals("from")) {
			SMSMenu otherMenu = plugin.getHandler().getMenu(args[2]);
			menu = handler.createMenu(menuName, otherMenu, owner);
		} else {
			String menuTitle = MiscUtil.parseColourSpec(player, combine(args, 1));
			menu = handler.createMenu(menuName, menuTitle, owner);
		}
		
		if (loc != null) {
			SMSSignView.addSignToMenu(menu, loc);
			MiscUtil.statusMessage(player, "Created new menu &e" + menuName + "&- with sign view @ &f" + MiscUtil.formatLocation(loc));
		} else if (mapId >= 0) {
			SMSMapView.addMapToMenu(mapId, menu);
			MiscUtil.statusMessage(player, "Created new menu &e" + menuName + "&- with map_" + mapId);
		} else {
			MiscUtil.statusMessage(player, "Created new menu &e" + menuName + "&- with no views");
		}

		return true;
	}

}
