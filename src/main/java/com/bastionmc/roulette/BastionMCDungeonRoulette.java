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
        return color(raw);
    }
    private String color(String raw) {
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
        if (!sender.hasPermission("bastionroulette.admin")) {sender.sendMessage(message("admin-no-permission", "&cSolo OP."));return true;}
        if (args.length==1 && args[0].equalsIgnoreCase("reload")) {
            for (Spin spin: new ArrayList<>(running.values())) spin.stop();
            reloadConfig();
            sender.sendMessage(message("admin-reload", "&aConfiguración de ruletas recargada."));
            return true;
        }
        if (!(sender instanceof Player p)) {sender.sendMessage(message("admin-player-only", "&cSolo en el juego."));return true;}
        if (args.length==2 && args[0].equalsIgnoreCase("set") && LEVELS.contains(args[1].toLowerCase(Locale.ROOT))) {
            Block block = p.getTargetBlockExact(7);
            if (block==null || block.getType().isAir()) {p.sendMessage(message("admin-no-target", "&cMira a un bloque cercano."));return true;}
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
        sender.sendMessage(message("admin-usage", "&e/droulette set facil|normal|dificil, /droulette remove, /droulette reload"));
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
            String name=Objects.toString(row.containsKey("name") ? row.get("name") : id,id);
            String description=Objects.toString(row.containsKey("description") ? row.get("description") : "","");
            Map<?,?> holo=row.get("hologram") instanceof Map<?,?> h ? h : Map.of();
            if(!id.isBlank()&&!model.isBlank()&&weight>0) cards.add(new MapCard(id,model,weight,name,description,holo));
        }
        return cards;
    }
    record MapCard(String id,String model,int weight,String name,String description,Map<?,?> hologram) {}
    private MapCard choose(List<MapCard> cards) {
        int total=cards.stream().mapToInt(MapCard::weight).sum();
        int index=random.nextInt(total);
        for(MapCard c:cards) {index-=c.weight();if(index<0)return c;}
        return cards.get(0);
    }
    private final class Spin {
        final Block block;
        final Player player;
        final String level;
        final List<ItemDisplay> displays=new ArrayList<>();
        final List<TextDisplay> hologramDisplays=new ArrayList<>();
        BukkitRunnable task;
        Spin(Block b,Player p,String l){block=b;player=p;level=l;}

        void stop() {
            if(task!=null){task.cancel();task=null;}
            for(ItemDisplay d:displays) if(d.isValid()) d.remove();
            clearHologram();
            displays.clear();
            running.remove(key(block));
            pending.remove(player.getUniqueId());
        }
        private int getInt(String path,int defaultValue,int min,int max) {
            return Math.max(min,Math.min(max,getConfig().getInt(path,defaultValue)));
        }
        void start() {
            List<MapCard> cards=maps();
            if(cards.isEmpty()){player.sendMessage(message("no-maps", "&cNo hay mapas configurados."));stop();return;}
            MapCard winning=choose(cards);
            final int left=getInt("roulette.cards-left",2,0,8);
            final int right=getInt("roulette.cards-right",4,0,8);
            final int count=left+right+1;
            final int duration=getInt("roulette.duration-ticks",130,45,1200);
            final int hold=getInt("roulette.winner-pause-ticks",60,0,600);
            final boolean toRight=!"left".equalsIgnoreCase(getConfig().getString("roulette.slide-direction","right"));
            final double height=getConfig().getDouble("roulette.display-height",1.4);
            final double spacing=Math.max(0.1,getConfig().getDouble("roulette.card-spacing",1.18));
            final float scale=(float)Math.max(0.05,getConfig().getDouble("roulette.image-scale",0.72));
            final Location origin=block.getLocation().add(0.5,height,0.5);
            // One additional card is kept just outside the visible strip so
            // that a new preview moves in naturally as the carousel scrolls.
            final int anchor=toRight?left+1:left;
            final int displayCount=count+1;
            for(int i=0;i<displayCount;i++) {
                final int offset=i-anchor;
                ItemDisplay d=block.getWorld().spawn(origin.clone().add(offset*spacing,0,0),ItemDisplay.class, entity->{
                    entity.setBillboard(Display.Billboard.CENTER);
                    entity.setPersistent(false);
                    entity.setTransformation(new Transformation(new Vector3f(),new Quaternionf(),new Vector3f(scale,scale,scale),new Quaternionf()));
                    entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GUI);
                });
                displays.add(d);
            }
            final int winningIndex=cards.indexOf(winning);
            final int totalSteps=getInt("roulette.scroll-steps",28,3,500);
            task=new BukkitRunnable() {
                int tick=0;
                int lastStep=Integer.MIN_VALUE;
                boolean finished=false;
                int hologramEndTick=Integer.MAX_VALUE;
                @Override public void run() {
                    if(!player.isOnline() || !block.getWorld().isChunkLoaded(block.getX()>>4,block.getZ()>>4)) {stop();return;}
                    if (tick >= hologramEndTick) {clearHologram();hologramEndTick=Integer.MAX_VALUE;}
                    if(tick < duration) {
                        double progress=(double)tick/duration;
                        double stepPosition=totalSteps*(1-Math.pow(1-progress,2.6));
                        int step=(int)Math.floor(stepPosition);
                        double fractional=stepPosition-step;
                        int direction=toRight?1:-1;
                        for(int i=0;i<displayCount;i++) {
                            int offset=i-anchor;
                            ItemDisplay display=displays.get(i);
                            display.teleport(origin.clone().add((offset+direction*fractional)*spacing,0,0));
                            if(step!=lastStep) {
                                int index=Math.floorMod(winningIndex+offset+direction*(totalSteps-step),cards.size());
                                update(display,cards.get(index));
                            }
                        }
                        if(step!=lastStep) {
                            lastStep=step;
                            try {
                                Sound sound=Sound.valueOf(getConfig().getString("roulette.sound","BLOCK_NOTE_BLOCK_HAT"));
                                block.getWorld().playSound(origin,sound,(float)getConfig().getDouble("roulette.volume",0.45),(float)getConfig().getDouble("roulette.pitch",1.4));
                            } catch(IllegalArgumentException ignored) {
                                getLogger().warning("roulette.sound no es un sonido válido.");
                            }
                        }
                    } else if(!finished) {
                        finished=true;
                        // Center = winning, neighbors alternate other maps, displayed WITHOUT moving during hold.
                        for(int i=0;i<displayCount;i++) {
                            int offset=i-anchor;
                            int index=Math.floorMod(winningIndex+offset,cards.size());
                            update(displays.get(i),cards.get(index));
                            displays.get(i).teleport(origin.clone().add(offset*spacing,0,0));
                        }
                        block.getWorld().spawnParticle(Particle.END_ROD,origin.clone().add(0,0.2,0),20,0.45,0.3,0.2,0.04);
                        block.getWorld().playSound(origin,Sound.UI_TOAST_CHALLENGE_COMPLETE,0.8f,1.1f);
                        showHologram(winning,origin);
                        int hologramDuration=getInt("winner-hologram.duration-ticks",0,0,1200);
                        hologramEndTick=tick+Math.min(hold,hologramDuration==0?hold:hologramDuration);
                    } else if(tick>=duration+hold) {
                        stop();
                        announceWinner(winning,origin);
                        launchDungeon(winning);
                        return;
                    }
                    tick++;
                }
            };
            task.runTaskTimer(BastionMCDungeonRoulette.this,0L,1L);
        }
        private void clearHologram() {
            for(TextDisplay display:hologramDisplays) if(display.isValid()) display.remove();
            hologramDisplays.clear();
        }
        private double mapNumber(Map<?,?> settings,String option,double fallback) {
            Object value=settings.get(option);
            if(value instanceof Number n) return n.doubleValue();
            if(value!=null) try{return Double.parseDouble(value.toString());}catch(NumberFormatException ignored){}
            return fallback;
        }
        private boolean mapBoolean(Map<?,?> settings,String option,boolean fallback) {
            Object value=settings.get(option);
            return value==null?fallback:Boolean.parseBoolean(value.toString());
        }
        private String applyPlaceholders(String value,MapCard card) {
            return color(value.replace("{map}",card.name()).replace("{name}",card.name())
                .replace("{id}",card.id()).replace("{description}",card.description()));
        }
        private void showHologram(MapCard card,Location origin) {
            clearHologram();
            if(!getConfig().getBoolean("winner-hologram.enabled",true)) return;
            Map<?,?> options=card.hologram();
            if(!mapBoolean(options,"enabled",true)) return;
            // Map-specific displacement is additive to the common position.
            double x=getConfig().getDouble("winner-hologram.offset-x",0.0)+mapNumber(options,"offset-x",0);
            double y=getConfig().getDouble("winner-hologram.offset-y",0.0)+mapNumber(options,"offset-y",0);
            double z=getConfig().getDouble("winner-hologram.offset-z",0.0)+mapNumber(options,"offset-z",0);
            double height=getConfig().getDouble("winner-hologram.height",0.9)+mapNumber(options,"height",0);
            double lineSpacing=Math.max(0.10,mapNumber(options,"line-spacing",getConfig().getDouble("winner-hologram.line-spacing",0.27)));
            // Each hologram line may be a plain string (legacy V5) or a YAML map
            // with independent text, scale, height and offsets (V6).
            List<?> lines;
            Object rawLines=options.get("lines");
            if(rawLines instanceof List<?> custom) {
                lines=custom;
            } else {
                lines=getConfig().getList("winner-hologram.default-lines",List.of());
            }
            if(lines.isEmpty()) lines=List.of("&d&lMapa Ganador","&f{name}","&7Mata a el jefe para ganar");
            boolean shadow=mapBoolean(options,"shadow",getConfig().getBoolean("winner-hologram.shadow",true));
            boolean seeThrough=mapBoolean(options,"see-through",getConfig().getBoolean("winner-hologram.see-through",false));
            // 0 = completely transparent (default), 255 = fully opaque black.
            // A map may override the global value, and each line may override its map.
            int defaultBackgroundOpacity=(int)Math.max(0,Math.min(255,
                mapNumber(options,"background-opacity",getConfig().getInt("winner-hologram.background-opacity",0))));
            double defaultScale=Math.max(0.05,mapNumber(options,"scale",getConfig().getDouble("winner-hologram.scale",1.0)));
            Location base=origin.clone().add(x,height+y,z);
            for(int i=0;i<lines.size();i++) {
                Object source=lines.get(i);
                if(source==null) continue;
                Map<?,?> lineOptions=source instanceof Map<?,?> settings ? settings : Map.of();
                String rawText=source instanceof Map<?,?> ? Objects.toString(lineOptions.get("text"),"") : source.toString();
                if(rawText.isBlank() || !mapBoolean(lineOptions,"enabled",true)) continue;
                String line=applyPlaceholders(rawText,card);
                // height is relative to the common + map hologram origin.
                // For legacy strings, retain the original automatic spacing.
                double lineHeight=mapNumber(lineOptions,"height",(lines.size()-1-i)*lineSpacing);
                double lineX=mapNumber(lineOptions,"offset-x",0);
                double lineY=mapNumber(lineOptions,"offset-y",0);
                double lineZ=mapNumber(lineOptions,"offset-z",0);
                float lineScale=(float)Math.max(0.05,Math.min(8.0,mapNumber(lineOptions,"scale",defaultScale)));
                boolean lineShadow=mapBoolean(lineOptions,"shadow",shadow);
                boolean lineSeeThrough=mapBoolean(lineOptions,"see-through",seeThrough);
                int lineBackgroundOpacity=(int)Math.max(0,Math.min(255,
                    mapNumber(lineOptions,"background-opacity",defaultBackgroundOpacity)));
                Location location=base.clone().add(lineX,lineHeight+lineY,lineZ);
                TextDisplay display=block.getWorld().spawn(location,TextDisplay.class, entity->{
                    entity.setPersistent(false);
                    entity.setBillboard(Display.Billboard.CENTER);
                    entity.setDefaultBackground(false);
                    entity.setBackgroundColor(Color.fromARGB(lineBackgroundOpacity,0,0,0));
                    entity.setShadowed(lineShadow);
                    entity.setSeeThrough(lineSeeThrough);
                    entity.setText(line);
                    entity.setTransformation(new Transformation(new Vector3f(),new Quaternionf(),new Vector3f(lineScale,lineScale,lineScale),new Quaternionf()));
                });
                hologramDisplays.add(display);
            }
        }
        private void announceWinner(MapCard winner,Location origin) {
            String title=color(getConfig().getString("roulette.title-format","&dDungeon &f{map}"))
                .replace("{map}",winner.name()).replace("{id}",winner.id()).replace("{description}",winner.description());
            String subtitle=color(getConfig().getString("roulette.subtitle-format","&7{description}"))
                .replace("{map}",winner.name()).replace("{id}",winner.id()).replace("{description}",winner.description());
            String chat=message("winner","&dDungeon &f{map}")
                .replace("{map}",winner.name()).replace("{id}",winner.id()).replace("{description}",winner.description());
            double radius=Math.max(1,getConfig().getDouble("roulette.nearby-title-radius",20));
            int fadeIn=getInt("roulette.title-fade-in-ticks",10,0,200);
            int stay=getInt("roulette.title-stay-ticks",65,1,1200);
            int fadeOut=getInt("roulette.title-fade-out-ticks",15,0,200);
            boolean showTitle=getConfig().getBoolean("winner-hologram.also-show-title",false);
            boolean showChat=getConfig().getBoolean("winner-hologram.also-show-chat",false);
            if (!showTitle && !showChat) return;
            for(Player nearby:block.getWorld().getPlayers()) {
                if(nearby.getLocation().distanceSquared(origin)>radius*radius) continue;
                if (showTitle) nearby.sendTitle(title,subtitle,fadeIn,stay,fadeOut);
                if (showChat) nearby.sendMessage(chat);
            }
        }
        private void launchDungeon(MapCard winner) {
            try {
                Plugin dungeon=getServer().getPluginManager().getPlugin("BastionDungeons");
                if(dungeon==null || !dungeon.isEnabled()) throw new IllegalStateException("Falta BastionDungeons");
                Method method=dungeon.getClass().getMethod("startRouletteDungeon",Player.class,String.class,String.class);
                boolean success=(boolean)method.invoke(dungeon,player,winner.id(),switch(level){case "facil"->"EASY";case "dificil"->"HARD";default->"NORMAL";});
                if(!success) player.sendMessage(message("start-failed", "&cNo se pudo iniciar la partida: verifica equipo, horarios y mapa."));
            } catch(Exception ex) {
                getLogger().warning("Error conectando Dungeons: "+ex);
                player.sendMessage(message("dungeons-unavailable", "&cNo se pudo conectar con Dungeons. ¿Instalaste el JAR integrado?"));
            }
        }
        void update(ItemDisplay display,MapCard card) {
            ItemStack item=new ItemStack(Material.PAPER);
            ItemMeta meta=item.getItemMeta();
            NamespacedKey model=NamespacedKey.fromString(card.model());
            if(model!=null) meta.setItemModel(model);
            item.setItemMeta(meta);
            display.setItemStack(item);
        }
    }
}
