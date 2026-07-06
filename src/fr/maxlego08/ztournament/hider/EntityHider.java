package fr.maxlego08.ztournament.hider;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

public abstract class EntityHider implements Listener {

	protected Map<UUID, Integer> potions = new HashMap<UUID, Integer>();
	protected Table<Integer, Integer, Boolean> observerEntityMap = HashBasedTable.create();

	protected boolean enable = false;

	private static final PacketType[] ENTITY_PACKETS = { PacketType.Play.Server.ENTITY_EQUIPMENT,
			PacketType.Play.Server.ENTITY_LOOK, PacketType.Play.Server.NAMED_ENTITY_SPAWN,
			PacketType.Play.Server.COLLECT, PacketType.Play.Server.SPAWN_ENTITY,
			PacketType.Play.Server.SPAWN_ENTITY_PAINTING, PacketType.Play.Server.SPAWN_ENTITY_EXPERIENCE_ORB,
			PacketType.Play.Server.EXPERIENCE, PacketType.Play.Server.ENTITY_VELOCITY,
			PacketType.Play.Server.ENTITY_TELEPORT, PacketType.Play.Server.ENTITY_HEAD_ROTATION,
			PacketType.Play.Server.REL_ENTITY_MOVE_LOOK, PacketType.Play.Server.REL_ENTITY_MOVE,
			PacketType.Play.Server.ENTITY_STATUS, PacketType.Play.Server.ATTACH_ENTITY,
			PacketType.Play.Server.ENTITY_METADATA, PacketType.Play.Server.ENTITY_EFFECT,
			PacketType.Play.Server.REMOVE_ENTITY_EFFECT, PacketType.Play.Server.BLOCK_BREAK_ANIMATION,
			PacketType.Play.Server.ANIMATION, PacketType.Play.Server.WORLD_PARTICLES,
			PacketType.Play.Server.WORLD_EVENT, PacketType.Play.Server.SPAWN_ENTITY_LIVING, };

	public enum Policy {
		WHITELIST,
		BLACKLIST,
	}

	private ProtocolManager manager;
	private Listener bukkitListener;
	private PacketAdapter protocolListener;
	protected final Policy policy;

	public EntityHider(Plugin plugin, Policy policy) {
		Preconditions.checkNotNull(plugin, "plugin cannot be NULL.");
		this.policy = policy;
		this.manager = ProtocolLibrary.getProtocolManager();
		plugin.getServer().getPluginManager().registerEvents(bukkitListener = constructBukkit(), plugin);
		manager.addPacketListener(protocolListener = constructProtocol(plugin));
	}

	protected boolean setVisibility(Player observer, int entityID, boolean visible) {
		switch (policy) {
		case BLACKLIST:
			return !setMembership(observer, entityID, !visible);
		case WHITELIST:
			return setMembership(observer, entityID, visible);
		default:
			throw new IllegalArgumentException("Unknown policy: " + policy);
		}
	}

	protected boolean setMembership(Player observer, int entityID, boolean member) {
		if (member) {
			return observerEntityMap.put(observer.getEntityId(), entityID, true) != null;
		} else {
			return observerEntityMap.remove(observer.getEntityId(), entityID) != null;
		}
	}

	protected boolean getMembership(Player observer, int entityID) {
		return observerEntityMap.contains(observer.getEntityId(), entityID);
	}

	protected boolean isVisible(Player observer, int entityID) {
		boolean presence = getMembership(observer, entityID);
		return policy == Policy.WHITELIST ? presence : !presence;
	}

	protected void removeEntity(Entity entity, boolean destroyed) {
		int entityID = entity.getEntityId();
		for (Map<Integer, Boolean> maps : observerEntityMap.rowMap().values()) {
			maps.remove(entityID);
		}
	}

	protected void removePlayer(Player player) {
		observerEntityMap.rowMap().remove(player.getEntityId());
	}

	private Listener constructBukkit() {
		return new Listener() {
			@EventHandler
			public void onEntityDeath(EntityDeathEvent e) {
				removeEntity(e.getEntity(), true);
			}
			@EventHandler
			public void onChunkUnload(ChunkUnloadEvent e) {
				for (Entity entity : e.getChunk().getEntities()) {
					removeEntity(entity, false);
				}
			}
			@EventHandler
			public void onPlayerQuit(PlayerQuitEvent e) {
				removePlayer(e.getPlayer());
			}
		};
	}

	private PacketAdapter constructProtocol(Plugin plugin) {
		return new PacketAdapter(plugin, ENTITY_PACKETS) {
			@Override
			public void onPacketSending(PacketEvent event) {
				int entityID = event.getPacket().getIntegers().read(0);
				if (!isVisible(event.getPlayer(), entityID)) {
					event.setCancelled(true);
				}
			}
		};
	}

	public final boolean toggleEntity(Player observer, Entity entity) {
		if (isVisible(observer, entity.getEntityId())) {
			return hideEntity(observer, entity);
		} else {
			return !showEntity(observer, entity);
		}
	}

	public final boolean showEntity(Player observer, Entity entity) {
		validate(observer, entity);
		boolean hiddenBefore = !setVisibility(observer, entity.getEntityId(), true);
		if (manager != null && hiddenBefore) {
			manager.updateEntity(entity, Arrays.asList(observer));
		}
		return hiddenBefore;
	}

	public final boolean hideEntity(Player observer, Entity entity) {
		validate(observer, entity);
		boolean visibleBefore = setVisibility(observer, entity.getEntityId(), false);
		if (visibleBefore) {
			PacketContainer destroyEntity = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
			destroyEntity.getIntegerArrays().write(0, new int[] { entity.getEntityId() });
			try {
				manager.sendServerPacket(observer, destroyEntity);
			} catch (Exception e) {
				throw new RuntimeException("Cannot send server packet.", e);
			}
		}
		return visibleBefore;
	}

	public final boolean canSee(Player observer, Entity entity) {
		validate(observer, entity);
		return isVisible(observer, entity.getEntityId());
	}

	private void validate(Player observer, Entity entity) {
		Preconditions.checkNotNull(observer, "observer cannot be NULL.");
		Preconditions.checkNotNull(entity, "entity cannot be NULL.");
	}

	public Policy getPolicy() {
		return policy;
	}

	public void close() {
		if (manager != null) {
			HandlerList.unregisterAll(bukkitListener);
			manager.removePacketListener(protocolListener);
			manager = null;
		}
	}

}
