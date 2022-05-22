package fr.blendman.magnet.proxy.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import fr.blendman.magnet.Magnet;
import fr.blendman.magnet.proxy.VelocityMagnet;
import fr.blendman.magnet.utils.ApiCallBackToCompletableFuture;
import fr.blendman.skynet.client.ApiException;
import net.kyori.adventure.text.Component;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * @author Blendman974
 */
public class SendCommand implements SimpleCommand {
    private final VelocityMagnet velocityMagnet;

    public SendCommand(VelocityMagnet velocityMagnet) {
        this.velocityMagnet = velocityMagnet;
    }


    @Override
    public void execute(Invocation invocation) {

    }


}
