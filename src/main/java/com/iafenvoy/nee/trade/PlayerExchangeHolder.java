package com.iafenvoy.nee.trade;

import com.iafenvoy.nee.Constants;
import com.iafenvoy.nee.screen.context.PlayerContext;
import com.iafenvoy.nee.screen.handler.ScreenHandlerUtils;
import com.iafenvoy.nee.screen.handler.TradeCommandScreenHandler;
import com.iafenvoy.nee.util.InventoryUtil;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public final class PlayerExchangeHolder {
    private static final Map<PlayerEntity, PlayerExchangeHolder> HOLDER_BY_PLAYER = new HashMap<>();
    private static final Map<PlayerEntity, Single> SINGLE_BY_PLAYER = new HashMap<>();
    private final Single user1, user2;

    private PlayerExchangeHolder(PlayerEntity user1, PlayerEntity user2) {
        this.user1 = new Single(user1, new SimpleInventory(20));
        this.user2 = new Single(user2, new SimpleInventory(20));
        HOLDER_BY_PLAYER.put(user1, this);
        HOLDER_BY_PLAYER.put(user2, this);
        SINGLE_BY_PLAYER.put(user1, this.user1);
        SINGLE_BY_PLAYER.put(user2, this.user2);
    }

    public void onCancel() {
        InventoryUtil.insertItems(this.user1.player.getInventory(), this.user1.inventory);
        InventoryUtil.insertItems(this.user2.player.getInventory(), this.user2.inventory);
        this.close();
    }

    public void onConfirm() {
        InventoryUtil.insertItems(this.user1.player.getInventory(), this.user2.inventory);
        InventoryUtil.insertItems(this.user2.player.getInventory(), this.user1.inventory);
        ScreenHandlerUtils.playCheckedSound(PlayerContext.of(this.user1.player));
        ScreenHandlerUtils.playCheckedSound(PlayerContext.of(this.user2.player));
        this.close();
    }

    public void close() {
        HOLDER_BY_PLAYER.remove(this.user1.player);
        HOLDER_BY_PLAYER.remove(this.user2.player);
        SINGLE_BY_PLAYER.remove(this.user1.player);
        SINGLE_BY_PLAYER.remove(this.user2.player);
        closeTradeScreen(this.user1.player);
        closeTradeScreen(this.user2.player);
    }

    public void checkTrade() {
        if (this.user1.accepted && this.user2.accepted) this.onConfirm();
    }

    public void openScreen() {
        this.user1.player.openHandledScreen(this.createScreen(this.user1, this.user2));
        this.user2.player.openHandledScreen(this.createScreen(this.user2, this.user1));
    }

    private NamedScreenHandlerFactory createScreen(Single current, Single another) {
        String anotherName = another.player.getGameProfile().getName();
        return new ExtendedScreenHandlerFactory() {
            @Override
            public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
                buf.writeString(anotherName);
            }

            @Override
            public Text getDisplayName() {
                return Text.translatable("screen.not_enough_economy.trade");
            }

            @Override
            public @NotNull ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
                return new TradeCommandScreenHandler(syncId, playerInventory, current.inventory, another.inventory, PlayerContext.of(current.player), anotherName);
            }
        };
    }

    public static void closeTradeScreen(PlayerEntity player) {
        assert Constants.TRADE_STATE_CHANGE != null;
        if (player instanceof ServerPlayerEntity serverPlayer)
            ServerPlayNetworking.send(serverPlayer, Constants.TRADE_STATE_CHANGE, PacketByteBufs.create().writeEnumConstant(TradeMessageType.ANOTHER_CLOSE_SCREEN));
    }

    public static void launchTrade(PlayerEntity user1, PlayerEntity user2) {
        new PlayerExchangeHolder(user1, user2).openScreen();
    }

    static {
        assert Constants.TRADE_STATE_CHANGE != null;
        ServerPlayNetworking.registerGlobalReceiver(Constants.TRADE_STATE_CHANGE, (server, player, handler, buf, responseSender) -> {
            PlayerExchangeHolder holder = HOLDER_BY_PLAYER.get(player);
            Single single = SINGLE_BY_PLAYER.get(player);
            if (holder == null || single == null) return;
            TradeMessageType type = buf.readEnumConstant(TradeMessageType.class);
            server.execute(switch (type) {
                case SELF_ACCEPT -> (Runnable) () -> {
                    single.accepted = true;
                    holder.checkTrade();
                };
                case SELF_CANCEL_ACCEPT -> (Runnable) () -> single.accepted = false;
                case SELF_CLOSE_SCREEN -> (Runnable) holder::onCancel;
                default -> TradeMessageType.EMPTY;
            });
        });
    }

    private static class Single {
        private final PlayerEntity player;
        private final Inventory inventory;
        private boolean accepted;

        private Single(PlayerEntity player, Inventory inventory) {
            this.player = player;
            this.inventory = inventory;
        }
    }
}