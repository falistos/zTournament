package fr.maxlego08.ztournament.zcore.utils;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permissible;

import fr.maxlego08.ztournament.ZTournamentPlugin;
import fr.maxlego08.ztournament.zcore.ZPlugin;
import fr.maxlego08.ztournament.zcore.enums.Inventory;
import fr.maxlego08.ztournament.zcore.enums.Message;
import fr.maxlego08.ztournament.zcore.enums.Permission;
import fr.maxlego08.ztournament.zcore.utils.builder.CooldownBuilder;
import fr.maxlego08.ztournament.zcore.utils.builder.ItemBuilder;
import fr.maxlego08.ztournament.zcore.utils.builder.TimerBuilder;
import fr.maxlego08.ztournament.zcore.utils.nms.ItemStackUtils;
import fr.maxlego08.ztournament.zcore.utils.nms.NMSUtils;
import fr.maxlego08.ztournament.zcore.utils.players.ActionBar;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.HoverEvent.Action;
import net.md_5.bungee.api.chat.TextComponent;

@SuppressWarnings("deprecation")
public abstract class ZUtils extends MessageUtils {

	private static transient List<String> teleportPlayers = new ArrayList<String>();
	protected transient ZTournamentPlugin plugin = (ZTournamentPlugin) ZPlugin.z();

	protected Location changeStringLocationToLocation(String s) {
		String[] a = s.split(",");
		if (a.length == 6)
			return changeStringLocationToLocationEye(s);
		World w = Bukkit.getServer().getWorld(a[0]);
		float x = Float.parseFloat(a[1]);
		float y = Float.parseFloat(a[2]);
		float z = Float.parseFloat(a[3]);
		return new Location(w, x, y, z);
	}

	protected Location changeStringLocationToLocationEye(String s) {
		String[] a = s.split(",");
		World w = Bukkit.getServer().getWorld(a[0]);
		float x = Float.parseFloat(a[1]);
		float y = Float.parseFloat(a[2]);
		float z = Float.parseFloat(a[3]);
		if (a.length == 6) {
			float yaw = Float.parseFloat(a[4]);
			float pitch = Float.parseFloat(a[5]);
			return new Location(w, x, y, z, yaw, pitch);
		}
		return new Location(w, x, y, z);
	}

	protected String changeLocationToString(Location location) {
		String ret = location.getWorld().getName() + "," + location.getBlockX() + "," + location.getBlockY() + ","
				+ location.getBlockZ();
		return ret;
	}

	protected String changeLocationToStringEye(Location location) {
		String ret = location.getWorld().getName() + "," + location.getBlockX() + "," + location.getBlockY() + ","
				+ location.getBlockZ() + "," + location.getYaw() + "," + location.getPitch();
		return ret;
	}

	protected Chunk changeStringChuncToChunk(String chunk) {
		String[] a = chunk.split(",");
		World w = Bukkit.getServer().getWorld(a[0]);
		return w.getChunkAt(Integer.valueOf(a[1]), Integer.valueOf(a[2]));
	}

	protected String changeChunkToString(Chunk chunk) {
		String c = chunk.getWorld().getName() + "," + chunk.getX() + "," + chunk.getZ();
		return c;
	}

	protected String changeCuboidToString(Cuboid cuboid) {
		return cuboid.getWorld().getName() + "," + cuboid.getLowerX() + "," + cuboid.getLowerY() + ","
				+ cuboid.getLowerZ() + "," + ";" + cuboid.getWorld().getName() + "," + cuboid.getUpperX() + ","
				+ cuboid.getUpperY() + "," + cuboid.getUpperZ();
	}

	protected Cuboid changeStringToCuboid(String str) {
		String parsedCuboid[] = str.split(";");
		String parsedFirstLoc[] = parsedCuboid[0].split(",");
		String parsedSecondLoc[] = parsedCuboid[1].split(",");
		String firstWorldName = parsedFirstLoc[0];
		double firstX = Double.valueOf(parsedFirstLoc[1]);
		double firstY = Double.valueOf(parsedFirstLoc[2]);
		double firstZ = Double.valueOf(parsedFirstLoc[3]);
		String secondWorldName = parsedSecondLoc[0];
		double secondX = Double.valueOf(parsedSecondLoc[1]);
		double secondY = Double.valueOf(parsedSecondLoc[2]);
		double secondZ = Double.valueOf(parsedSecondLoc[3]);
		Location l1 = new Location(Bukkit.getWorld(firstWorldName), firstX, firstY, firstZ);
		Location l2 = new Location(Bukkit.getWorld(secondWorldName), secondX, secondY, secondZ);
		return new Cuboid(l1, l2);
	}

