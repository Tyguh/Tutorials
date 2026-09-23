package net.viraxis.tutorials;

import com.massivecraft.massivecore.MassiveException;
import com.massivecraft.massivecore.command.MassiveCommand;
import com.massivecraft.massivecore.command.requirement.RequirementHasPerm;
import com.massivecraft.massivecore.command.type.sender.TypePlayer;
import com.massivecraft.massivecore.util.Feedback;
import org.bukkit.entity.Player;

public class TutorialCommand extends MassiveCommand {
    private static final TutorialCommand INSTANCE = new TutorialCommand();
    public static TutorialCommand get() { return INSTANCE; }

    public TutorialCommand() {
        setAliases("tutorial", "guide");
        setDesc("View your current tutorial step.");
        addChild(new Reload());
        addChild(new Reset());
    }

    @Override public void perform() {
        if (senderIsConsole) { sender.sendMessage("Use /tutorial reload or /tutorial reset <player>."); return; }
        TutorialsPlugin.get().showTutorial(me);
    }

    private static final class Reload extends MassiveCommand {
        Reload() {
            setAliases("reload");
            setDesc("Reload the active tutorial file.");
            addRequirements(RequirementHasPerm.get("tutorials.admin"));
        }
        @Override public void perform() {
            boolean loaded = TutorialsPlugin.get().reloadTutorial();
            if (sender instanceof Player player) Feedback.send(player, loaded
                    ? Feedback.ok("Tutorial reloaded.") : Feedback.fail("Reload failed. Previous tutorial is still active."));
            else sender.sendMessage(loaded ? "Tutorial reloaded." : "Reload failed. Previous tutorial is still active.");
        }
    }

    private static final class Reset extends MassiveCommand {
        Reset() {
            setAliases("reset");
            setDesc("Reset an online player's tutorial progress.");
            addParameter(TypePlayer.get(), "player");
            addRequirements(RequirementHasPerm.get("tutorials.admin"));
        }
        @Override public void perform() throws MassiveException {
            Player target = readArg();
            boolean reset = TutorialsPlugin.get().resetTutorial(target);
            if (sender instanceof Player player) Feedback.send(player, reset
                    ? Feedback.ok("Reset tutorial for <h>" + target.getName() + "</h>.")
                    : Feedback.fail("No active tutorial."));
            else sender.sendMessage(reset ? "Reset tutorial for " + target.getName() + "." : "No active tutorial.");
        }
    }
}
