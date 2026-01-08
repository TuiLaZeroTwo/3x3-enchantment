// Inside handlePickup method...

    private void handlePickup(Player player, Location loc) {
        // 1. Get Remaining Time BEFORE removing logic
        long expiry = boosterLogic.getBoosters().getOrDefault(loc, 0L);
        long remainingMillis = expiry - System.currentTimeMillis();
        long remainingSeconds = remainingMillis / 1000;

        if (remainingSeconds <= 0) remainingSeconds = 0;

        // 2. Remove block and logic
        Bukkit.getRegionScheduler().execute(plugin, loc, () -> {
             loc.getBlock().setType(Material.AIR);
        });
        boosterLogic.removeBooster(loc);
        
        // 3. Create Item with Saved Time
        FileConfiguration config = plugin.getConfig();
        ItemStack booster = new ItemStack(Material.valueOf(config.getString("shop.crop-booster.material", "BEACON")));
        ItemMeta meta = booster.getItemMeta();
        
        meta.displayName(Component.text("Crop Booster", NamedTextColor.GREEN));
        meta.getPersistentDataContainer().set(ShopListener.KEY_BOOSTER, PersistentDataType.INTEGER, 1);
        
        // SAVE TIME KEY
        meta.getPersistentDataContainer().set(ShopListener.KEY_TIME_LEFT, PersistentDataType.LONG, remainingSeconds);

        // Update Lore for visibility
        long hours = remainingSeconds / 3600;
        long minutes = (remainingSeconds % 3600) / 60;
        meta.lore(java.util.Arrays.asList(
            Component.text("Contains saved data.", NamedTextColor.GRAY),
            Component.text("Time Left: " + String.format("%02d:%02d", hours, minutes), NamedTextColor.YELLOW)
        ));

        booster.setItemMeta(meta);
        
        // 4. Give Item
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(booster);
        } else {
            player.getWorld().dropItemNaturally(player.getLocation(), booster);
        }

        player.sendMessage(Component.text("Booster picked up! Time saved.", NamedTextColor.YELLOW));
        player.closeInventory();
    }