	protected String encode(ItemStack item) {
		return ItemStackUtils.serializeItemStack(item);
	}

	protected ItemStack decode(String item) {
		return ItemStackUtils.deserializeItemStack(item);
	}

	protected String betterMaterial(Material material) {
		return TextUtil.getMaterialLowerAndMajAndSpace(material);
	}

	protected int getNumberBetween(int a, int b) {
		return ThreadLocalRandom.current().nextInt(a, b);
	}

	protected boolean hasInventoryFull(Player player) {
		int slot = 0;
		ItemStack[] arrayOfItemStack;
		int x = (arrayOfItemStack = player.getInventory().getContents()).length;
		for (int i = 0; i < x; i++) {
			ItemStack contents = arrayOfItemStack[i];
			if ((contents == null))
				slot++;
		}
		return slot == 0;
	}

	protected boolean give(ItemStack item, Player player) {
		if (hasInventoryFull(player))
			return false;
		player.getInventory().addItem(item);
		return true;
	}

	protected void give(Player player, ItemStack item) {
		if (hasInventoryFull(player))
			player.getWorld().dropItem(player.getLocation(), item);
		else
			player.getInventory().addItem(item);
	}

	private static transient Material[] byId;

	static {
		if (!NMSUtils.isNewVersion()) {
			byId = new Material[0];
			for (Material material : Material.values()) {
				if (byId.length > material.getId()) {
					byId[material.getId()] = material;
				} else {
					byId = Arrays.copyOfRange(byId, 0, material.getId() + 2);
					byId[material.getId()] = material;
				}
			}
		}
	}

	protected ItemStack getGlass() {
		if (NMSUtils.isNewVersion()) {
			Material gray = Material.getMaterial("GRAY_STAINED_GLASS_PANE");
			if (gray != null) return new ItemStack(gray);
		}
		return new ItemBuilder(getMaterial(160), 1, 8).build();
	}

	public Material getMaterial(int id) {
		return byId.length > id && id >= 0 ? byId[id] : null;
	}

	protected boolean same(ItemStack stack, String name) {
		return stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()
				&& stack.getItemMeta().getDisplayName().equals(name);
	}

	protected boolean contains(ItemStack stack, String name) {
		return stack.hasItemMeta() && stack.getItemMeta().hasDisplayName()
				&& stack.getItemMeta().getDisplayName().contains(name);
	}

	protected void removeItemInHand(Player player) {
		removeItemInHand(player, 64);
	}

	protected void removeItemInHand(Player player, int how) {
		if (player.getItemInHand().getAmount() > how)
			player.getItemInHand().setAmount(player.getItemInHand().getAmount() - 1);
		else
			player.setItemInHand(new ItemStack(Material.AIR));
		player.updateInventory();
	}

	protected boolean same(Location l, Location l2) {
		return (l.getBlockX() == l2.getBlockX()) && (l.getBlockY() == l2.getBlockY())
				&& (l.getBlockZ() == l2.getBlockZ()) && l.getWorld().getName().equals(l2.getWorld().getName());
	}

	protected void teleport(Player player, int delay, Location location) {
		teleport(player, delay, location, null);
	}

