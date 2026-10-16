package space.gorogoro.explosioncontrol;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractWindCharge;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.entity.minecart.ExplosiveMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 爆発による地形の破壊やダメージを、爆発の種類ごとに制御する。
 */
public class ExplosionControl extends JavaPlugin implements Listener {

  private static final String PERMISSION_RELOAD = "explosioncontrol.reload";

  // 爆発の種類(config.yml の explosions の下のキー)
  private static final String TYPE_CREEPER = "creeper";
  private static final String TYPE_TNT = "tnt";
  private static final String TYPE_TNT_MINECART = "tnt-minecart";
  private static final String TYPE_FIREBALL = "fireball";
  private static final String TYPE_WITHER = "wither";
  private static final String TYPE_END_CRYSTAL = "end-crystal";
  private static final String TYPE_OTHER = "other";
  private static final String[] EXPLOSION_TYPES = {
          TYPE_CREEPER, TYPE_TNT, TYPE_TNT_MINECART, TYPE_FIREBALL, TYPE_WITHER, TYPE_END_CRYSTAL, TYPE_OTHER
  };

  /**
   * 爆発の種類ごとの設定。
   */
  private record ExplosionSettings(boolean breakBlocks, boolean damagePlayer, boolean damageMob,
                                   boolean damageItem, boolean damageDecoration) {
  }

  // config.yml の設定。起動時とリロード時に一度だけ読み込む
  private Set<String> worlds = Collections.emptySet();
  private final Map<String, ExplosionSettings> explosions = new HashMap<>();
  private boolean preventEndermanPickup;
  private boolean preventEndermanPlace;

  // 表示するメッセージ(config.yml の messages.<language>)。起動時とリロード時に一度だけ読み込む
  private static final String DEFAULT_LANGUAGE = "en";
  private static final String[] MESSAGE_KEYS = {
          "reloaded", "no-permission", "usage"
  };
  private final Map<String, String> messages = new HashMap<>();

  /**
   * JavaPlugin method onEnable.
   */
  @Override
  public void onEnable() {
    try {
      getLogger().info("The Plugin Has Been Enabled!");

      // If there is no setting file, it is created
      if(!getDataFolder().exists()){
        getDataFolder().mkdir();
      }

      File configFile = new File(getDataFolder(), "config.yml");
      if(!configFile.exists()){
        saveDefaultConfig();
      }
      loadSettings();

      getServer().getPluginManager().registerEvents(this, this);
    } catch (Exception e) {
      logStackTrace(e);
    }
  }

  /**
   * config.yml を読み込んで保持する。足りない項目があれば jar 内の既定値を追記する。
   */
  private void loadSettings() {
    if (!getConfig().getKeys(true).containsAll(getConfig().getDefaults().getKeys(true))) {
      getConfig().options().copyDefaults(true);
      saveConfig();
    }
    worlds = Collections.unmodifiableSet(new HashSet<>(getConfig().getStringList("worlds")));
    explosions.clear();
    for (String type : EXPLOSION_TYPES) {
      String path = "explosions." + type + ".";
      explosions.put(type, new ExplosionSettings(
              getConfig().getBoolean(path + "break-blocks"),
              getConfig().getBoolean(path + "damage.player"),
              getConfig().getBoolean(path + "damage.mob"),
              getConfig().getBoolean(path + "damage.item"),
              getConfig().getBoolean(path + "damage.decoration")));
    }
    preventEndermanPickup = getConfig().getBoolean("enderman.prevent-pickup");
    preventEndermanPlace = getConfig().getBoolean("enderman.prevent-place");
    loadMessages();
  }

  /**
   * language で選んだ言語のメッセージを読み込む。無い言語なら英語にする。足りない項目は jar 内の英語で補う。
   */
  private void loadMessages() {
    String language = getConfig().getString("language");
    if (language == null || !getConfig().isConfigurationSection("messages." + language)) {
      getLogger().warning("Unknown language '" + language + "' in config.yml. Using '" + DEFAULT_LANGUAGE + "'.");
      language = DEFAULT_LANGUAGE;
    }
    messages.clear();
    for (String key : MESSAGE_KEYS) {
      String text = getConfig().getString("messages." + language + "." + key);
      if (text == null && getConfig().getDefaults() != null) {
        text = getConfig().getDefaults().getString("messages." + DEFAULT_LANGUAGE + "." + key);
      }
      messages.put(key, text != null ? text : key);
    }
  }

  private String message(String key) {
    return messages.getOrDefault(key, key);
  }

  /**
   * そのワールドで動くか。worlds が空なら全ワールドで動く。
   */
  private boolean isTargetWorld(World world) {
    return worlds.isEmpty() || worlds.contains(world.getName());
  }

