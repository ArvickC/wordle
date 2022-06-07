package me.mystc.wordle;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import me.mystc.wordle.commands.Start;
import me.mystc.wordle.commands.Stop;
import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.util.*;

public final class Wordle extends JavaPlugin implements Listener {
    // Var
    public static boolean isGame = false;
    public static int move = -1;
    public static String answer = null;
    public static ArrayList<String> dictionary = new ArrayList<>();
    public static ArrayList<String> answerDictionary = new ArrayList<>();
    public static Location prevLoc = null;

    @Override
    public void onEnable() {
        // Plugin startup logic
        Bukkit.getPluginManager().registerEvents(this, this);

        getCommand("start").setExecutor(new Start());
        getCommand("end").setExecutor(new Stop());

        // Fill dictionary
        File dictionaryFile = new File(getDataFolder().getAbsolutePath() + "/dictionary.txt");
        try {
            Scanner sc = new Scanner(dictionaryFile);
            while(sc.hasNextLine()){
                dictionary.add(sc.nextLine());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Fill answer dictionary
        File answerFile = new File(getDataFolder().getAbsolutePath() + "/answers.txt");
        try {
            Scanner sc = new Scanner(answerFile);
            while(sc.hasNextLine()){
                answerDictionary.add(sc.nextLine());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        seutpWordleBlock();
        setupWordleite();

        System.out.println(ChatColor.translateAlternateColorCodes('&', "&7[&aW&8or&6d&al&8e&7] Plugin&a activated&7."));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        if(isGame) {
            try {
                stop();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println(ChatColor.translateAlternateColorCodes('&', "&7[&aW&8or&6d&al&8e&7] Plugin&c deactivated&7."));
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        // Var
        String message = e.getMessage().trim().toLowerCase();
        Player p = e.getPlayer();

        // Check
        if(!isGame) return;
        if(message == null) return;
        if(message.equalsIgnoreCase("%answer")) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[&aW&8or&6d&al&8e&7] The answer is: " + answer));
            return;
        }
        if(message.contains(" ")) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[&aW&8or&6d&al&8e&7]&c Input contains a space!"));
            return;
        }
        if(message.length() != 5) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[&aW&8or&6d&al&8e&7]&c Input must be 5 letters!"));
            return;
        }
        if(!dictionary.contains(message)) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[&aW&8or&6d&al&8e&7]&c Input isn't in word list!"));
            return;
        }
        if(move-1 >= 7) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[&aW&8or&6d&al&8e&7]&c Out of moves!"));
            return;
        }

        // Increase move
        move++;

        // Print and Animate word
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);

        new BukkitRunnable() {
            @Override
            public void run(){
                printWord(message, p);
                colorWord();
            }
        }.runTask(this);

        new BukkitRunnable() {
            @Override
            public void run(){
                checkWord(message, p);
            }
        }.runTaskLater(this, 20);
    }

    @EventHandler
    public void pickUp(EntityPickupItemEvent e) {
        if(!(e.getEntity() instanceof Player)) return;
        if(e.getItem().getItemStack().getType().equals(Material.COAL)) {

            ItemStack item = e.getItem().getItemStack();

            ItemMeta itemMeta = item.getItemMeta();
            itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aW&7or&6d&al&7e&7i&at&6e"));
            ArrayList<String> lore = new ArrayList<String>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Get 9 to craft a &aW&7or&6d&al&7e&7 block to go to the wordle realm"));
            itemMeta.setLore(lore);
            item.setItemMeta(itemMeta);

            e.getItem().setItemStack(item);
        }

        if(e.getItem().getItemStack().getType().equals(Material.COAL_BLOCK)) {

            ItemStack item = new ItemStack(Material.COAL_BLOCK);
            ItemMeta itemMeta = item.getItemMeta();
            itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aW&7or&6d&al&7e&7"));
            ArrayList<String> lore = new ArrayList<String>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Place to go to the wordle realm"));
            itemMeta.setLore(lore);
            item.setItemMeta(itemMeta);

            e.getItem().setItemStack(item);
        }

    }