	protected void teleport(Player player, int delay, Location location, Consumer<Boolean> cmd) {
		if (teleportPlayers.contains(player.getName())) {
			message(player, Message.TELEPORT_ERROR);
			return;
		}
		ScheduledExecutorService ses = Executors.newScheduledThreadPool(1);
		Location playerLocation = player.getLocation();
		AtomicInteger verif = new AtomicInteger(delay);
		teleportPlayers.add(player.getName());
		if (!location.getChunk().isLoaded())
			location.getChunk().load();
		ses.scheduleWithFixedDelay(() -> {
			if (!same(playerLocation, player.getLocation())) {
				message(player, Message.TELEPORT_MOVE);
				ses.shutdown();
				teleportPlayers.remove(player.getName());
				if (cmd != null)
					cmd.accept(false);
				return;
			}
			int currentSecond = verif.getAndDecrement();
			if (!player.isOnline()) {
				ses.shutdown();
				teleportPlayers.remove(player.getName());
				return;
			}
			if (currentSecond == 0) {
				ses.shutdown();
				teleportPlayers.remove(player.getName());
				player.teleport(location);
				message(player, Message.TELEPORT_SUCCESS);
				if (cmd != null)
					cmd.accept(true);
			} else
				message(player, Message.TELEPORT_MESSAGE, currentSecond);
		}, 0, 1, TimeUnit.SECONDS);
	}

	protected String format(double decimal) {
		return format(decimal, "#.##");
	}

	protected String format(double decimal, String format) {
		DecimalFormat decimalFormat = new DecimalFormat(format);
		return decimalFormat.format(decimal);
	}

	protected void removeItems(Player player, int item, ItemStack itemStack) {
		for (ItemStack is : player.getInventory().getContents()) {
			if (is != null && is.isSimilar(itemStack)) {
				int currentAmount = is.getAmount() - item;
				item -= is.getAmount();
				if (currentAmount <= 0)
					player.getInventory().removeItem(is);
				else
					is.setAmount(currentAmount);
			}
		}
		player.updateInventory();
	}

