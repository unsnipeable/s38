package s38;

import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Mod(modid = "mwe")
public class S38 {
    public static boolean master;
    public static boolean extra;
    public final static Minecraft mc = Minecraft.getMinecraft();
    public final static String PREFIX = "§e[§7S38§e] §f";

    @SubscribeEvent
    public void packet(event e) {
        if (e.packet instanceof S38PacketPlayerListItem) {
            if (!master) return;
            S38PacketPlayerListItem s38 = ((S38PacketPlayerListItem)e.packet);
            if (s38.getAction()==S38PacketPlayerListItem.Action.ADD_PLAYER) {
                for (S38PacketPlayerListItem.AddPlayerData apd : s38.getEntries()) {
                    print("§a+ §f" + apd.getDisplayName().getFormattedText() + (extra ? (" §7(" + apd.getPing() + "ms)") : ""));
                }
            }
            if (s38.getAction()==S38PacketPlayerListItem.Action.REMOVE_PLAYER) {
                for (S38PacketPlayerListItem.AddPlayerData apd : s38.getEntries()) {
                    print("§c- §f" + apd.getDisplayName().getFormattedText() + (extra ? (" §7(" + apd.getPing() + "ms)") : ""));
                }
            }
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        ClientCommandHandler.instance.registerCommand(new command());
    }

    public static void print(String msg) {
        if (mc.thePlayer == null) return;
        mc.thePlayer.addChatMessage(new ChatComponentText(PREFIX + msg));
    }

    @Getter
    @Cancelable
    public static class event extends Event {
        private final Packet<?> packet;
        public event(Packet<?> packet) {
            this.packet = packet;
        }
    }

    public static class command implements ICommand {

        @Override
        public String getCommandName() {
            return "s38";
        }

        @Override
        public String getCommandUsage(ICommandSender sender) {
            return "/s38";
        }

        @Override
        public List<String> getCommandAliases() {
            return Collections.emptyList();
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) {
            if (args.length == 1 && args[0].equalsIgnoreCase("ping")) {
                extra = !extra;
                print("§7ping: " + (extra ? "§aEnabled" : "§cDisabled"));
                return;
            }
            master = !master;
            print(master ? "§aEnabled" : "§cDisabled");
        }

        @Override
        public boolean canCommandSenderUseCommand(ICommandSender sender) {
            return true;
        }

        @Override
        public List<String> addTabCompletionOptions(ICommandSender iCommandSender, String[] strings, BlockPos blockPos) {
            return Collections.emptyList();
        }

        @Override
        public boolean isUsernameIndex(String[] args, int index) {
            return false;
        }

        @Override
        public int compareTo(ICommand o) {
            return this.getCommandName().compareTo(o.getCommandName());
        }
    }


    @Mixin(NetworkManager.class)
    public static class packet {
        @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
        public void receivePacket(ChannelHandlerContext p_channelRead0_1_, Packet p_channelRead0_2_, CallbackInfo ci) {
            S38.event receivePacketEvent = new S38.event(p_channelRead0_2_);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(receivePacketEvent);
            if (receivePacketEvent.isCanceled()) {
                ci.cancel();
            }
        }
    }
}