  /**
   * 爆発を起こしたエンティティから爆発の種類を決める。扱わないもの(ドラゴンやウィンドチャージなど)は null。
   * 爆発元のエンティティが無い(ベッドやアンカーなどのブロックの爆発)ときは other。
   */
  private static String explosionType(Entity source) {
    if (source == null) {
      return TYPE_OTHER;
    }
    if (source instanceof Creeper) {
      return TYPE_CREEPER;
    }
    if (source instanceof TNTPrimed) {
      return TYPE_TNT;
    }
    if (source instanceof ExplosiveMinecart) {
      return TYPE_TNT_MINECART;
    }
    // ウィザーの頭は Fireball の一種なので、Fireball より先に判定する
    if (source instanceof Wither || source instanceof WitherSkull) {
      return TYPE_WITHER;
    }
    if (source instanceof Fireball) {
      return TYPE_FIREBALL;
    }
    if (source instanceof EnderCrystal) {
      return TYPE_END_CRYSTAL;
    }
    // ウィンドチャージ(地形を壊さない)と、エンダードラゴンが体当たりでブロックを壊すのはバニラのままにする
    if (source instanceof AbstractWindCharge || source instanceof EnderDragon) {
      return null;
    }
    return TYPE_OTHER;
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onEntityExplode(EntityExplodeEvent event) {
    if (event.blockList().isEmpty() || !isTargetWorld(event.getLocation().getWorld())) {
      return;
    }
    String type = explosionType(event.getEntity());
    if (type != null && !explosions.get(type).breakBlocks()) {
      event.blockList().clear();
    }
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onBlockExplode(BlockExplodeEvent event) {
    if (event.blockList().isEmpty() || !isTargetWorld(event.getBlock().getWorld())) {
      return;
    }
    if (!explosions.get(TYPE_OTHER).breakBlocks()) {
      event.blockList().clear();
    }
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onEntityDamage(EntityDamageEvent event) {
    EntityDamageEvent.DamageCause cause = event.getCause();
    if (cause != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION && cause != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
      return;
    }
    Entity target = event.getEntity();
    if (!isTargetWorld(target.getWorld())) {
      return;
    }
    String type = explosionType(event.getDamageSource().getDirectEntity());
    if (type == null) {
      return;
    }
    ExplosionSettings settings = explosions.get(type);
    boolean allowed;
    if (target instanceof Player) {
      allowed = settings.damagePlayer();
    } else if (target instanceof ArmorStand || target instanceof Hanging) {
      allowed = settings.damageDecoration();
    } else if (target instanceof LivingEntity) {
      allowed = settings.damageMob();
    } else if (target instanceof Item) {
      allowed = settings.damageItem();
    } else {
      // トロッコやボートなどはバニラのままにする
      return;
    }
    if (!allowed) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onHangingBreak(HangingBreakEvent event) {
    if (event.getCause() != HangingBreakEvent.RemoveCause.EXPLOSION || !isTargetWorld(event.getEntity().getWorld())) {
      return;
    }
    // 爆発元のエンティティがあれば HangingBreakByEntityEvent の remover になる
    Entity source = event instanceof HangingBreakByEntityEvent byEntity ? byEntity.getRemover() : null;
    String type = explosionType(source);
    if (type != null && !explosions.get(type).damageDecoration()) {
      event.setCancelled(true);
    }
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onEntityChangeBlock(EntityChangeBlockEvent event) {
    if (!(event.getEntity() instanceof Enderman) || !isTargetWorld(event.getBlock().getWorld())) {
      return;
    }
    // 空気に変わるなら持ち上げ、それ以外は設置
    boolean pickup = event.getBlockData().getMaterial().isAir();
    if (pickup ? preventEndermanPickup : preventEndermanPlace) {
      event.setCancelled(true);
    }
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!sender.hasPermission(PERMISSION_RELOAD)) {
      sender.sendMessage(Component.text(message("no-permission"), NamedTextColor.RED));
      return true;
    }
    if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
      sender.sendMessage(Component.text(message("usage"), NamedTextColor.RED));
      return true;
    }
    reloadConfig();
    loadSettings();
    sender.sendMessage(Component.text(message("reloaded"), NamedTextColor.GREEN));
    return true;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (args.length == 1 && sender.hasPermission(PERMISSION_RELOAD) && "reload".startsWith(args[0].toLowerCase())) {
      return List.of("reload");
    }
    return Collections.emptyList();
  }

  public void logStackTrace(Exception e){
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      pw.flush();
      getLogger().warning(sw.toString());
  }

  /**
   * JavaPlugin method onDisable.
   */
  @Override
  public void onDisable() {
    try {
      getLogger().info("The Plugin Has Been Disabled!");
    } catch (Exception e) {
      logStackTrace(e);
    }
  }
}
