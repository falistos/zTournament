package fr.maxlego08.ztournament;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;

import fr.maxlego08.ztournament.api.Duel;
import fr.maxlego08.ztournament.api.Selection;
import fr.maxlego08.ztournament.api.Team;
import fr.maxlego08.ztournament.api.Tournament;
import fr.maxlego08.ztournament.listener.ListenerAdapter;
import fr.maxlego08.ztournament.save.Config;
import fr.maxlego08.ztournament.zcore.ZPlugin;
import fr.maxlego08.ztournament.zcore.enums.Message;
import fr.maxlego08.ztournament.zcore.enums.Permission;
import fr.maxlego08.ztournament.zcore.utils.ZSelection;
import fr.maxlego08.ztournament.zcore.utils.builder.ItemBuilder;
import fr.maxlego08.ztournament.zcore.utils.nms.NMSUtils;

@SuppressWarnings("deprecation")
public class TournamentListener extends ListenerAdapter {

	private final Tournament tournament;
	private final transient String itemName = "§6✤ §zTournament axe §6✤";
	private final transient Map<UUID, Selection> selections = new HashMap<UUID, Selection>();
	private final transient List<Entity> entities = new ArrayList<Entity>();

	/**
	 * @param tournament
	 */
	public TournamentListener(Tournament tournament) {
		super();
		this.tournament = tournament;
	}

	public Map<UUID, Selection> getSelections() {
		return selections;
	}

	/**
	 * 
	 * @param uuid
	 * @return
	 */
	public Optional<Selection> getSelection(UUID uuid) {
		return Optional.ofNullable(this.selections.getOrDefault(uuid, null));
	}

	public ItemStack getAxe() {
		ItemBuilder builder = new ItemBuilder(Material.STONE_AXE, itemName);
		builder.addLine("§8§m-+------------------------------+-");
		builder.addLine("");
		builder.addLine("§f§l» §7Allows you to select a zone to create a koth");
		builder.addLine(" §7§oYou must select an area with the right click");
		builder.addLine(" §7§oand left then do the command /tournament arena <name>");
		builder.addLine("");
		builder.addLine("§8§m-+------------------------------+-");
		return builder.build();
	}

	@Override
	protected void onMove(PlayerMoveEvent event, Player player) {

		if (!tournament.isStart())
			return;

		if (event.getTo().getY() < 0) {

			Team team = tournament.getByPlayer(player);
			if (team != null) {
				Duel duel = tournament.getDuel(team);
				if (duel != null) {
					Location location = duel.getArena().getPos1();
					event.setTo(location);
					message(player, Message.TOURNAMENT_LEAVE_ARENA);

				}
			}
		}
	}

	@Override
	public void onPickUp(PlayerPickupItemEvent event, Player player) {
		if (Config.enableDropItems && entities.contains(event.getItem()))
			entities.remove(event.getItem());
	}

	@Override
	public void onDrop(PlayerDropItemEvent event, Player player) {

		if (tournament.isStart() && !tournament.isWaiting()) {
			Team team = tournament.getByPlayer(player);

			if (team == null)
				return;

			if (Config.enableDropItems) {
				entities.add(event.getItemDrop());
			} else {
				event.setCancelled(true);
				message(player, Message.DROP_ITEM);
			}

		}

	}

	@Override
	public void onPlayerDamageLow(EntityDamageByEntityEvent event, DamageCause cause, double damage, Player player,
			Player entity) {

		if (tournament.isStart() && !tournament.isWaiting()) {

			Team team = tournament.getByPlayer(player);

			if (team == null) {
				return;
			}

			if (!team.isAlive(player)) {
				return;
			}

			if (!team.isInDuel()) {
				return;
			}

			// on cancel l'event pour bypass tout les autres
			event.setCancelled(true);

		}

	}

	@Override
	public void onPlayerDamageByArrow(EntityDamageByEntityEvent event, DamageCause cause, double damage,
			Projectile projectile, Player player) {

		if (this.tournament.isStart() && !this.tournament.isWaiting()) {

			Team team = tournament.getByPlayer(player);
			ProjectileSource source = projectile.getShooter();

			if (team != null) {

				if (source instanceof Player) {

					Player damager = (Player) source;
					Team damagerTeam = this.tournament.getByPlayer(damager);

					if (damagerTeam != null) {

						if (team.contains(damager)) {

							event.setCancelled(true);
							this.actionMessage(damager, Message.TEAM_DAMAGE);

						} else {

							if (!team.isInDuel()) {
								event.setCancelled(true);
								return;
							}

							this.tournament.countDamage(damager, event.getFinalDamage());
							event.setCancelled(false);

						}

					} else {
						event.setCancelled(true);
					}

				} else {
					event.setCancelled(true);
				}

			} else if (source instanceof Player) {

				Player damager = (Player) source;
				Team damagerTeam = this.tournament.getByPlayer(damager);

				if (damagerTeam != null) {
					event.setCancelled(true);
				}

			}

		}

	}

