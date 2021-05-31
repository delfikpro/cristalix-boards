package ru.cristalix.boards.bukkitapi;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.val;
import net.minecraft.server.v1_12_R1.PacketDataSerializer;
import net.minecraft.server.v1_12_R1.PacketPlayOutCustomPayload;
import org.apache.commons.io.IOUtils;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import ru.cristalix.core.display.DisplayChannels;
import ru.cristalix.core.display.messages.Mod;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Boards implements Listener {

    public static final List<Board> boards = new ArrayList<>();
    public static final Set<Player> active = new HashSet<>();
    private static Plugin plugin;

    public static Board newBoard() {
        init();
        return new SimpleBoard();
    }

    public static void init() {
        if (plugin != null) return;
        plugin = JavaPlugin.getProvidingPlugin(Boards.class);
        Bukkit.getPluginManager().registerEvents(new Boards(), plugin);
        Bukkit.getMessenger().registerIncomingPluginChannel(plugin, "boards:loaded", (channel, player, data) -> {
            active.add(player);
            for (Board board : boards) {
                if (board.getLocation().getWorld() == player.getWorld()) {
                    board.updateStructure(player);
                    board.updateContent(player);
                }
            }
        });
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) throws Exception {
        InputStream resource = plugin.getResource("boards-client-mod-bundle.jar");
        byte[] serialize = Mod.serialize(new Mod(IOUtils.readFully(resource, resource.available())));
        ByteBuf buf = Unpooled.buffer();
        buf.writeBytes(serialize);
        PacketDataSerializer ds = new PacketDataSerializer(buf);
        PacketPlayOutCustomPayload packet = new PacketPlayOutCustomPayload(DisplayChannels.MOD_CHANNEL, ds);
        ((CraftPlayer) e.getPlayer()).getHandle().playerConnection.sendPacket(packet);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        active.remove(e.getPlayer());
    }

    @EventHandler
    public void onAddToWorld(EntityAddToWorldEvent event) {
        if (!(event.getEntity() instanceof CraftPlayer)) return;
        CraftPlayer player = (CraftPlayer) event.getEntity();
        if (!active.contains(player)) return;
        val packet = new PacketPlayOutCustomPayload("boards:reset", new PacketDataSerializer(Unpooled.buffer()));
        player.getHandle().playerConnection.sendPacket(packet);
        for (Board board : boards) {
            board.updateStructure(player);
            board.updateContent(player);
        }
    }

    public static void addBoard(Board board) {
        boards.add(board);
        board.updateStructure();
        board.updateContent();
    }

}
