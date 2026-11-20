package com.bastionmc.roulette;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import java.lang.reflect.Method;
import java.util.*;

public final class BastionMCDungeonRoulette extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
    private static final List<String> LEVELS = List.of("facil", "normal", "dificil");
    private final Random random = new Random();
    private final Map<String, Spin> running = new HashMap<>();
    private final Map<UUID, String> pending = new HashMap<>();
    private static String key(Block b) { return b.getWorld().getUID()+":"+b.getX()+":"+b.getY()+":"+b.getZ(); }
    private String message(String key, String fallback) {
        String raw = getConfig().getString("messages."+key, fallback);
        var m = java.util.regex.Pattern.compile("&#([0-9a-fA-F]{6})").matcher(raw);
        StringBuffer sb = new StringBuffer();
        while (m.find()) m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(net.md_5.bungee.api.ChatColor.of("#"+m.group(1)).toString()));
        m.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&',sb.toString());
    }
    @Override public void onEnable() {
        saveDefaultConfig();
        Objects.requireNonNull(getCommand("droulette")).setExecutor(this);
        Objects.requireNonNull(getCommand("droulette")).setTabCompleter(this);
        getServer().getPluginManager().registerEvents(this,this);
    }
    @Override public void onDisable() {
        for (Spin s: new ArrayList<>(running.values())) s.stop();
        running.clear(); pending.clear();
    }
    @Override public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("bastionroulette.admin")) {sender.sendMessage(ChatColor.RED+"Solo OP.");return true;}
        if (args.length==1 && args[0].equalsIgnoreCase("reload")) {reloadConfig();sender.sendMessage(ChatColor.GREEN+"Ruletas recargadas.");return true;}
        if (!(sender instanceof Player p)) {sender.sendMessage("Solo en el juego.");return true;}
        if (args.length==2 && args[0].equalsIgnoreCase("set") && LEVELS.contains(args[1].toLowerCase(Locale.ROOT))) {
            Block block = p.getTargetBlockExact(7);
            if (block==null || block.getType().isAir()) {p.sendMessage(ChatColor.RED+"Mira a un bloque cercano.");return true;}
            String level=args[1].toLowerCase(Locale.ROOT);
            // Ensure one block does not belong to multiple difficulties.
            for (String l: LEVELS) {
                ConfigurationSection section=getConfig().getConfigurationSection("blocks."+l);
                if (section!=null && Objects.equals(section.getString("location"), key(block))) getConfig().set("blocks."+l,null);
            }
            getConfig().set("blocks."+level+".location",key(block));
            getConfig().set("blocks."+level+".world",block.getWorld().getName());
            getConfig().set("blocks."+level+".xyz",Arrays.asList(block.getX(),block.getY(),block.getZ()));
            saveConfig();
            p.sendMessage(message("admin-set", "&aRuleta {difficulty} configurada.").replace("{difficulty}",level));
            return true;
        }
        if (args.length==1 && args[0].equalsIgnoreCase("remove")) {
            Block block = p.getTargetBlockExact(7);
            if (block==null || block.getType().isAir()) {p.sendMessage(message("admin-remove-no-target", "&cMira al bloque de ruleta que quieres quitar."));return true;}
            String blockKey=key(block);
            if (running.containsKey(blockKey)) {p.sendMessage(message("admin-remove-busy", "&cEspera a que termine la ruleta antes de eliminarla."));return true;}
            String removed=null;
            for (String l: LEVELS) {
                if (Objects.equals(getConfig().getString("blocks."+l+".location"),blockKey)) {
                    getConfig().set("blocks."+l,null);
                    removed=l;
                }
            }
            if (removed==null) {p.sendMessage(message("admin-remove-not-found", "&cEse bloque no tiene una ruleta configurada."));return true;}
            saveConfig();
            p.sendMessage(message("admin-remove", "&aRuleta &f{difficulty} &aeliminada de este bloque.").replace("{difficulty}",removed));
            return true;
        }
        sender.sendMessage(ChatColor.YELLOW+"/droulette set facil|normal|dificil, /droulette remove, /droulette reload");
        return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if(!sender.hasPermission("bastionroulette.admin")) return List.of();
        List<String> values=args.length==1?List.of("set","remove","reload"):args.length==2&&args[0].equalsIgnoreCase("set")?LEVELS:List.of();
        return values.stream().filter(s->s.startsWith(args[args.length-1].toLowerCase(Locale.ROOT))).toList();
    }
    @EventHandler(priority=EventPriority.HIGH,ignoreCancelled=true)
    public void onBlock(PlayerInteractEvent e) {
        if (e.getAction()!=Action.RIGHT_CLICK_BLOCK || e.getClickedBlock()==null) return;
        String level=null;
        for(String candidate:LEVELS) if(key(e.getClickedBlock()).equals(getConfig().getString("blocks."+candidate+".location"))) {level=candidate;break;}
        if(level==null) return;
        e.setCancelled(true);
        Player player=e.getPlayer();
        if(pending.containsKey(player.getUniqueId()) || running.containsKey(key(e.getClickedBlock()))) {
            player.sendMessage(message("busy", "&cYa hay una ruleta en curso.")); return;
        }
        Spin s=new Spin(e.getClickedBlock(),player,level);
        running.put(key(e.getClickedBlock()),s);
        pending.put(player.getUniqueId(),key(e.getClickedBlock()));
        s.start();
    }
    private List<MapCard> maps() {
        List<MapCard> cards=new ArrayList<>();
        for(Map<?,?> row: getConfig().getMapList("roulette.maps")) {
            String id=Objects.toString(row.get("id"),""); String model=Objects.toString(row.get("model"),"");
            int weight=1;
            try {weight=Math.max(0,Integer.parseInt(Objects.toString(row.containsKey("weight") ? row.get("weight") : 1)));} catch(Exception ignored){}
            if(!id.isBlank()&&!model.isBlank()&&weight>0) cards.add(new MapCard(id,model,weight));
        }
        return cards;
    }
    record MapCard(String id,String model,int weight) {}
    private MapCard choose(List<MapCard> cards) {
        int total=cards.stream().mapToInt(MapCard::weight).sum();
        int index=random.nextInt(total);
        for(MapCard c:cards) {index-=c.weight();if(index<0)return c;}
        return cards.get(0);
    }
    private final class Spin {
        final Block block; final Player player; final String level; final List<ItemDisplay> displays=new ArrayList<>();
        BukkitRunnable task;
        Spin(Block b,Player p,String l){block=b;player=p;level=l;}
        void stop() {
            if(task!=null){task.cancel();task=null;}
            for(ItemDisplay d:displays)if(d.isValid())d.remove();
            displays.clear();running.remove(key(block));pending.remove(player.getUniqueId());
        }
        void start() {
            List<MapCard> cards=maps();
            if(cards.isEmpty()){player.sendMessage(ChatColor.RED+"No hay mapas configurados.");stop();return;}
            // Predetermine the result before the animation (not influenced by click timing).
            MapCard winning=choose(cards);
            double height=getConfig().getDouble("roulette.display-height",2.7);
            float scale=(float)getConfig().getDouble("roulette.image-scale",1.8);
            Location origin=block.getLocation().add(0.5,height,0.5);
            for(int i=0;i<3;i++) {
                ItemDisplay d=block.getWorld().spawn(origin.clone().add((i-1)*1.4,0,0),ItemDisplay.class, entity->{
                    entity.setBillboard(Display.Billboard.CENTER);
                    entity.setPersistent(false);
                    entity.setTransformation(new Transformation(new Vector3f(),new Quaternionf(),new Vector3f(scale,scale,scale),new Quaternionf()));
                    entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GUI);
                });
                displays.add(d);
            }
            int duration=Math.max(45,getConfig().getInt("roulette.duration-ticks",130));
            task=new BukkitRunnable() {
                int tick=0,selected=0; int lastStep=-1;
                @Override public void run() {
                    if(!player.isOnline() || !block.getWorld().isChunkLoaded(block.getX()>>4,block.getZ()>>4)) {stop();return;}
                    // Slower movement near the end. Step position in the image carousel.
                    double progress=Math.min(1.0,(double)tick/duration);
                    double totalSteps=24;
                    double stepPosition=totalSteps*(1-Math.pow(1-progress,2.6));
                    int step=(int)Math.floor(stepPosition);
                    double slide=stepPosition-step;
                    // Slide previews physically to the right while scrolling.
                    for(int i=0;i<displays.size();i++) {
                        displays.get(i).teleport(origin.clone().add((i-1+slide)*1.4,0,0));
                    }
                    if(step!=lastStep) {
                        lastStep=step;
                        selected=step;
                        for(int i=0;i<3;i++) update(displays.get(i),cards.get(Math.floorMod(step+i-1,cards.size())));
                        Sound sound=Sound.valueOf(getConfig().getString("roulette.sound","BLOCK_NOTE_BLOCK_HAT"));
                        player.getWorld().playSound(origin,sound,(float)getConfig().getDouble("roulette.volume",0.45),(float)getConfig().getDouble("roulette.pitch",1.4));
                    }
                    if(tick>=duration) {
                        for(int i=0;i<3;i++) update(displays.get(i),winning);
                        block.getWorld().spawnParticle(Particle.END_ROD,origin,28,0.8,0.6,0.2,0.07);
                        player.getWorld().playSound(origin,Sound.UI_TOAST_CHALLENGE_COMPLETE,0.8f,1.1f);
                        player.sendMessage(message("winner","&dMapa: &f{map}").replace("{map}",winning.id()));
                        stop();
                        try {
                            Plugin dungeon=getServer().getPluginManager().getPlugin("BastionDungeons");
                            if(dungeon==null || !dungeon.isEnabled()) throw new IllegalStateException("Falta BastionDungeons");
                            Method method=dungeon.getClass().getMethod("startRouletteDungeon",Player.class,String.class,String.class);
                            boolean success=(boolean)method.invoke(dungeon,player,winning.id(),switch(level){case "facil"->"EASY";case "dificil"->"HARD";default->"NORMAL";});
                            if(!success) player.sendMessage(ChatColor.RED+"No se pudo iniciar la partida: verifica equipo, horarios y mapa.");
                        } catch(Exception ex){getLogger().warning("Error conectando Dungeons: "+ex);player.sendMessage(ChatColor.RED+"No se pudo conectar con Dungeons. ¿Instalaste el JAR integrado?");}
                        return;
                    }
                    tick++;
                }
            };
            task.runTaskTimer(BastionMCDungeonRoulette.this,0L,1L);
        }
        void update(ItemDisplay display,MapCard card) {
            ItemStack item=new ItemStack(Material.PAPER);
            ItemMeta meta=item.getItemMeta();
            NamespacedKey model = NamespacedKey.fromString(card.model());
            if (model != null) meta.setItemModel(model);
            item.setItemMeta(meta);
            display.setItemStack(item);
        }
    }
}