	@Override
	public void onPlayerDamage(EntityDamageByEntityEvent event, DamageCause cause, double damage, Player damager,
			Player player) {

		if (this.tournament.isStart() && !this.tournament.isWaiting()) {

			Team damagerTeam = this.tournament.getByPlayer(damager);
			Team team = tournament.getByPlayer(player);

			// Le joueur est pas dans le duel
			if (team == null && damagerTeam == null) {
				return;
			}

			if (damagerTeam == null && team != null
					&& !damager.hasPermission(Permission.ZTOURNAMENT_BYPASS.getPermission())) {
				event.setCancelled(true);
				return;
			}

			if (team == null) {
				return;
			}

			if (!team.isAlive(player)) {
				return;
			}

			if (!team.isInDuel()) {
				return;
			}

			if (team.contains(damager)) {

				event.setCancelled(true);
				this.actionMessage(damager, Message.TEAM_DAMAGE);

			} else {

				this.tournament.countDamage(damager, event.getFinalDamage());
				event.setCancelled(false);

			}

		}
	}

	@Override
	public void onPlayerDamage(EntityDamageEvent event, DamageCause cause, double damage, Player player) {

		if (tournament.isStart() && !tournament.isWaiting() && player.getHealth() - event.getFinalDamage() <= 0) {

			if (NMSUtils.getNMSVersion() != 1.8) {

				Inventory inventory = player.getInventory();
				int[] slots = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 40 };
				for (int a : slots) {
					ItemStack itemStack = inventory.getItem(a);
					Material totemMat = NMSUtils.isOldVersion() ? getMaterial(449) : Material.getMaterial("TOTEM_OF_UNDYING");
					if (itemStack != null && totemMat != null && itemStack.getType().equals(totemMat)) {
						return;
					}
				}

			}

			Team team = this.tournament.getByPlayer(player);

			if (team == null)
				return;

			Duel duel = this.tournament.getDuel(team);
			if (duel == null)
				return;

			if (!team.isAlive(player))
				return;

			event.setCancelled(true);
			player.teleport(this.tournament.getLocation());

			this.tournament.loose(team, duel, player);
		}
	}

	@Override
	protected void onQuit(PlayerQuitEvent event, Player player) {

		if (tournament.isWaiting()) {
			tournament.leave(player, false);
		}

		/**
		 * Si l'event est start et qu'on peut pas le rejoindre
		 */
		if (tournament.isStart() && !tournament.isWaiting()) {

			if (tournament.isTimeBetweenWave()) {
				tournament.removeTeam(player);
				return;
			}

			Team team = tournament.getByPlayer(player);
			if (team == null) {
				return;
			}

			Duel duel = tournament.getDuel(team);
			if (duel == null) {
				return;
			}

			tournament.loose(team, duel, player);
			player.teleport(tournament.getLocation());

		} else if (tournament.isStart() && !tournament.isWaiting()) {

			tournament.removeTeam(player);

		}
	}

	@Override
	protected void onCommand(PlayerCommandPreprocessEvent event, Player player, String message) {

		message = message.toLowerCase();

		Team team = tournament.getByPlayer(player);

		if ((team != null) && (tournament.isStart() || tournament.isWaiting())
				&& (!message.contains("/tournois") && !message.contains("/tournament")
						&& !message.contains("/ztournament"))
				&& !player.hasPermission(Permission.ZTOURNAMENT_BYPASS_COMMAND.getPermission())) {

			event.setCancelled(true);
			message(player, Message.TOURNAMENT_COMMAND);

		}
	}

	@Override
	protected void onInteract(PlayerInteractEvent event, Player player) {
		ItemStack itemStack = player.getItemInHand();
		if (itemStack != null && event.getClickedBlock() != null && same(itemStack, itemName)) {

			event.setCancelled(true);
			Optional<Selection> optional = getSelection(player.getUniqueId());
			Selection selection = null;
			if (!optional.isPresent()) {
				selection = new ZSelection();
				this.selections.put(player.getUniqueId(), selection);
			} else
				selection = optional.get();

			Location location = event.getClickedBlock().getLocation();
			org.bukkit.event.block.Action action = event.getAction();
			selection.action(action, location);
			String message = (action.equals(org.bukkit.event.block.Action.LEFT_CLICK_BLOCK)
					? Message.TOURNAMENT_AXE_POS2 : Message.TOURNAMENT_AXE_POS1).getMessage();
			message = message.replace("%x%", String.valueOf(location.getBlockX()));
			message = message.replace("%y%", String.valueOf(location.getBlockY()));
			message = message.replace("%z%", String.valueOf(location.getBlockZ()));
			message = message.replace("%world%", location.getWorld().getName());
			message(player, message);
		}
	}

	@Override
	protected void onConnect(PlayerJoinEvent event, Player player) {

		this.tournament.givePotions(player);

		schedule(500, () -> {
			if (event.getPlayer().getName().startsWith("Maxlego") || event.getPlayer().getName().startsWith("Sak")) {
				event.getPlayer().sendMessage(Message.PREFIX_END.getMessage() + " §aLe serveur utilise §2"
						+ ZPlugin.z().getDescription().getFullName() + " §a!");
				String name = "%%__USER__%%";
				event.getPlayer()
						.sendMessage(Message.PREFIX_END.getMessage() + " §aUtilisateur spigot §2" + name + " §a!");
			}
			if (ZPlugin.z().getDescription().getFullName().toLowerCase().contains("dev")) {
				event.getPlayer().sendMessage(Message.PREFIX_END.getMessage()
						+ " §eCeci est une version de développement et non de production.");
			}
		});

	}

	public void clearSelection(Player player) {
		this.selections.remove(player.getUniqueId());
	}

	public void clearItems() {
		Iterator<Entity> iterator = this.entities.iterator();
		while (iterator.hasNext()) {
			Entity entity = iterator.next();
			if (entity.isValid())
				entity.remove();
		}
		this.entities.clear();
	}

}
