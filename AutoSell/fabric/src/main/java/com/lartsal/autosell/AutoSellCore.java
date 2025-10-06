package com.lartsal.autosell;

import com.lartsal.autosell.config.Config;
import com.lartsal.autosell.config.Config.ParticleDistributionType;
import com.lartsal.autosell.config.ConfigSerializer;
import com.lartsal.autosell.datastructures.Point3D;
import com.lartsal.autosell.datastructures.Trade;
import com.lartsal.autosell.enums.WorkMode;
import com.lartsal.autosell.platform.Services;
import com.lartsal.autosell.utils.Utils;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class AutoSellCore {
    // Keybindings
    private static final KeyMapping MODE_SWITCH_KEY = KeyMapping.Builder.create(
            "key.autosell.mode_switch",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            "key.category.autosell.main"
    ).build();

    private static final KeyMapping TOGGLE_HIGHLIGHTING_KEY = KeyMapping.Builder.create(
            "key.autosell.toggle_highlighting",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_T,
            "key.category.autosell.main"
    ).build();

    // GUI interaction
    private static final int TRADE_OUTPUT_SLOT = 2;
    private static final int FIRST_INVENTORY_SLOT = 3;
    private static final int LEFT_MOUSE_BUTTON = 0;

    // General
    private static WorkMode workMode = WorkMode.OFF;
    private static boolean enabled;
    private static boolean highlightingEnabled;
    private static int tradeProcessDelay;
    private static double acceptablePriceMultiplier;
    private static final List<Trade> trades = new ArrayList<>();

    // Effects
    private static SimpleParticleType highlightingParticles;
    private static double particlesPerTick;
    private static double particlesPerTickAccumulator = 0.0;
    private static double yLevel;
    private static ParticleDistributionType particlesShape;
    private static double radiusX, radiusY, radiusZ;
    private static boolean randomSpeed;
    private static double speedX, speedY, speedZ;

    private static int currentTradeProcessDelay = 0;

    private static AbstractVillager potentialVillager;
    private static AbstractVillager lastTradedVillager;

    private static Config config;

    public static void init() {
        Constants.LOGGER.info("{} loaded in the {} environment using {} loader", Constants.MOD_NAME, Services.PLATFORM.getEnvironmentName(), Services.PLATFORM.getPlatformName());

        registerConfigScreen();
        registerClientStartCallbacks();
        registerKeyBindings();
        registerEntityInteractionCallbacks();
        registerTickEndCallbacks();
    }

    public static void applyConfig(Config config) {
        AutoSellCore.config = config;

        enabled = config.isModEnabled;
        highlightingEnabled = config.isVillagerHighlightingEnabled;
        tradeProcessDelay = config.tradeProcessDelay;
        acceptablePriceMultiplier = config.acceptablePriceMultiplier;

        highlightingParticles = (SimpleParticleType) BuiltInRegistries.PARTICLE_TYPE.getValue(ResourceLocation.parse(config.highlightingParticlesName));
        particlesPerTick = config.particlesPerTick;
        yLevel = config.yLevel;
        particlesShape = config.particlesShape;
        radiusX = config.radiusX;
        radiusY = config.radiusY;
        radiusZ = config.radiusZ;
        randomSpeed = config.randomSpeed;
        speedX = config.speedX;
        speedY = config.speedY;
        speedZ = config.speedZ;

        trades.clear();

        for (String tradeEntry : config.trades) {
            Matcher matcher = Utils.TRADES_ENTRY_PATTERN.matcher(tradeEntry);
            if (!matcher.matches()) {
                throw new RuntimeException("VERY Unexpected invalid trade entry: \"" + tradeEntry + "\"");
            }

            Item first = BuiltInRegistries.ITEM.getValue(ResourceLocation.fromNamespaceAndPath(
                    matcher.group(1) == null ? "minecraft" : matcher.group(1),
                    matcher.group(2)
            ));
            Item second = matcher.group(4) == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(ResourceLocation.fromNamespaceAndPath(
                    matcher.group(3) == null ? "minecraft" : matcher.group(3),
                    matcher.group(4)
            ));
            Item result = BuiltInRegistries.ITEM.getValue(ResourceLocation.fromNamespaceAndPath(
                    matcher.group(5) == null ? "minecraft" : matcher.group(5),
                    matcher.group(6)
            ));

            trades.add(new Trade(first, second, result));
        }
    }

    private static void registerConfigScreen() {
        Services.PLATFORM.registerConfigScreen();
    }

    private static void registerClientStartCallbacks() {
        Services.PLATFORM.registerClientStartCallback(AutoSellCore::initConfig);
    }

    private static void registerKeyBindings() {
        Services.PLATFORM.registerKeyBinding(MODE_SWITCH_KEY);
        Services.PLATFORM.registerKeyBinding(TOGGLE_HIGHLIGHTING_KEY);
    }

    private static void registerEntityInteractionCallbacks() {
        Services.PLATFORM.registerEntityInteractionCallback(AutoSellCore::handleEntityClick);
    }

    private static void registerTickEndCallbacks() {
        Services.PLATFORM.registerClientTickEndCallback(AutoSellCore::handleTick);
        Services.PLATFORM.registerClientTickEndCallback(AutoSellCore::displayParticles);
    }

    private static void initConfig() {
        applyConfig(ConfigSerializer.getConfig());
    }

    private static InteractionResult handleEntityClick(Player player, Entity entity, InteractionHand hand) {
        if (entity instanceof AbstractVillager villager) {
            potentialVillager = villager;
        }
        return InteractionResult.PASS;
    }

    private static void handleTick() {
        if (!enabled) return;

        handleKeyPress();

        Minecraft minecraft = Minecraft.getInstance();
        if (workMode == WorkMode.OFF || !(minecraft.screen instanceof MerchantScreen screen)) return;

        if (currentTradeProcessDelay > 0) {
            currentTradeProcessDelay--;
            return;
        }

        MerchantMenu menu = screen.getMenu();

        takeResult(menu);

        int firstValidOfferIdx = findFirstValidOffer(menu);
        if (firstValidOfferIdx == -1) return;

        selectOffer(menu, firstValidOfferIdx);
    }

    // TODO: Move to separate class?
    private static void displayParticles() {
        if (!highlightingEnabled || lastTradedVillager == null) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.isPaused() || minecraft.level == null) return;

        particlesPerTickAccumulator += particlesPerTick;
        int particlesToSpawn = (int) particlesPerTickAccumulator;
        if (particlesToSpawn <= 0) return;

        for (int i = 0; i < particlesToSpawn; i++) {
            Point3D point = switch (particlesShape) {
                case CUBOID -> Utils.getRandomPointOnCuboid(radiusX, radiusY, radiusZ);
                case ELLIPSOID -> Utils.getRandomPointOnEllipsoid(radiusX, radiusY, radiusZ);
            };
            double offsetX = point.x();
            double offsetY = point.y();
            double offsetZ = point.z();

            double finalSpeedX = Utils.getRandomSign() * (randomSpeed ? Utils.getRandomDouble(speedX) : speedX);
            double finalSpeedY = Utils.getRandomSign() * (randomSpeed ? Utils.getRandomDouble(speedY) : speedY);
            double finalSpeedZ = Utils.getRandomSign() * (randomSpeed ? Utils.getRandomDouble(speedZ) : speedZ);

            minecraft.level.addParticle(
                    highlightingParticles,
                    lastTradedVillager.getX() + offsetX,
                    lastTradedVillager.getY() + offsetY + yLevel,
                    lastTradedVillager.getZ() + offsetZ,
                    finalSpeedX,
                    finalSpeedY,
                    finalSpeedZ
            );
        }
        particlesPerTickAccumulator -= particlesToSpawn;
    }

    private static void handleKeyPress() {
        if (MODE_SWITCH_KEY.consumeClick()) {
            workMode = workMode.next();
            sendLocalMessage(Component.literal(Constants.MOD_NAME + ": ")
                            .append(workMode.getDisplayText()), true);
        }

        if (TOGGLE_HIGHLIGHTING_KEY.consumeClick()) {
            highlightingEnabled = !highlightingEnabled;
            config.isVillagerHighlightingEnabled = highlightingEnabled;
            sendLocalMessage(Component.translatable( "autosell.villager_highlight")
                    .append(": ")
                    .append(highlightingEnabled
                            ? Component.translatable("autosell.on")
                            : Component.translatable("autosell.off")), true);
        }
    }

    private static void takeResult(MerchantMenu menu) {
        ItemStack tradeOutputStack = menu.getSlot(TRADE_OUTPUT_SLOT).getItem();
        if (tradeOutputStack.isEmpty()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameMode == null || minecraft.player == null) return;

        minecraft.gameMode.handleInventoryMouseClick(
                menu.containerId,
                TRADE_OUTPUT_SLOT,
                LEFT_MOUSE_BUTTON,
                ClickType.QUICK_MOVE,
                minecraft.player
        );

        lastTradedVillager = potentialVillager;
        currentTradeProcessDelay = tradeProcessDelay;
    }

    private static int findFirstValidOffer(MerchantMenu menu) {
        MerchantOffers offers = menu.getOffers();
        for (int i = 0; i < offers.size(); i++) {
            MerchantOffer offer = offers.get(i);

            // Skipping if the trade is locked
            if (offer.isOutOfStock()) continue;

            Item firstItemType = offer.getCostA().getItem();
            Item secondItemType = offer.getCostB().getItem();
            Item resultItemType = offer.getResult().getItem();

            Trade currentTrade = new Trade(firstItemType, secondItemType, resultItemType);

            // Or if we don't want to process these trades
            if (!trades.contains(currentTrade)) continue;

            boolean secondExists = offer.getItemCostB().isPresent();

            int offeredFirstPrice = offer.getCostA().getCount();
            int baseFirstPrice = offer.getBaseCostA().getCount();

            int offeredSecondPrice = offer.getCostB().getCount();
            int baseSecondPrice = secondExists ? offer.getItemCostB().get().count() : -1;

            // Or if the price is fucked up
            if (workMode == WorkMode.SELL_ONLY_REAL_PRICES) {
                if (offeredFirstPrice > baseFirstPrice ||
                        secondExists && offeredSecondPrice > baseSecondPrice) continue;
            }

            if (workMode == WorkMode.SELL_ACCEPTABLE_PRICES) {
                if (offeredFirstPrice > baseFirstPrice * acceptablePriceMultiplier ||
                        secondExists && offeredSecondPrice > baseSecondPrice * acceptablePriceMultiplier) continue;
            }

            // Or if we don't have enough items
            if (!hasEnoughItems(menu, offeredFirstPrice, firstItemType)) continue;
            if (secondExists && !hasEnoughItems(menu, offeredSecondPrice, secondItemType)) continue;

            return i;
        }
        return -1;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean hasEnoughItems(MerchantMenu menu, int requiredAmount, Item itemType) {
        int total = 0;

        for (int i = FIRST_INVENTORY_SLOT; i < menu.slots.size(); i++) {
            ItemStack stack = menu.getSlot(i).getItem();
            if (stack.isEmpty() || !stack.is(itemType)) continue;

            total += stack.getCount();
            if (total >= requiredAmount) return true;
        }

        return false;
    }

    private static void selectOffer(MerchantMenu menu, int offerIdx) {
        menu.setSelectionHint(offerIdx);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getConnection() != null) {
            minecraft.getConnection().send(new ServerboundSelectTradePacket(offerIdx));
        }
    }

    private static void sendLocalMessage(Component message, boolean actionBar) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(message, actionBar);
        }
    }
}