	protected void schedule(long delay, Runnable runnable) {
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				if (runnable != null)
					runnable.run();
			}
		}, delay);
	}

	protected String name(String string) {
		return TextUtil.name(string);
	}

	protected int getMaxPage(Collection<?> items) {
		return (items.size() / 45) + 1;
	}

	protected int getMaxPage(Collection<?> items, int a) {
		return (items.size() / a) + 1;
	}

	protected double percent(double value, double total) {
		return (double) ((value * 100) / total);
	}

	protected double percentNum(double total, double percent) {
		return (double) (total * (percent / 100));
	}

	protected void schedule(long delay, int count, Runnable runnable) {
		new Timer().scheduleAtFixedRate(new TimerTask() {
			int tmpCount = 0;
			@Override
			public void run() {
				if (!ZPlugin.z().isEnabled()) {
					cancel();
					return;
				}
				if (tmpCount > count) {
					cancel();
					return;
				}
				tmpCount++;
				Bukkit.getScheduler().runTask(ZPlugin.z(), runnable);
			}
		}, 0, delay);
	}

	protected void createInventory(Player player, Inventory inventory) {
		createInventory(player, inventory, 1);
	}

	protected void createInventory(Player player, Inventory inventory, int page) {
		createInventory(player, inventory, page, new Object() {});
	}

	protected void createInventory(Player player, Inventory inventory, int page, Object... objects) {
		plugin.getInventoryManager().createInventory(inventory, player, page, objects);
	}

	protected void createInventory(Player player, int inventory, int page, Object... objects) {
		plugin.getInventoryManager().createInventory(inventory, player, page, objects);
	}

	protected boolean hasPermission(Permissible permissible, Permission permission) {
		return permissible.hasPermission(permission.getPermission());
	}

	protected boolean hasPermission(Permissible permissible, String permission) {
		return permissible.hasPermission(permission);
	}

	protected void scheduleFix(long delay, BiConsumer<TimerTask, Boolean> runnable) {
		new Timer().scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (!ZPlugin.z().isEnabled()) {
					cancel();
					runnable.accept(this, false);
					return;
				}
				Bukkit.getScheduler().runTask(ZPlugin.z(), () -> runnable.accept(this, true));
			}
		}, delay, delay);
	}

	protected <T> T randomElement(List<T> element) {
		if (element.size() == 0)
			return null;
		if (element.size() == 1)
			return element.get(0);
		Random random = new Random();
		return element.get(random.nextInt(element.size() - 1));
	}

	protected String getItemName(ItemStack item) {
		if (item.hasItemMeta() && item.getItemMeta().hasDisplayName())
			return item.getItemMeta().getDisplayName();
		String name = item.serialize().get("type").toString().replace("_", " ").toLowerCase();
		return name.substring(0, 1).toUpperCase() + name.substring(1);
	}

	protected String color(String message) {
		return message != null ? message.replace("&", "§") : message;
	}

	public String colorReverse(String message) {
		return message != null ? message.replace("§", "&") : message;
	}

	protected List<String> color(List<String> messages) {
		return messages.stream().map(message -> color(message)).collect(Collectors.toList());
	}

	public List<String> colorReverse(List<String> messages) {
		return messages.stream().map(message -> colorReverse(message)).collect(Collectors.toList());
	}

	protected ItemFlag getFlag(String flagString) {
		for (ItemFlag flag : ItemFlag.values()) {
			if (flag.name().equalsIgnoreCase(flagString))
				return flag;
		}
		return null;
	}

	protected <T> List<T> reverse(List<T> list) {
		List<T> tmpList = new ArrayList<>();
		for (int index = list.size() - 1; index != -1; index--)
			tmpList.add(list.get(index));
		return tmpList;
	}

	protected String price(long price) {
		return String.format("%,d", price);
	}

	protected String generateRandomString(int length) {
		int leftLimit = 97;
		int rightLimit = 122;
		int targetStringLength = 5;
		Random random = new Random();
		StringBuilder buffer = new StringBuilder(targetStringLength);
		for (int i = 0; i < targetStringLength; i++) {
			int randomLimitedInt = leftLimit + (int) (random.nextFloat() * (rightLimit - leftLimit + 1));
			buffer.append((char) randomLimitedInt);
		}
		return buffer.toString();
	}

	protected TextComponent buildTextComponent(String message) {
		return new TextComponent(message);
	}

	protected TextComponent setHoverMessage(TextComponent component, String... messages) {
		BaseComponent[] list = new BaseComponent[messages.length];
		for (int a = 0; a != messages.length; a++)
			list[a] = new TextComponent(messages[a] + (messages.length - 1 == a ? "" : "\n"));
		component.setHoverEvent(new HoverEvent(Action.SHOW_TEXT, list));
		return component;
	}

	protected TextComponent setHoverMessage(TextComponent component, List<String> messages) {
		BaseComponent[] list = new BaseComponent[messages.size()];
		for (int a = 0; a != messages.size(); a++)
			list[a] = new TextComponent(messages.get(a) + (messages.size() - 1 == a ? "" : "\n"));
		component.setHoverEvent(new HoverEvent(Action.SHOW_TEXT, list));
		return component;
	}

	protected TextComponent setClickAction(TextComponent component, net.md_5.bungee.api.chat.ClickEvent.Action action,
			String command) {
		component.setClickEvent(new ClickEvent(action, command));
		return component;
	}

	protected void removeItems(org.bukkit.inventory.Inventory inventory, ItemStack removeItemStack, int amount) {
		for (ItemStack itemStack : inventory.getContents()) {
			if (itemStack != null && itemStack.isSimilar(itemStack) && amount > 0) {
				int currentAmount = itemStack.getAmount() - amount;
				amount -= itemStack.getAmount();
				if (currentAmount <= 0)
					inventory.removeItem(itemStack);
				else
					itemStack.setAmount(currentAmount);
			}
		}
	}

	protected String getDisplayBalence(double value) {
		if (value < 10000)
			return format(value, "#.#");
		else if (value < 1000000)
			return String.valueOf(Integer.valueOf((int) (value / 1000))) + "k ";
		else if (value < 1000000000)
			return String.valueOf(format((value / 1000) / 1000, "#.#")) + "m ";
		else if (value < 1000000000000l)
			return String.valueOf(Integer.valueOf((int) (((value / 1000) / 1000) / 1000))) + "M ";
		else
			return "to much";
	}

	protected String getDisplayBalence(long value) {
		if (value < 10000)
			return format(value, "#.#");
		else if (value < 1000000)
			return String.valueOf(Integer.valueOf((int) (value / 1000))) + "k ";
		else if (value < 1000000000)
			return String.valueOf(format((value / 1000) / 1000, "#.#")) + "m ";
		else if (value < 1000000000000l)
			return String.valueOf(Integer.valueOf((int) (((value / 1000) / 1000) / 1000))) + "M ";
		else
			return "to much";
	}

	protected int count(org.bukkit.inventory.Inventory inventory, Material material) {
		int count = 0;
		for (ItemStack itemStack : inventory.getContents())
			if (itemStack != null && itemStack.getType().equals(material))
				count += itemStack.getAmount();
		return count;
	}

	protected Enchantment enchantFromString(String str) {
		for (Enchantment enchantment : Enchantment.values())
			if (enchantment.getName().equalsIgnoreCase(str))
				return enchantment;
		return null;
	}

	protected BlockFace getClosestFace(float direction) {
		direction = direction % 360;
		if (direction < 0)
			direction += 360;
		direction = Math.round(direction / 45);
		switch ((int) direction) {
		case 0: return BlockFace.WEST;
		case 1: return BlockFace.NORTH_WEST;
		case 2: return BlockFace.NORTH;
		case 3: return BlockFace.NORTH_EAST;
		case 4: return BlockFace.EAST;
		case 5: return BlockFace.SOUTH_EAST;
		case 6: return BlockFace.SOUTH;
		case 7: return BlockFace.SOUTH_WEST;
		default: return BlockFace.WEST;
		}
	}

	protected String betterPrice(long price) {
		String betterPrice = "";
		String[] splitPrice = String.valueOf(price).split("");
		int current = 0;
		for (int a = splitPrice.length - 1; a > -1; a--) {
			current++;
			if (current > 3) {
				betterPrice += ".";
				current = 1;
			}
			betterPrice += splitPrice[a];
		}
		StringBuilder builder = new StringBuilder().append(betterPrice);
		builder.reverse();
		return builder.toString();
	}

	protected boolean hasEnchant(Enchantment enchantment, ItemStack itemStack) {
		return itemStack.hasItemMeta() && itemStack.getItemMeta().hasEnchants()
				&& itemStack.getItemMeta().hasEnchant(enchantment);
	}

	protected String timerFormat(Player player, String cooldown) {
		return TimerBuilder.getStringTime(CooldownBuilder.getCooldownPlayer(cooldown, player) / 1000);
	}

	protected boolean isCooldown(Player player, String cooldown) {
		return isCooldown(player, cooldown, 0);
	}

	protected boolean isCooldown(Player player, String cooldown, int timer) {
		if (CooldownBuilder.isCooldown(cooldown, player)) {
			ActionBar.sendActionBar(player,
					String.format("§cVous devez attendre encore §6%s §cavant de pouvoir faire cette action.",
							timerFormat(player, cooldown)));
			return true;
		}
		if (timer > 0)
			CooldownBuilder.addCooldown(cooldown, player, timer);
		return false;
	}

	protected String toList(Stream<String> list) {
		return toList(list.collect(Collectors.toList()), "§e", "§6");
	}

	protected String toList(List<String> list) {
		return toList(list, "§e", "§6§n");
	}

	protected String toList(List<String> list, String color, String color2) {
		if (list == null || list.size() == 0)
			return null;
		if (list.size() == 1)
			return list.get(0);
		String str = "";
		for (int a = 0; a != list.size(); a++) {
			if (a == list.size() - 1 && a != 0)
				str += color + " et " + color2;
			else if (a != 0)
				str += color + ", " + color2;
			str += list.get(a);
		}
		return str;
	}

	public String removeColor(String message) {
		for (ChatColor color : ChatColor.values())
			message = message.replace("§" + color.getChar(), "").replace("&" + color.getChar(), "");
		return message;
	}

	public String format(long l) {
		return format(l, ' ');
	}

	public String format(long l, char c) {
		DecimalFormat formatter = (DecimalFormat) NumberFormat.getInstance(Locale.US);
		DecimalFormatSymbols symbols = formatter.getDecimalFormatSymbols();
		symbols.setGroupingSeparator(c);
		formatter.setDecimalFormatSymbols(symbols);
		return formatter.format(l);
	}

}
