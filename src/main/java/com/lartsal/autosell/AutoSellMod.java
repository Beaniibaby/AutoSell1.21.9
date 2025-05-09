package com.lartsal.autosell;

import com.lartsal.autosell.config.ConfigManager;
import com.lartsal.autosell.config.ModConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.screen.MerchantScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.village.TradeOffer;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;

public class AutoSellMod implements ClientModInitializer {
    private static final Logger LOGGER = LoggerFactory.getLogger("AutoSellMod");

    private enum WorkMode {
        OFF,
        SELL_ONLY_REAL_PRICE,
        SELL_ACCEPTABLE_PRICE,
        SELL_ANY_PRICE;

        public WorkMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private static class Trade {
        Item first;
        Item second;
        Item result;

        public Trade(Item first, Item second, Item result) {
            this.first = first;
            this.second = second;
            this.result = result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Trade trade)) return false;
            return (Registries.ITEM.getId(first) == Registries.ITEM.getId(trade.first)) &&
                   (Registries.ITEM.getId(second) == Registries.ITEM.getId(trade.second)) &&
                   (Registries.ITEM.getId(result) == Registries.ITEM.getId(trade.result));
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    Registries.ITEM.getId(first),
                    Registries.ITEM.getId(second),
                    Registries.ITEM.getId(result)
            );
        }
    }

    //private static final int TRADE_INPUT_SLOT_1 = 0;
    //private static final int TRADE_INPUT_SLOT_2 = 1;
    private static final int TRADE_OUTPUT_SLOT = 2;
    private static final int FIRST_INVENTORY_SLOT = 3;

    private static final int LEFT_MOUSE_BUTTON = 0;

    private static WorkMode workMode = WorkMode.OFF;

    private static KeyBinding switchModeKey;
    private static KeyBinding toggleHighlightKey;

    private static MerchantEntity lastTradedVillager;
    private static MerchantEntity potentialVillager;

    private static boolean enabled;
    private static boolean highlightingEnabled;
    private static SimpleParticleType highlightingParticles;
    private static int tradesDelay;
    private static double acceptablePriceMultiplier;
    private static final List<Trade> trades = new ArrayList<>();

    private static int tradeProcessCooldown = 0;
    private static int particleSpawnCooldown = 0;

    private static ModConfig config;

    private static MinecraftClient client;

    public static void applyConfig(ModConfig config) {
        AutoSellMod.config = config;

        enabled = config.isModEnabled;
        highlightingEnabled = config.isVillagerHighlightingEnabled;
        highlightingParticles = (SimpleParticleType) Registries.PARTICLE_TYPE.get(Identifier.of(config.highlightingParticlesId));
        tradesDelay = config.tradesDelay;
        acceptablePriceMultiplier = config.acceptablePriceMultiplier;

        trades.clear();

        for (String tradeEntry : config.trades) {
            Matcher matcher = ModConfig.TRADES_ENTRY_PATTERN.matcher(tradeEntry);
            if (!matcher.matches()) {
                // If SOMEHOW this shit will shoot (AFTER ALL CHECKS), I'll delete this fucking mod
                throw new RuntimeException("VERY Unexpected invalid trade entry: \"" + tradeEntry + "\"");
            }

            Item first = Registries.ITEM.get(Identifier.of(
                    matcher.group(1) == null ? "minecraft" : matcher.group(1),
                    matcher.group(2)
            ));
            Item second = matcher.group(4) == null ? Items.AIR : Registries.ITEM.get(Identifier.of(
                    matcher.group(3) == null ? "minecraft" : matcher.group(3),
                    matcher.group(4)
            ));
            Item result = Registries.ITEM.get(Identifier.of(
                    matcher.group(5) == null ? "minecraft" : matcher.group(5),
                    matcher.group(6)
            ));

            trades.add(new Trade(first, second, result));
        }
    }

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            ConfigManager.loadConfig();
            applyConfig(ConfigManager.getConfig());
        });

        // Registering keybindings
        switchModeKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autosell.toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                "key.category.autosell.main"
        ));

        toggleHighlightKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.autosell.toggle_highlight",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_T,
                "key.category.autosell.main"
        ));

        // Registering tick handler
        ClientTickEvents.END_CLIENT_TICK.register(this::handleTick);

        // Check RMB clicks on entities
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient) return ActionResult.PASS;

            if (entity instanceof MerchantEntity villager) {
                potentialVillager = villager;
            }

            return ActionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (highlightingEnabled && lastTradedVillager != null) {
                if (client.isPaused()) {
                    return;
                }

                if (particleSpawnCooldown > 0) {
                    particleSpawnCooldown--;
                    return;
                }

                Random random = new Random();

                if (client.world == null) {
                    return;
                }

                client.world.addParticle(
                        highlightingParticles,
                        lastTradedVillager.getX(),
                        lastTradedVillager.getY() + 1.25,
                        lastTradedVillager.getZ(),
                        (random.nextBoolean() ? 1 : -1) * random.nextDouble(0.07),
                        (random.nextBoolean() ? 1 : -1) * random.nextDouble(0.07),
                        (random.nextBoolean() ? 1 : -1) * random.nextDouble(0.07)
                );

                particleSpawnCooldown = 1;
            }
        });
    }

    private void handleTick(MinecraftClient client) {
        AutoSellMod.client = client;

        if (!enabled) return;

        if (switchModeKey.wasPressed()) {
            workMode = workMode.next();

            if (client.player != null) {
                String workModeText = switch (workMode) {
                    case OFF -> "§cOFF§r";
                    case SELL_ONLY_REAL_PRICE -> "§aON§r, §6ONLY REAL PRICES§r";
                    case SELL_ACCEPTABLE_PRICE -> "§aON§r, §eACCEPTABLE PRICES§r";
                    case SELL_ANY_PRICE -> "§aON§r, §aANY PRICES§r";
                };

                client.player.sendMessage(Text.literal("AutoSell: " +
                        workModeText), true);
            }
            return;
        }

        if (toggleHighlightKey.wasPressed()) {
            highlightingEnabled = !highlightingEnabled;
            config.isVillagerHighlightingEnabled = highlightingEnabled;
            if (client.player != null) {
                client.player.sendMessage(Text.literal("VILLAGER HIGHLIGHT: " +
                        (highlightingEnabled ? "§aON" : "§cOFF")), true);
            }
        }

        // * 0. Quit if no trade menu is opened
        if (workMode == WorkMode.OFF || !(client.currentScreen instanceof MerchantScreen)) {
            return;
        }

        if (tradeProcessCooldown > 0) {
            tradeProcessCooldown--;
            return;
        }

        MerchantScreenHandler handler = ((MerchantScreen) client.currentScreen).getScreenHandler();

        // * 1. Take trade output if any
        if (!handler.getSlot(TRADE_OUTPUT_SLOT).getStack().isEmpty()) {
            takeResult(client, handler);
            return;
        }

        // * 2. Find first valid trade
        TradeOffer offer = findFirstValidOffer(handler);
        if (offer == null) {
            return;
        }

        // TODO: Remove all unnecessary shit
        // * 3. Fill trade slot with items
        /*int tradeFirstPrice = offer.getDisplayedFirstBuyItem().getCount();
        Item firstItemType = offer.getDisplayedFirstBuyItem().getItem();
        fillTradeSlotWithRequiredAmount(client, handler, TRADE_INPUT_SLOT_1, tradeFirstPrice, firstItemType);

        int tradeSecondPrice;
        Item secondItemType;
        boolean secondExists = offer.getSecondBuyItem().isPresent();
        if (secondExists) {
            tradeSecondPrice = offer.getDisplayedSecondBuyItem().getCount();
            secondItemType = offer.getDisplayedSecondBuyItem().getItem();
            fillTradeSlotWithRequiredAmount(client, handler, TRADE_INPUT_SLOT_2, tradeSecondPrice, secondItemType);
        }*/

        // * 4. Buy it!
        // *    It means we wait for the next tick to trigger takeResult
    }

    // Deprecated?
    private void fillTradeSlotWithRequiredAmount(MinecraftClient client, MerchantScreenHandler handler, int slot, int requiredAmount, Item itemType) {
        /*
        > Find required item in slot
        > REMEMBER FROM WHERE WE TOOK IT
        > Put it into trade slot
        > Check if we have something left in our cursor
        > If so, put it IN REMEMBERED SLOT
        > Quit when trade slot have requiredAmount or more
        */
        //int max = itemType.getMaxCount();

        if (client.player == null) {
            return;
        }

        ItemStack cursorStack = client.player.currentScreenHandler.getCursorStack();
        int rememberedSlot = -1;

        for (int i = FIRST_INVENTORY_SLOT; i < handler.slots.size(); i++) {
            ItemStack slotStack = handler.getSlot(i).getStack();

            if (slotStack.isEmpty() || slotStack.getItem() != itemType) {
                continue;
            }

            rememberedSlot = i;
            clickSlot(client, handler, i);
            clickSlot(client, handler, slot);

            // Return extra items
            if (!cursorStack.isEmpty()) {
                clickSlot(client, handler, rememberedSlot);
            }

            if (handler.getSlot(slot).getStack().getCount() >= requiredAmount) {
                break;
            }
        }
    }

    private void takeResult(MinecraftClient client, MerchantScreenHandler handler) {
        ItemStack outputStack = handler.getSlot(TRADE_OUTPUT_SLOT).getStack();

        if (!outputStack.isEmpty()) {
            if (client.interactionManager != null) {
                client.interactionManager.clickSlot(
                        handler.syncId,
                        TRADE_OUTPUT_SLOT,
                        LEFT_MOUSE_BUTTON,
                        SlotActionType.QUICK_MOVE,
                        client.player
                );
            }
        }

        setLastTradedVillager(potentialVillager);

        tradeProcessCooldown = tradesDelay;
    }

    private TradeOffer findFirstValidOffer(MerchantScreenHandler handler) {
        List<TradeOffer> offers = handler.getRecipes();
        for (int i = 0; i < offers.size(); i++) {
            TradeOffer offer = offers.get(i);

            // Skipping if the trade is locked
            if (offer.isDisabled()) continue;

            Item firstItem = offer.getDisplayedFirstBuyItem().getItem();
            Item secondItem = offer.getDisplayedSecondBuyItem().getItem();
            Item resultItem = offer.getSellItem().getItem();

            Trade currentTrade = new Trade(firstItem, secondItem, resultItem);

            // Or if we don't want to process these trades
            if (!trades.contains(currentTrade)) continue;

            boolean secondExist = offer.getSecondBuyItem().isPresent();

            int tradeFirstPrice = offer.getDisplayedFirstBuyItem().getCount();
            int realFirstPrice = offer.getFirstBuyItem().itemStack().getCount();

            int tradeSecondPrice = offer.getDisplayedSecondBuyItem().getCount();
            int realSecondPrice = -1;
            if (secondExist) {
                realSecondPrice = offer.getSecondBuyItem().get().itemStack().getCount();
            }

            // Or if the price is fucked up
            if (workMode == WorkMode.SELL_ONLY_REAL_PRICE &&
                    (tradeFirstPrice > realFirstPrice ||
                            secondExist && tradeSecondPrice > realSecondPrice)) continue;

            if (workMode == WorkMode.SELL_ACCEPTABLE_PRICE &&
                    (tradeFirstPrice > realFirstPrice * acceptablePriceMultiplier ||
                            secondExist && tradeSecondPrice > realSecondPrice * acceptablePriceMultiplier)) continue;

            // Or if we don't have enough items
            if (!hasEnoughItems(handler, tradeFirstPrice, offer.getDisplayedFirstBuyItem().getItem())) continue;
            if (secondExist && !hasEnoughItems(handler, tradeSecondPrice, offer.getDisplayedSecondBuyItem().getItem()))
                continue;

            handler.setRecipeIndex(i);
            if (client.getNetworkHandler() != null) {
                client.getNetworkHandler().sendPacket(new SelectMerchantTradeC2SPacket(i));
            }

            return offer;
        }
        return null;
    }

    private boolean hasEnoughItems(MerchantScreenHandler handler, int requiredCount, Item targetItem) {
        int total = 0;

        for (int i = FIRST_INVENTORY_SLOT; i < handler.slots.size(); i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (!stack.isEmpty() && stack.getItem() == targetItem) {
                total += stack.getCount();
                if (total >= requiredCount) return true;
            }
        }
        return false;
    }

    private void clickSlot(MinecraftClient client, MerchantScreenHandler handler, int slot) {
        client.interactionManager.clickSlot(
                handler.syncId,
                slot,
                LEFT_MOUSE_BUTTON,
                SlotActionType.PICKUP,
                client.player
        );
    }

    private void setLastTradedVillager(MerchantEntity villager) {
        lastTradedVillager = villager;
    }
}