    public static void seutpWordleBlock(){
        Bukkit.removeRecipe(new NamespacedKey(Bukkit.getPluginManager().getPlugin("Wordle"), "coal_block"));

        ItemStack item = new ItemStack(Material.COAL_BLOCK);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aW&7or&6d&al&7e&7"));
        ArrayList<String> lore = new ArrayList<String>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Place to go to the wordle realm"));
        itemMeta.setLore(lore);
        item.setItemMeta(itemMeta);

        NamespacedKey key = new NamespacedKey(Bukkit.getPluginManager().getPlugin("Wordle"), "wordle_block");

        ShapedRecipe recipe = new ShapedRecipe(key, item);
        recipe.shape("CCC", "CCC", "CCC");
        recipe.setIngredient('C', Material.COAL);
        Bukkit.addRecipe(recipe);
    }

    public static void setupWordleite(){
        Bukkit.removeRecipe(new NamespacedKey(Bukkit.getPluginManager().getPlugin("Wordle"), "coal"));

        ItemStack item = new ItemStack(Material.COAL);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aW&7or&6d&al&7e&7i&at&6e"));
        ArrayList<String> lore = new ArrayList<String>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Get 9 to craft a &aW&7or&6d&al&7e&7 block to go to the wordle realm"));
        itemMeta.setLore(lore);
        item.setItemMeta(itemMeta);
        item.setAmount(9);

        NamespacedKey key = new NamespacedKey(Bukkit.getPluginManager().getPlugin("Wordle"), "wordleite");

        ShapedRecipe recipe = new ShapedRecipe(key, item);
        recipe.shape("C");
        recipe.setIngredient('C', Material.COAL_BLOCK);
        Bukkit.addRecipe(recipe);
    }

