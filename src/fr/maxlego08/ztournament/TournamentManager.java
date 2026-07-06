package fr.maxlego08.ztournament;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import fr.maxlego08.ztournament.api.Arena;
import fr.maxlego08.ztournament.api.ClearReason;
import fr.maxlego08.ztournament.api.Duel;
import fr.maxlego08.ztournament.api.Kit;
import fr.maxlego08.ztournament.api.Kits;
import fr.maxlego08.ztournament.api.Team;
import fr.maxlego08.ztournament.api.Tournament;
import fr.maxlego08.ztournament.api.TournoisType;
import fr.maxlego08.ztournament.api.events.TournamentAutoEndWaveEvent;
import fr.maxlego08.ztournament.api.events.TournamentEvent;
import fr.maxlego08.ztournament.api.events.TournamentPostWaveEvent;
import fr.maxlego08.ztournament.api.events.TournamentPreWaveEvent;
import fr.maxlego08.ztournament.api.events.TournamentStartEvent;
import fr.maxlego08.ztournament.api.events.TournamentStartNoEnoughEvent;
import fr.maxlego08.ztournament.api.events.TournamentStartNoEnoughRestartEvent;
import fr.maxlego08.ztournament.api.events.TournamentStartTickEvent;
import fr.maxlego08.ztournament.api.events.TournamentStartWaveEvent;
import fr.maxlego08.ztournament.api.events.TournamentStopEvent;
import fr.maxlego08.ztournament.api.events.TournamentTeamCreateEvent;
import fr.maxlego08.ztournament.api.events.TournamentTeamDisbandEvent;
import fr.maxlego08.ztournament.api.events.TournamentTeamInviteEvent;
import fr.maxlego08.ztournament.api.events.TournamentTeamJoinEvent;
import fr.maxlego08.ztournament.api.events.TournamentTeamJoinSuccessEvent;
import fr.maxlego08.ztournament.api.events.TournamentTeamLeaveEvent;
import fr.maxlego08.ztournament.api.events.TournamentTeamLooseEvent;
import fr.maxlego08.ztournament.api.events.TournamentTeamRewardEvent;
import fr.maxlego08.ztournament.api.events.TournamentWareArenaEvent;
import fr.maxlego08.ztournament.api.events.TournamentWaveCommandNextEvent;
import fr.maxlego08.ztournament.api.events.TournamentWinEvent;
import fr.maxlego08.ztournament.save.Config;
import fr.maxlego08.ztournament.zcore.ZPlugin;
import fr.maxlego08.ztournament.zcore.enums.Message;
import fr.maxlego08.ztournament.zcore.logger.Logger;
import fr.maxlego08.ztournament.zcore.logger.Logger.LogType;
import fr.maxlego08.ztournament.zcore.utils.ZPotionEffect;
import fr.maxlego08.ztournament.zcore.utils.ZUtils;
import fr.maxlego08.ztournament.zcore.utils.builder.TimerBuilder;
import fr.maxlego08.ztournament.zcore.utils.inventory.Pagination;
import fr.maxlego08.ztournament.zcore.utils.storage.Persist;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEvent.Action;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class TournamentManager extends ZUtils implements Tournament {

	private static List<ZArena> arenas = new ArrayList<ZArena>();
	private static String location;
	private transient List<Team> teams = new ArrayList<>();
	private transient List<Team> eliminatedTeams = new ArrayList<>();
	private transient List<Duel> duels = new ArrayList<>();
	private transient int maxTeams;
	private transient int currentTeams;

	private transient boolean isStart = false;
	private transient boolean isWaiting = false;
	private transient TournoisType type;
	private transient int wave = 1;
	private transient int countTeam;
	private transient int timer = 300;
	private transient boolean asNewTimer = false;
	private transient final Kits kits;
	private transient Kit kit;

	private transient Map<UUID, Collection<PotionEffect>> potions = new HashMap<UUID, Collection<PotionEffect>>();
	private transient Map<UUID, Double> playerDamageCount = new HashMap<>();
	private static Map<UUID, List<ZPotionEffect>> playerPotions = new HashMap<UUID, List<ZPotionEffect>>();

	private transient final HashSet<String> substanceChars = new HashSet<String>(Arrays.asList(new String[] { "0", "1",
			"2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
			"N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z", "a", "b", "c", "d", "e", "f", "g", "h",
			"i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z" }));

	private transient int maxTeamPerArena = 1;
	private transient boolean isTimeBetweenWave;

	public TournamentManager(Kits kits) {
		this.kits = kits;
	}

	/**
	 * Return arenas names
	 * 
	 * @return names
	 */
	@Override
	public List<String> getArenaNames() {
		return arenas.stream().map(Arena::getName).collect(Collectors.toList());
	}

	/**
	 * Permet de load la class
	 */
	@Override
	public void load(Persist persist) {
		persist.loadOrSaveDefault(this, TournamentManager.class, "tournaments");
		Logger.info("Number of arenas available: " + arenas.size(), LogType.SUCCESS);
		playerPotions.forEach((uuid, effects) -> {
			List<PotionEffect> potions = new ArrayList<>();
			effects.forEach(e -> potions.add(e.toPotionEffect()));
			this.potions.put(uuid, potions);
		});
	}

	/**
	 * Permet de save la class
	 */
	@Override
	public void save(Persist persist) {
		this.potions.forEach((player, potions) -> {
			List<ZPotionEffect> effets = new ArrayList<>();
			potions.forEach(e -> {
				effets.add(new ZPotionEffect(e.getType().getName(), e.getDuration(), e.getAmplifier()));
			});
			playerPotions.put(player, effets);
		});
		persist.save(this, "tournaments");
	}

	@Override
	public Team getByName(String name) {
		return this.teams.stream().filter(team -> team.getName().equals(name) || team.getOwner().getName().equals(name))
				.findAny().orElse(null);
	}

	@Override
	public Team getByPlayer(Player player) {
		return this.teams.stream().filter(team -> team.contains(player)).findAny().orElse(null);
	}

	@Override
	public Duel getDuel(Team team) {
		return this.duels.stream().filter(duel -> duel.match(team)).findAny().orElse(null);
	}

	@Override
	public Arena getAvaibleArena() {
		return arenas.stream().filter(arena -> arena.size() <= this.maxTeamPerArena - 1).findAny().orElse(null);
	}

	@Override
	public int getAvaibleArenaCount() {
		return (int) arenas.stream().filter(arena -> arena.size() <= this.maxTeamPerArena - 1).count();
	}

	@Override
	public Team getByPassTeam() {
		return this.teams.stream().filter(team -> !team.isInDuel()).findAny().orElse(null);
	}

	@Override
	public void clearPlayer(Player player, ClearReason clearReason) {
		player.getInventory().clear();
		player.getInventory().setBoots(null);
		player.getInventory().setChestplate(null);
		player.getInventory().setLeggings(null);
		player.getInventory().setHelmet(null);
		player.getPlayer().setItemOnCursor(null);
		player.getPlayer().setFireTicks(0);
		player.getPlayer().getOpenInventory().getTopInventory().clear();
		player.getPlayer().getActivePotionEffects().clear();
		player.setGameMode(GameMode.SURVIVAL);

		if (clearReason.equals(ClearReason.JOIN)) {
			this.potions.put(player.getUniqueId(), player.getPlayer().getActivePotionEffects());
		}

		player.getPlayer().getActivePotionEffects().forEach(e -> {
			player.getPlayer().removePotionEffect(e.getType());
		});

	}

	@Override
	public void givePotions(Player player) {
		if (Config.giveEffectPotionsToPlayerAfter && player != null && player.isOnline()) {

			Collection<PotionEffect> collection = this.potions.getOrDefault(player.getUniqueId(), new ArrayList<>());
			collection.forEach(effect -> player.addPotionEffect(effect));
			this.potions.remove(player.getUniqueId());

		}
	}

	@Override
	public Team getRandomTeam() {

		Random random = new Random();
		int rest = this.teams.size() % 2;

		if (rest == 1 && this.teams.size() - getDuelTeam() == 1) {
			return null;
		}

		Team team = this.teams.get(random.nextInt(this.teams.size()));
		if (team.isInDuel()) {
			return getRandomTeam();
		} else {
			team.setInDuel(true);
			return team;
		}
	}

	@Override
	public int getDuelTeam() {
		return (int) this.teams.stream().filter(team -> team.isInDuel()).count();
	}

	@Override
	public void setLobbyLocation(Player sender, Location location) {

		if (this.isStart) {
			message(sender, Message.TOURNAMENT_ENABLE);
			return;
		}

		TournamentManager.location = changeLocationToStringEye(location);
		message(sender, Message.TOURNAMENT_LOBBY_CREATE);
	}

	@Override
	public void createRandomDuel() {
		Team team = this.getRandomTeam();
		Team opponant = this.getRandomTeam();

		if (team == null || opponant == null) {
			return;
		}
		this.duels.add(new ZDuel(team, opponant));
		this.countTeam -= 2;
	}

	@Override
	public void createArena(CommandSender sender, String name, Location pos1, Location pos2) {

		if (this.isStart) {
			message(sender, Message.TOURNAMENT_ENABLE);
			return;
		}

		Optional<ZArena> optional = arenas.stream().filter(arena -> {
			return arena.getName().equals(name);
		}).findAny();

		if (optional.isPresent()) {
			message(sender, Message.TOURNAMENT_ARENA_ALREADY_EXIST);
			return;
		}

		ZArena arena = new ZArena(name, pos1, pos2);
		arenas.add(arena);
		message(sender, Message.TOURNAMENT_ARENA_CREATE);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void sendArena(Player player) {

		if (this.isStart) {
			message(player, Message.TOURNAMENT_ENABLE);
			return;
		}

		if (arenas.size() == 0) {
			message(player, Message.TOURNAMENT_ARENA_NO);
			return;
		}

		TextComponent message = buildTextComponent(Message.PREFIX.getMessage() + " §fList of arenas§8: §a");
		for (int a = 0; a != arenas.size(); a++) {
			if (a == arenas.size() - 1 && a != 0) {
				message.addExtra("§7 et §a");
			} else if (a != 0) {
				message.addExtra("§7, §a");
			}
			Arena arena = arenas.get(a);
			TextComponent component = new TextComponent("§a" + arena.getName());

			component.setClickEvent(
					new ClickEvent(Action.SUGGEST_COMMAND, "/ztournament delete " + arena.getUniqueId()));
			component.setHoverEvent(
					new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, getLore(arena)));
			message.addExtra(component);
		}
		player.spigot().sendMessage(message);
	}

	private BaseComponent[] getLore(Arena arena) {
		BaseComponent[] lore = new BaseComponent[3];
		lore[0] = new TextComponent("§8» §7Click to delete this arena\n");
		lore[1] = new TextComponent("§8» §7Location 1: §f" + arena.getPos1String().replace(",", " §8,§f ") + "\n");
		lore[2] = new TextComponent("§8» §7Location 2: §f" + arena.getPos2String().replace(",", " §8,§f "));
		return lore;
	}

	@Override
	public void deleteArena(CommandSender sender, UUID uuid) {

		if (this.isStart) {
			message(sender, Message.TOURNAMENT_ENABLE);
			return;
		}

		Arena a = arenas.stream().filter(arena -> {
			return arena.getId().equals(uuid);
		}).findAny().orElse(null);

		if (a == null) {
			message(sender, Message.TOURNAMENT_ARENA_NOT_FOUND);
			return;
		}

		arenas.remove(a);
		message(sender, Message.TOURNAMENT_ARENA_DELETE);
	}

	@Override
	public boolean canHurt(Player player, Player damager) {
		Team teamA = getByPlayer(player);
		Team teamB = getByPlayer(damager);
		return teamA == null ? true : teamB == null ? true : teamA.equals(teamB);
	}

	@Override
	public boolean inventoryHasItem(Player player) {
		ItemStack itemStack = player.getInventory().getBoots();
		if (itemStack != null) {
			return true;
		}

		itemStack = player.getInventory().getChestplate();
		if (itemStack != null) {
			return true;
		}

		itemStack = player.getInventory().getLeggings();
		if (itemStack != null) {
			return true;
		}

		itemStack = player.getInventory().getHelmet();
		if (itemStack != null) {
			return true;
		}

		for (ItemStack itemStack1 : player.getInventory().getContents()) {
			if (itemStack1 != null) {
				return true;
			}
		}

		return false;
	}

	@Override
	public void removeTeam(Player player) {
		Team team = getByPlayer(player);
		if (team == null) {
			return;
		}
		team.death(player);
		if (team.hasLoose()) {
			teams.remove(team);
		}
		this.givePotions(player);
	}

	@Override
	public void checkTeam() {
		Iterator<Team> iterator = this.teams.iterator();
		while (iterator.hasNext()) {
			Team team = iterator.next();
			if (!team.isValid()) {
				iterator.remove();
			}
		}
	}

	@Override
	public void startTournois(CommandSender sender, TournoisType type, Kit kit) {

		if (this.isStart || this.isWaiting) {
			message(sender, Message.TOURNAMENT_ENABLE);
			return;
		}

		if (arenas.size() == 0) {
			message(sender, Message.TOURNAMENT_START_ERROR_ARENA);
			return;
		}

		if (location == null) {
			message(sender, Message.TOURNAMENT_START_ERROR_LOOBY);
			return;
		}

		this.kit = kit;

		TournamentStartEvent event = new TournamentStartEvent(type, kit);
		event.callEvent();

		if (event.isCancelled()) {
			return;
		}

		this.kit = event.getKit();
		this.type = event.getType();

		this.isStart = true;
		this.isWaiting = true;
		this.asNewTimer = false;

		this.duels.clear();
		this.teams.clear();

		this.maxTeams = 0;

		String pvp = type.getMax() + "v" + type.getMax();
		broadcast(Message.TOURNAMENT_START, "%type%", pvp);
		broadcast(Message.TITLE_START, "%type%", pvp);

		this.timer = Config.timeStartTournamentInSecond;

		new BukkitRunnable() {

			@Override
			public void run() {

				if (!isWaiting) {
					cancel();
					return;
				}

				TournamentEvent event = new TournamentStartTickEvent(timer);
				event.callEvent();

				if (event.isCancelled()) {
					cancel();
					return;
				}

				if (Config.displayTournamentInformations.contains(timer)) {
					broadcast(Message.TOURNAMENT_START_TIMER, "%timer%", TimerBuilder.getStringTime(timer));
					broadcast(Message.TITLE_START_INFO, "%timer%", TimerBuilder.getStringTime(timer));
				}

				if (timer == 0) {

					if (teams.size() <= 1) {

						if (!asNewTimer && Config.restartTeamSearch) {

							event = new TournamentStartNoEnoughRestartEvent();
							event.callEvent();

							asNewTimer = true;
							timer = Config.timeStartTournamentInSecond;
							broadcast(Message.TOURNAMENT_START_ERROR_PLAYER);
							broadcast(Message.TITLE_START_ERROR_PLAYER, "%timer%", TimerBuilder.getStringTime(timer));

						} else {

							event = new TournamentStartNoEnoughEvent();
							event.callEvent();

							isStart = false;
							isWaiting = false;
							asNewTimer = false;

							teams.forEach(e -> {
								e.clear();
								e.show();
								e.getRealPlayers().forEach(p -> {
									if (p.isOnline()) {
										p.getPlayer().teleport(getLocation());
										p.getPlayer().teleport(getLocation());
										givePotions(p.getPlayer());
									}
								});
							});

							teams.clear();
							duels.clear();

							cancel();

							broadcast(Message.TOURNAMENT_START_ERROR_PLAYER_CANCEL);

						}

					} else {
						cancel();
						start();
					}
				}
				timer--;
				if (!isStart) {
					cancel();
				}

			}

		}.runTaskTimer(ZPlugin.z(), 0, Config.enableDebug ? 4 : 20);

	}

	@Override
	public void start() {

		if (this.teams.size() == 0) {
			broadcast(Message.TOURNAMENT_START_ERROR_TEAM);
			return;
		}

		this.isWaiting = false;
		this.isStart = true;
		this.wave = 1;
		this.eliminatedTeams.clear();

		checkTeam();

		this.maxTeams = this.teams.size();
		this.currentTeams = this.teams.size();

		TournamentStartWaveEvent event = new TournamentStartWaveEvent(new ArrayList<>(teams), type);
		event.callEvent();
		if (event.isCancelled()) {
			return;
		}

		broadcast(Message.TOURNAMENT_START_FIRST_WAVE, "%size%", String.valueOf(this.teams.size()));

		this.startWave();
	}

	@Override
	public void startWave() {

		if (!this.isStart) {
			return;
		}

		TournamentEvent event = new TournamentPreWaveEvent(new ArrayList<>(teams), type, true);
		event.callEvent();
		if (event.isCancelled()) {
			return;
		}

		// Check
		checkTeam();

		this.playerDamageCount.clear();

		int currentWave = wave;
		this.countTeam = teams.size();
		this.maxTeamPerArena = 1;

		arenas.forEach(arena -> arena.clear());

		if (countTeam == 1) {
			end();
			return;
		}

		int duel = countTeam >> 1;
		for (int a = 0; a < duel; a++) {
			createRandomDuel();
		}

		Team bypassTeam = getByPassTeam();

		event = new TournamentPostWaveEvent(new ArrayList<>(teams), new ArrayList<>(duels), new ArrayList<>(arenas),
				type, bypassTeam);
		event.callEvent();
		if (event.isCancelled()) {
			return;
		}

		// Si il y a un nombre impair d'équipe
		if (bypassTeam != null) {
			bypassTeam.message(Message.TOURNAMENT_WAVE_AUTO);
		}

		broadcast(Message.TOURNAMENT_WAVE_START, "%round%", String.valueOf(currentWave), "%duel%",
				String.valueOf(duels.size()));

		this.wave++;
		this.duels.forEach(currentDuel -> {

			if (this.getAvaibleArenaCount() == 0) {
				this.maxTeamPerArena++;
			}

			Arena arena = getAvaibleArena();

			TournamentEvent event2 = new TournamentWareArenaEvent(arena, currentDuel);
			event2.callEvent();

			arena.addDuel(currentDuel);
			currentDuel.startDuel(arena.getPos1(), arena.getPos2());
			currentDuel.setArenea(arena);

		});

		broadcast(Message.TOURNAMENT_WAVE_TIMER, "%round%", String.valueOf(currentWave), "%timer%",
				TimerBuilder.getStringTime(Config.timeWaveEndInSecond));

		new BukkitRunnable() {

			int timer = Config.timeWaveEndInSecond;

			@Override
			public void run() {

				if (isTimeBetweenWave) {
					cancel();
					return;
				}

				if (!isStart) {
					cancel();
					return;
				}

				timer--;

				if (Config.displayWaveEndInformations.contains(timer)) {
					broadcast(Message.TOURNAMENT_WAVE_TIMER, "%round%", String.valueOf(currentWave), "%timer%",
							TimerBuilder.getStringTime(timer));
				}

				if (timer <= 0) {

					TournamentEvent event = new TournamentAutoEndWaveEvent();
					event.callEvent();

					cancel();
					broadcast(Message.TOURNAMENT_WAVE_END);

					if (Config.randomLooseTeam) {

						Iterator<Duel> iterator = new ArrayList<>(duels).iterator();
						while (iterator.hasNext()) {
							Duel duel = iterator.next();
							Team team = null;
							if (Config.enableEliminationOfThePlayerWhoMadeTheFewestDamage) {

								team = duel.getRandomTeamLessDamage(playerDamageCount);

							} else {
								team = new Random().nextBoolean() ? duel.getTeam() : duel.getOpponant();
							}
							Team finalTeam = team;
							new ArrayList<>(team.getPlayers()).forEach(e -> loose(finalTeam, duel, e));
						}

						canStartNextWave();
						return;
					}

					// Fix #1 — timeout else-path: properly eliminate the loser
					new ArrayList<>(duels).forEach(duel -> {

						duel.heal();

						Team winner = duel.getTeam();
						winner.setInDuel(false);
						winner.clear();
						winner.reMap();
						winner.show();
						winner.heal();
						winner.teleport(getLocation());

						Team looser = duel.getOpponant();
						looser.setInDuel(false);
						looser.clear();
						looser.heal();
						looser.show();
						looser.teleport(getLocation());

						looser.setPosition(currentTeams--);
						looser.message(Message.TOURNAMENT_DUEL_LOOSE, "%position%",
								String.valueOf(looser.getPosition()), "%team%", String.valueOf(maxTeams));
						looser.getRealPlayers().forEach(offlinePlayer -> {
							if (offlinePlayer.isOnline()) {
								givePotions(offlinePlayer.getPlayer());
							}
						});
						if (!eliminatedTeams.contains(looser)) {
							eliminatedTeams.add(looser);
						}
						teams.remove(looser);

					});

					duels.clear();

					canStartNextWave();

				}
			}
		}.runTaskTimer(ZPlugin.z(), 0, Config.enableDebug ? 4 : 20);
	}

	@Override
	public void end() {

		if (this.teams.size() == 0) {
			broadcast(Message.TOURNAMENT_WIN_ERROR);
			return;
		}

		Team winner = this.teams.get(0);
		winner.clear();
		winner.getPlayers().forEach(player -> player.teleport(getLocation()));
		winner.show();
		winner.setPosition(1);

		if (!this.eliminatedTeams.contains(winner)) {
			this.eliminatedTeams.add(winner);
		}

		winner.getRealPlayers().forEach(offlinePlayer -> {
			if (offlinePlayer.isOnline()) {
				this.givePotions(offlinePlayer.getPlayer());
			}
		});

		TournamentEvent event = new TournamentWinEvent(new ArrayList<>(this.eliminatedTeams), winner);
		event.callEvent();

		if (event.isCancelled()) {
			return;
		}

		this.isStart = false;
		this.isWaiting = false;
		this.teams.clear();
		this.duels.clear();
		this.wave = 0;

		broadcast(Message.TITLE_WIN, "%team%", winner.getName());
		broadcast(Message.TOURNAMENT_WIN, "%team%", winner.getName());

		if (Config.showRanking) {
			Pagination<Team> pagination = new Pagination<>();
			pagination.paginateReverse(this.eliminatedTeams, 3, 1).forEach(team -> {

				String position = Config.rankingPosition.getOrDefault(team.getPosition(),
						"Impossible de trouver la position, message de vérifier votre configuration.");

				String msg = Message.TOURNAMENT_CLASSEMENT.replace("%team%", team.getName()).replace("%position%",
						position);

				TextComponent component = buildTextComponent(msg);

				List<String> strings = new ArrayList<>();
				List<String> classementMessage = Message.TOURNAMENT_CLASSEMENT_HOVER.getMessages();
				classementMessage.forEach(m -> {

					if (m.contains("%players%")) {
						team.getRealPlayers().forEach(pp -> {
							if (pp != null)
								strings.add(
										Message.TOURNAMENT_CLASSEMENT_HOVER_PLAYER.replace("%player%", pp.getName()));
						});
					} else {
						strings.add(m);
					}

				});

				setHoverMessage(component, strings);

				Bukkit.getOnlinePlayers().forEach(bukkitPlayer -> {
					bukkitPlayer.spigot().sendMessage(component);
				});

			});
		}

		// Fix #3 — sort by position descending (last eliminated first, winner last)
		// and schedule each reward 1 s apart to avoid chat/Discord flooding
		List<Team> sortedEliminated = new ArrayList<>(this.eliminatedTeams);
		sortedEliminated.sort((a, b) -> Integer.compare(b.getPosition(), a.getPosition()));

		for (int i = 0; i < sortedEliminated.size(); i++) {
			final Team team = sortedEliminated.get(i);
			Bukkit.getScheduler().runTaskLater(ZPlugin.z(), () -> {

				Reward reward = this.getReward(team.getPosition());

				TournamentTeamRewardEvent tournamentEvent = new TournamentTeamRewardEvent(team, reward);
				tournamentEvent.callEvent();

				if (tournamentEvent.isCancelled()) {
					return;
				}

				Reward finalReward = tournamentEvent.getReward();

				if (finalReward != null) {

					finalReward.getCommands().forEach(command -> {

						String finalCommand = command.replace("%team%", team.getName()).replace("%leader%",
								team.getOwner().getName());

						if (finalCommand.contains("%player%")) {
							team.getRealPlayers().forEach(player -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
									finalCommand.replace("%player%", player.getName())));
						} else {
							Bukkit.dispatchCommand(Bukkit.getConsoleSender(), finalCommand);
						}

					});

				}

			}, 20L * i);
		}

		this.eliminatedTeams.clear();
	}

	@Override
	public void kick(CommandSender sender, Player player) {

		if (this.isStart && this.isWaiting) {
			message(sender, Message.TOURNAMENT_KICK_ERROR);
			return;
		}

		Team team = getByPlayer(player);
		if (team == null) {
			message(sender, Message.TOURNAMENT_KICK_ERROR_PLAYER);
			return;
		}

		if (this.isTimeBetweenWave()) {
			this.removeTeam(player);
			message(sender, Message.TOURNAMENT_KICK_SUCCESS);
			return;
		}

		Duel duel = this.getDuel(team);
		if (duel == null) {
			message(sender, Message.TOURNAMENT_KICK_ERROR_DUEL);
			return;
		}

		this.loose(team, duel, player);
		player.teleport(this.getLocation());
		message(sender, Message.TOURNAMENT_KICK_SUCCESS);

	}

	@Override
	public void createTeam(Player player, String name) {
		if (isStart && !isWaiting) {
			message(player, Message.TOURNAMENT_CREATE_ERROR);
			return;
		}

		if (!isWaiting) {
			message(player, Message.TOURNAMENT_CREATE_ERROR);
			return;
		}

		// Verif si le mec a déjà une team
		Team team = getByPlayer(player);
		if (team != null) {
			message(player, Message.TOURNAMENT_CREATE_ERROR_PLAYER);
			return;
		}

		// Verif si la team existe
		team = getByName(name);
		if (team != null) {
			message(player, Message.TOURNAMENT_CREATE_ERROR_EXIT, "%name%", name);
			return;
		}

		// Verif de la taille maximal
		if (name.length() > Config.teamNameMaxName) {
			message(player, Message.TOURNAMENT_CREATE_ERROR_NAME_MAX, "%max%", String.valueOf(Config.teamNameMaxName));
			return;
		}

		// Verif de la taille minimal
		if (name.length() < Config.teamNameMinName) {
			message(player, Message.TOURNAMENT_CREATE_ERROR_NAME_MIN, "%min%", String.valueOf(Config.teamNameMinName));
			return;
		}

		// Verif des char
		for (char c : name.toCharArray()) {
			if (!substanceChars.contains(String.valueOf(c))) {
				message(player, Message.TOURNAMENT_CREATE_ERROR_NAME, "%char%", String.valueOf(c));
				return;
			}
		}

		// Verif de l'inventaire
		if (inventoryHasItem(player)) {
			message(player, Message.TOURNAMENT_CREATE_ERROR_INVENOTRY);
			return;
		}

		TournamentTeamCreateEvent event = new TournamentTeamCreateEvent(
				new ZTeam(this.plugin.getHider(), name, this.type.getMax(), player, this.kit), player, name);
		event.callEvent();

		if (event.isCancelled()) {
			return;
		}

		name = event.getName();

		team = new ZTeam(this.plugin.getHider(), name, this.type.getMax(), player, this.kit);
		this.teams.add(team);
		clearPlayer(player, ClearReason.JOIN);

		message(player, Message.TITLE_CREATE_SUCCESS, "%team%", name);
		broadcast(Message.TOURNAMENT_CREATE_TEAM_BROADCAST, "%player%", player.getName(), "%team%", name);

		if (Config.enableTeleportDelay) {
			Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
				player.teleport(getLocation());
			}, 20 * Config.teleportDelayInSeconds);
		} else {
			player.teleport(getLocation());
		}
	}

	@Override
	public void joinTeam(Player player, String name) {

		if (this.isStart && !this.isWaiting) {
			message(player, Message.TOURNAMENT_JOIN_ERROR_JOIN);
			return;
		}

		Team team = getByPlayer(player);
		if (team != null) {
			message(player, Message.TOURNAMENT_JOIN_ERROR_TEAM_ALREADY, "%team%", name);
			return;
		}

		if (team == null) {
			team = this.getByName(name);
			if (team == null) {
				message(player, Message.TOURNAMENT_JOIN_ERROR_TEAM, "%team%", name);
				return;
			}
		}

		if (inventoryHasItem(player)) {
			message(player, Message.TOURNAMENT_JOIN_ERROR_INVENOTRY);
			return;
		}

		if (!team.isInvite(player)) {
			message(player, Message.TOURNAMENT_JOIN_ERROR_INVITE);
			return;
		}

		TournamentTeamJoinEvent joinTeamEvent = new TournamentTeamJoinEvent(team, player, team.join(player));
		joinTeamEvent.callEvent();

		if (joinTeamEvent.isCancelled())
			return;

		if (!joinTeamEvent.isCanJoin()) {

			message(player, Message.TOURNAMENT_JOIN_ERROR);

		} else {

			TournamentTeamJoinSuccessEvent event = new TournamentTeamJoinSuccessEvent(team, player);
			event.callEvent();

			if (event.isCancelled())
				return;

			team.message(Message.TOURNAMENT_JOIN_SUCCESS_INFO, "%player%", player.getName());
			clearPlayer(player, ClearReason.JOIN);

			message(player, Message.TITLE_JOIN_SUCCESS, "%team%", name);
			
			if (Config.enableTeleportDelay) {
				Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
					player.teleport(getLocation());
				}, 20 * Config.teleportDelayInSeconds);
			} else {
				player.teleport(getLocation());
			}
		}
	}

	@Override
	public void invitePlayer(Player player, Player target) {
		if (isStart && !isWaiting) {
			message(player, Message.TOURNAMENT_INVITE_ERROR);
			return;
		}

		if (!isWaiting) {
			message(player, Message.TOURNAMENT_INVITE_ERROR);
			return;
		}

		Team team = getByPlayer(player);
		if (team == null) {
			message(player, Message.TOURNAMENT_INVITE_ERROR_TEAM);
			return;
		}

		if (player.getUniqueId().equals(target.getUniqueId())) {
			message(player, Message.TOURNAMENT_INVITE_ERROR_PLAYER);
			return;
		}

		if (!team.isOwner(player)) {
			message(player, Message.TOURNAMENT_INVITE_ERROR_OWNER);
			return;
		}

		if (type.equals(TournoisType.V1)) {
			message(player, Message.TOURNAMENT_INVITE_ERROR_TYTE);
			return;
		}

		if (team.isInvite(target)) {
			team.removeInvite(target);
			team.message(Message.TOURNAMENT_INVITE_REMOVE, "%player%", player.getName(), "%target%", target.getName());
			return;
		}

		TournamentEvent event = new TournamentTeamInviteEvent(team, player);
		event.callEvent();

		if (event.isCancelled())
			return;

		team.invite(target);
		team.message(Message.TOURNAMENT_INVITE_TEAM, "%player%", player.getName(), "%target%", target.getName());

		message(target, Message.TOURNAMENT_INVITE_INFO, "%player%", player.getName(), "%team%", team.getName());

		TextComponent component = buildTextComponent(
				Message.TOURNAMENT_INVITE_INFO_JSON.replace("%team%", team.getName()));
		setClickAction(component, Action.RUN_COMMAND, "/ztournament join " + team.getName());
		setHoverMessage(component, Message.TOURNAMENT_INVITE_INFO_JSON_HOVER.replace("%team%", team.getName()));

		target.spigot().sendMessage(component);
	}

	@Override
	public void leave(Player player, boolean message) {

		if (this.isStart && !this.isWaiting) {
			if (message) {
				message(player, Message.TOURNAMENT_TEAM_LEAVE_ERROR);
			}
			return;
		}

		Team team = this.getByPlayer(player);
		if (team == null) {
			if (message) {
				message(player, Message.TOURNAMENT_TEAM_PLAYER_ERROR);
			}
			return;
		}

		if (team.getOwner().equals(player)) {

			TournamentTeamDisbandEvent event = new TournamentTeamDisbandEvent(team, player);
			event.callEvent();

			if (event.isCancelled()) {
				return;
			}

			team.disband();
			this.teams.remove(team);

		} else {

			TournamentTeamLeaveEvent event = new TournamentTeamLeaveEvent(team, player);
			event.callEvent();

			if (event.isCancelled()) {
				return;
			}

			if (message) {
				message(player, Message.TOURNAMENT_TEAM_LEAVE);
			}

			team.leave(player);
			player.teleport(getLocation());
			this.clearPlayer(player, ClearReason.LEAVE);

		}

		this.givePotions(player);

	}

	@Override
	public void loose(Team team, Duel duel, Player player) {

		duel.onPlayerLoose(player);

		TournamentEvent event = new TournamentTeamLooseEvent(team, duel, player);
		event.callEvent();

		duel.message(Message.TOURNAMENT_PLAYER_LOOSE, "%player%", player.getName());
		player.teleport(this.getLocation());
		this.clearPlayer(player, ClearReason.GAME);

		if (duel.hasWinner() && duel.isDuel()) {

			duel.heal();

			Team winner = duel.getWinner();
			winner.message(Message.TOURNAMENT_DUEL_WIN);
			winner.setInDuel(false);
			winner.clear();
			winner.reMap();
			winner.show();
			winner.heal();
			winner.teleport(this.getLocation());

			Team looser = duel.getLooser();
			looser.setInDuel(false);
			looser.clear();
			looser.heal();
			looser.show();
			looser.teleport(this.getLocation());

			looser.getRealPlayers().forEach(offlinePlayer -> {
				if (offlinePlayer.isOnline()) {
					this.givePotions(offlinePlayer.getPlayer());
				}
			});

			looser.setPosition(this.currentTeams--);
			looser.message(Message.TOURNAMENT_DUEL_LOOSE, "%position%", String.valueOf(looser.getPosition()), "%team%",
					String.valueOf(this.maxTeams));

			if (!this.eliminatedTeams.contains(looser)) {
				this.eliminatedTeams.add(looser);
			}

			this.teams.remove(looser);
			this.duels.remove(duel);

			this.canStartNextWave();
		}

	}

	@Override
	public synchronized void canStartNextWave() {

		/**
		 * On termine l'event s'il reste qu'une seul team Sinon on commence une
		 * nouvelle manche
		 */

		if (this.teams.size() == 1) {

			this.teams.forEach(Team::show);
			arenas.forEach(Arena::clear);
			end();

		} else if (duels.size() == 0) {

			if (this.isTimeBetweenWave) {
				return;
			}

			if (!this.isStart) {
				return;
			}

			this.teams.forEach(Team::show);
			arenas.forEach(Arena::clear);
			broadcast(Message.TOURNAMENT_WAVE_NEXT_TIME);

			this.plugin.getListener().clearItems();
			this.isTimeBetweenWave = true;

			new BukkitRunnable() {

				@Override
				public void run() {

					if (!isStart)
						return;

					broadcast(Message.TOURNAMENT_WAVE_NEXT, "%team%", String.valueOf(teams.size()));

					isTimeBetweenWave = false;

					startWave();

				}
			}.runTaskLater(ZPlugin.z(), 20 * Config.timeWaveNextInSecond);
		}
	}

	@Override
	public boolean canUseCommand(CommandSender sender) {
		if (!(sender instanceof Player))
			return true;
		Player player = (Player) sender;
		Team team = getByPlayer(player);
		return team != null && (isStart || isWaiting);
	}

	@Override
	public void stopTournois(CommandSender sender) {

		if (!this.isStart && !this.isWaiting) {
			message(sender, Message.TOURNAMENT_NOT_ENABLE);
			return;
		}

		this.potions.forEach((e, j) -> {
			OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(e);
			if (offlinePlayer.isOnline()) {
				Player player = offlinePlayer.getPlayer();
				j.forEach(b -> player.addPotionEffect(b));
			}
		});

		if (isWaiting) {

			TournamentEvent event = new TournamentStopEvent();
			event.callEvent();

			isStart = false;
			isWaiting = false;
			broadcast(Message.TOURNAMENT_STOP);
			return;

		}

		TournamentEvent event = new TournamentStopEvent();
		event.callEvent();

		isStart = false;
		isWaiting = false;
		broadcast(Message.TOURNAMENT_STOP);
		teams.forEach(e -> {
			e.clear();
			e.show();
			e.getRealPlayers().forEach(p -> {
				if (p.isOnline()) {
					p.getPlayer().teleport(getLocation());
					p.getPlayer().teleport(getLocation());
					this.givePotions(p.getPlayer());
				}
			});
		});

		this.teams.clear();
		this.duels.clear();
	}

	@Override
	public void nextWave(CommandSender sender) {
		if (!isStart) {
			message(sender, Message.TOURNAMENT_NOT_ENABLE);
			return;
		}

		TournamentEvent event = new TournamentWaveCommandNextEvent();
		event.callEvent();

		// Fix #1 — nextWave: properly eliminate the loser of each active duel
		new ArrayList<>(duels).forEach(duel -> {

			duel.heal();

			Team winner = duel.getTeam();
			winner.setInDuel(false);
			winner.clear();
			winner.reMap();
			winner.show();
			winner.heal();
			winner.teleport(getLocation());

			Team looser = duel.getOpponant();
			looser.setInDuel(false);
			looser.clear();
			looser.heal();
			looser.show();
			looser.teleport(getLocation());

			looser.setPosition(this.currentTeams--);
			looser.message(Message.TOURNAMENT_DUEL_LOOSE, "%position%",
					String.valueOf(looser.getPosition()), "%team%", String.valueOf(this.maxTeams));
			looser.getRealPlayers().forEach(offlinePlayer -> {
				if (offlinePlayer.isOnline()) {
					givePotions(offlinePlayer.getPlayer());
				}
			});
			if (!this.eliminatedTeams.contains(looser)) {
				this.eliminatedTeams.add(looser);
			}
			this.teams.remove(looser);

		});

		duels.clear();
		canStartNextWave();
	}

	@Override
	public boolean isStart() {
		return isStart;
	}

	@Override
	public boolean isWaiting() {
		return isWaiting;
	}

	@Override
	public Location getLocation() {
		return changeStringLocationToLocationEye(location);
	}

	@Override
	public boolean isTimeBetweenWave() {
		return this.isTimeBetweenWave;
	}

	@Override
	public void onPluginDisable() {

		if (this.isWaiting) {

			this.isStart = false;
			this.isWaiting = false;
			broadcast(Message.TOURNAMENT_STOP);
			return;

		}

		if (isStart) {

			this.isStart = false;
			this.isWaiting = false;
			broadcast(Message.TOURNAMENT_STOP);
			this.teams.forEach(e -> {
				e.clear();
				e.getRealPlayers().forEach(p -> {
					if (p.isOnline()) {
						p.getPlayer().teleport(getLocation());
						p.getPlayer().teleport(getLocation());
						this.givePotions(p.getPlayer());
					}
				});
			});

			teams.clear();
			duels.clear();
		}

	}

	@Override
	public Reward getReward(int position) {
		// Fix #2 — condition was inverted (maxPosition <= pos && minPosition >= pos)
		return Config.rewards.stream()
				.filter(reward -> reward.getMinPosition() <= position && reward.getMaxPosition() >= position).findAny()
				.orElse(null);
	}

	@Override
	public Kits getKits() {
		return kits;
	}

	@Override
	public Kit getKit() {
		return kit;
	}

	@Override
	public Optional<Team> getByOfflinePlayer(OfflinePlayer player) {
		List<Team> list = new ArrayList<>();
		list.addAll(this.teams);
		list.addAll(this.eliminatedTeams);
		return list.stream().filter(team -> team.match(player)).findFirst();
	}

	@Override
	public void setPosition(boolean isPos1, CommandSender sender, String name, Location location) {

		Arena arena = arenas.stream().filter(e -> {
			return e.getName().equals(name);
		}).findAny().orElse(null);

		if (arena == null) {
			message(sender, Message.TOURNAMENT_ARENA_NOT_FOUND);
			return;
		}

		if (isPos1) {
			arena.setPos1(location);
			message(sender, Message.TOURNAMENT_ARENA_POS1_CHANGE);
		} else {
			arena.setPos2(location);
			message(sender, Message.TOURNAMENT_ARENA_POS2_CHANGE);
		}

	}

	@Override
	public void countDamage(Player damager, double finalDamage) {
		this.playerDamageCount.put(damager.getUniqueId(),
				this.playerDamageCount.getOrDefault(damager.getUniqueId(), 0d) + finalDamage);
	}

}
