package me.saiintbrisson.bungee.command;

import lombok.Getter;
import me.saiintbrisson.bungee.command.command.BungeeCommand;
import me.saiintbrisson.bungee.command.executor.BungeeCommandExecutor;
import me.saiintbrisson.bungee.command.executor.BungeeCompleterExecutor;
import me.saiintbrisson.minecraft.command.CommandFrame;
import me.saiintbrisson.minecraft.command.annotation.Command;
import me.saiintbrisson.minecraft.command.annotation.Completer;
import me.saiintbrisson.minecraft.command.argument.AdapterMap;
import me.saiintbrisson.minecraft.command.command.CommandInfo;
import me.saiintbrisson.minecraft.command.executor.CommandExecutor;
import me.saiintbrisson.minecraft.command.executor.CompleterExecutor;
import me.saiintbrisson.minecraft.command.message.MessageHolder;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * @author Henry Fábio
 * Github: https://github.com/HenryFabio
 */
@Getter
public class BungeeFrame implements CommandFrame<Plugin, CommandSender, BungeeCommand> {

    private final Plugin plugin;
    private final AdapterMap adapterMap;
    private final MessageHolder messageHolder;

    private final Map<String, BungeeCommand> commandMap;

    public BungeeFrame(Plugin plugin, AdapterMap adapterMap) {
        this.plugin = plugin;

        this.adapterMap = adapterMap;
        this.messageHolder = new MessageHolder();

        this.commandMap = new HashMap<>();
    }

    public BungeeFrame(Plugin plugin, boolean registerDefault) {
        this(plugin, new AdapterMap(registerDefault));

        if (registerDefault) {
            registerAdapter(ProxiedPlayer.class, ProxyServer.getInstance()::getPlayer);
        }
    }

    public BungeeFrame(Plugin plugin) {
        this(plugin, true);
    }

    @Override
    public @Nullable Executor getExecutor() {
        return null;
    }

    @Override
    public void registerCommands(Object... objects) {
        for (Object object : objects) {
            for (Method method : object.getClass().getDeclaredMethods()) {
                Command command = method.getAnnotation(Command.class);
                if (command != null) {
                    registerCommand(new CommandInfo(command), new BungeeCommandExecutor(this, method, object));
                    continue;
                }

                Completer completer = method.getAnnotation(Completer.class);
                if (completer != null) {
                    registerCompleter(completer.name(), new BungeeCompleterExecutor(method, object));
                }
            }
        }
    }

    @Override
    public void registerCommand(CommandInfo commandInfo, CommandExecutor<CommandSender> commandExecutor) {
        BungeeCommand recursive = getCommand(commandInfo.getName());
        if (recursive == null) {
            return;
        }

        recursive.initCommand(commandInfo, commandExecutor);

        if (recursive.getPosition() == 0) {
            ProxyServer.getInstance().getPluginManager().registerCommand(
              plugin,
              recursive
            );
        }
    }

    @Override
    public void registerCompleter(String name, CompleterExecutor<CommandSender> completerExecutor) {
        BungeeCommand recursive = getCommand(name);
        if (recursive == null) {
            return;
        }

        recursive.initCompleter(completerExecutor);
    }

    @Override
    public BungeeCommand getCommand(String name) {
        int index = name.indexOf('.');
        String recursiveCommand = (index != -1 ? name.substring(0, index) : name).toLowerCase();

        BungeeCommand command = commandMap.get(recursiveCommand);
        if (command == null) {
            command = new BungeeCommand(this, recursiveCommand, 0);
            commandMap.put(recursiveCommand, command);
        }

        return index != -1 ? command.createRecursive(name) : command;
    }

}