    @EventHandler
    public void onClick(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        ItemStack block = e.getItemInHand();
        Inventory inv = p.getInventory();
        if(!block.getType().equals(Material.COAL_BLOCK) || !block.getItemMeta().getDisplayName().equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&', "&aW&7or&6d&al&7e&7"))) return;

        inv.removeItem(block);
        p.getInventory().setContents(inv.getContents());
        start(p);

        e.setCancelled(true);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if(isGame) e.setCancelled(true);
    }

    public static boolean start (Player p){
        // Check
        if(isGame) return false;

        p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[&aW&8or&6d&al&8e&7] Sending you to wordle!"));

        // Gen world
        WorldCreator wc = new WorldCreator("wordle");
        wc.generator(new EmptyChunkGenerator());
        wc.generateStructures(false);
        wc.createWorld();
        World w = Bukkit.getWorld("wordle");

        // Setup world
        w.getBlockAt(0, 124, 0).setType(Material.DIAMOND_BLOCK);
        for(int x=-23;x<=23;x++) {
            for(int y=154;y>=99;y--) {
                w.getBlockAt(x, y, -59).setType(Material.BLACK_WOOL);
            }
        }

        int x1 = -21;
        int y1 = 152;
        int z = -58;

        for(int i=0;i<6;i++){
            for(int j=0;j<5;j++) {

                int x2 = x1 + 6;
                for (; x1 <= x2; x1++) {

                    int y2 = y1 - 6;
                    for (; y1 >= y2; y1--) {
                        //System.out.println(x1 + ", " + y1);
                        //System.out.println(x2 + ", " + y2);
                        w.getBlockAt(x1, y1, z).setType(Material.WHITE_WOOL);
                    }
                    y1 += 7;
                }
                x1 += 2;
                //y1 += 6;
            }
            x1 -= 45;
            y1 -= 9;
        }

        // Teleport
        //yaw/pitch -180, -24
        prevLoc = p.getLocation();
        Location loc = new Location(w, 0.5, 125, 0.5, -180, -1);
        p.teleport(loc);

        p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[&aW&8or&6d&al&8e&7] Wordle Started!"));

        isGame = true;
        move = 1;
        Random rand = new Random();
        answer = answerDictionary.get(rand.nextInt(answerDictionary.size()));

        System.out.println(ChatColor.translateAlternateColorCodes('&', "&7[&aW&8or&6d&al&8e&7] Created wordle game with answer: &a" + answer));

        return true;
    }

    public static boolean stop() throws IOException {
        // Check
        if(!isGame) return false;

        // Teleport out of world
        World w = Bukkit.getWorld("wordle");
        for(Player p : Bukkit.getOnlinePlayers()){
            if(p.getWorld().equals(w)) {
                p.teleport(prevLoc);
            }
        }

        // Delete world
        Bukkit.unloadWorld(w, false);
        FileUtils.forceDelete(w.getWorldFolder());
        isGame = false;
        prevLoc = null;
        return true;
    }

    public synchronized static void printWord(String str, Player p){
        // Var
        char[] chars = str.toCharArray();
        File dir = new File(Bukkit.getPluginManager().getPlugin("Wordle").getDataFolder(), "schematics");
        File[] files = dir.listFiles();
        int charNumber = 1;

        // Checks
        if(!dir.exists()) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[&aW&8or&6d&al&8e&7]&c Schematics error!"));
            return;
        }

        // Print Schematic
        for(char c : chars){
            // Var
            File schematic = new File(Bukkit.getPluginManager().getPlugin("Wordle").getDataFolder().getAbsolutePath() + "/schematics/" + c + ".schem");

            // Checks
            if(!schematic.exists()) {
                p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[&aW&8or&6d&al&8e&7]&c Unable to find &7" + c + "&c schematic file"));
                return;
            }

            // Var
            int[] cords = getPasteLocation(charNumber, c);
            assert cords != null;
            BlockVector3 to = BlockVector3.at(cords[0], cords[1], cords[2]);
            com.sk89q.worldedit.world.World world = BukkitAdapter.adapt(Bukkit.getWorld("wordle"));
            ClipboardFormat format = ClipboardFormats.findByFile(schematic);
            ClipboardReader reader = null;
            Clipboard clipboard = null;

            // Load schematic
            try {
                assert format != null;
                reader = format.getReader(new FileInputStream(schematic));
                clipboard = reader.read();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Build schematic
            try(EditSession editSession = WorldEdit.getInstance().newEditSession(world)) {
            //try(EditSession editSession = WorldEdit.getInstance().getEditSessionFactory().getEditSession(world, -1)) {
                assert clipboard != null;
                Operation operation = new ClipboardHolder(clipboard).createPaste(editSession).to(to).ignoreAirBlocks(false).build();
                Operations.complete(operation);
            } catch (WorldEditException e) {
                e.printStackTrace();
            }
            charNumber++;
        }
    }

    public static int[] getPasteLocation(int num, char c){
        // Checks
        if(!isGame) return null;
        if(num>=6) return null;
        if(move-1>=7) return null;

        // Var
        int z = -57;
        int[] x = new int[]{-21, -12, -3, 6, 15};
        int[] y = new int[]{151, 142, 133, 124, 115, 106};
        char[] minusY = new char[]{'s', 'o', 'r', 'k', 'l', 'm', 'n', 't', 'v', 'u', 'j', 'q', 'b', 'p', 'y', 'w', 'x', 'z'};

        // Return result
        for(char ch : minusY) {
            if(c == ch) return new int[]{x[num-1], (y[move-2])-1, z};
        }
        return new int[]{x[num-1], y[move-2], z};
    }

    public static void colorWord(){
        // Checks
        if(!isGame) return;
        if(move-1>=7) return;

        // Var
        int z = -57;
        int x1 = -21;
        int x2 = 21;
        int[] y = new int[]{152, 143, 134, 125, 116, 107};
        World w = Bukkit.getWorld("wordle");

        // Colorize Blocks
        int y1 = y[move-2];
        int y2 = y1-6;

        for(; x1<=x2 ; x1++){
            for(; y1>=y2 ; y1--){
                if(w.getBlockAt(x1, y1, z).getType().equals(Material.MAGENTA_WOOL)) {
                    w.getBlockAt(x1, y1, z).setType(Material.BLACK_WOOL);
                }
            }
            y1 = y[move-2];
            y2 = y1-6;
        }
    }

    public static void checkWord(String str, Player p){
        // Check
        if(!isGame) return;

        // Var
        int z = -57;
        int[] x = new int[]{-21, -12, -3, 6, 15};
        int[] y = new int[]{152, 143, 134, 125, 116, 107};
        int[] greens = new int[]{0, 0, 0, 0, 0};
        World w = Bukkit.getWorld("wordle");
        char[] chars = str.toCharArray();
        char[] answerChar = answer.toCharArray();

        // Go through each char
        for (int i = 0; i < str.toCharArray().length; i++) {
            boolean isGreen = false;
            boolean isYellow = false;

            for (char ch : answerChar) {
                if (isGreen) continue;
                if (isYellow) continue;
                if (greens[i] == 1) continue;

                // Check if it is in the answer
                if (ch == chars[i]) {
                    // Check if it is in the same location
                    if (answerChar[i] == chars[i]) {
                        // Same location
                        int x1 = x[i];
                        int x2 = x1 + 6;
                        int y1 = y[move - 2];
                        int y2 = y1 - 6;

                        for (; x1 <= x2; x1++) {
                            for (; y1 >= y2; y1--) {
                                if (!w.getBlockAt(x1, y1, z).getType().equals(Material.AIR)) {
                                    w.getBlockAt(x1, y1, z).setType(Material.LIME_WOOL);
                                }
                            }
                            y1 = y[move - 2];
                            y2 = y1 - 6;
                        }
                        isGreen = true;
                        greens[i] = 1;
                    } else {
                        boolean isRep = false;
                        for(int k=0;k<answerChar.length;k++) {
                            if(answerChar[k] == chars[i] && answerChar[k] == chars[k]) {
                                isRep = true;
                            }
                        }
                        if(isRep) continue;

                        // Different Location
                        int x1 = x[i];
                        int x2 = x1 + 6;
                        int y1 = y[move - 2];
                        int y2 = y1 - 6;

                        for (; x1 <= x2; x1++) {
                            for (; y1 >= y2; y1--) {
                                if (!w.getBlockAt(x1, y1, z).getType().equals(Material.AIR)) {
                                    w.getBlockAt(x1, y1, z).setType(Material.YELLOW_WOOL);
                                }
                            }
                            y1 = y[move - 2];
                            y2 = y1 - 6;
                        }
                        isYellow = true;
                    }
                } else {
                    int x1 = x[i];
                    int x2 = x1 + 6;
                    int y1 = y[move - 2];
                    int y2 = y1 - 6;

                    for (; x1 <= x2; x1++) {
                        for (; y1 >= y2; y1--) {
                            if (!w.getBlockAt(x1, y1, z).getType().equals(Material.AIR)) {
                                w.getBlockAt(x1, y1, z).setType(Material.LIGHT_GRAY_WOOL);
                            }
                        }
                        y1 = y[move - 2];
                        y2 = y1 - 6;
                    }
                }
            }
        }

        // Play sound
        p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 1f, 1f);

        // Check for win
        if(str.equalsIgnoreCase(answer)) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[&aW&8or&6d&al&8e&7]&a Congrats!&7 You found the answer."));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[&aW&8or&6d&al&8e&7]&7 Ending game in 5 seconds."));
            new BukkitRunnable() {
                @Override
                public void run(){
                    try {
                        stop();
                        win(p);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }.runTaskLater(Bukkit.getPluginManager().getPlugin("Wordle"), 20 * 5);
            return;
        }

        // Check for lose
        if(move >= 7) {
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[&aW&8or&6d&al&8e&7]&c L!&7 You ran out of moves."));
            p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&7[&aW&8or&6d&al&8e&7]&7 Ending game in 5 seconds."));
            new BukkitRunnable() {
                @Override
                public void run(){
                    try {
                        p.setHealth(0.0);
                        stop();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }.runTaskLater(Bukkit.getPluginManager().getPlugin("Wordle"), 20 * 5);
        }
    }

    static void win(Player p){
        // Var
        Inventory inv = p.getInventory();
        ItemStack[] is = inv.getContents();
        Random rand = new Random();

        // Double Item
        ItemStack item = getRandomItem(is);
        if(move<=2) {
            item.setAmount(item.getAmount()*3);
        }else if(move<=3) {
            item.setAmount(item.getAmount()*2);
        } else {
            item.setAmount(item.getAmount());
        }

        // Give to player
        p.getInventory().addItem(item);
    }

    public static ItemStack getRandomItem(ItemStack[] content) {
        Random r = new Random();
        ArrayList<ItemStack> items = new ArrayList<ItemStack>();

        for (ItemStack item : content) {
            if (item != null)
                items.add(item);
        }

        return items.get(r.nextInt(items.size()));
    }
}
