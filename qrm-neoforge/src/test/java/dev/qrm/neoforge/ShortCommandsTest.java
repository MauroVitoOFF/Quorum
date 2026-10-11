package dev.qrm.neoforge;

import static org.junit.jupiter.api.Assertions.*;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.Test;

class ShortCommandsTest {
    private static Set<String> names(CommandNode<CommandSourceStack> node) {
        return node.getChildren().stream().map(CommandNode::getName).collect(Collectors.toSet());
    }

    private static CommandDispatcher<CommandSourceStack> dispatcher(boolean shortAliases) {
        CommandDispatcher<CommandSourceStack> d = new CommandDispatcher<>();
        QrmCommands.register(d);
        ShortCommands.register(d, shortAliases);
        return d;
    }

    @Test
    void shortRootsExistWhenEnabled() {
        CommandDispatcher<CommandSourceStack> d = dispatcher(true);
        assertEquals(Set.of("qrm", "job", "duty", "money", "balance", "bal", "pay", "char", "org", "perm"),
                names(d.getRoot()));
    }

    @Test
    void onlyQrmExistsWhenDisabled() {
        assertEquals(Set.of("qrm"), names(dispatcher(false).getRoot()));
    }

    @Test
    void jobRootHasPlayerAndAdminBranches() {
        CommandNode<CommandSourceStack> job = dispatcher(true).getRoot().getChild("job");
        assertNotNull(job.getCommand(), "/job da solo mostra il proprio lavoro");
        assertEquals(Set.of("info", "set", "remove", "list"), names(job));
    }

    @Test
    void dutyAloneToggles() {
        CommandNode<CommandSourceStack> duty = dispatcher(true).getRoot().getChild("duty");
        assertNotNull(duty.getCommand());
        assertEquals(Set.of("on", "off"), names(duty));
    }

    @Test
    void moneyRootCoversBalancePayAndAdminMoney() {
        CommandNode<CommandSourceStack> money = dispatcher(true).getRoot().getChild("money");
        assertNotNull(money.getCommand(), "/money da solo mostra i saldi");
        assertEquals(Set.of("pay", "give", "take"), names(money));
    }

    @Test
    void balanceAliasesRunTheSameCommandAsMoney() {
        var root = dispatcher(true).getRoot();
        assertNotNull(root.getChild("balance").getCommand());
        assertNotNull(root.getChild("bal").getCommand());
        assertEquals(Set.of("player"), names(root.getChild("pay")));
    }

    @Test
    void charRootMirrorsQrmCharacter() {
        var root = dispatcher(true).getRoot();
        assertEquals(names(root.getChild("qrm").getChild("character")), names(root.getChild("char")));
    }

    @Test
    void orgRootMergesPlayerAndAdminBranches() {
        CommandNode<CommandSourceStack> org = dispatcher(true).getRoot().getChild("org");
        assertNotNull(org.getCommand(), "/org da solo elenca le proprie organizzazioni");
        assertEquals(Set.of("list", "info", "create", "archive", "delete", "rename", "rank", "member"), names(org));
    }

    @Test
    void permRootHasTheThreeVerbs() {
        assertEquals(Set.of("grant", "deny", "clear"), names(dispatcher(true).getRoot().getChild("perm")));
    }

    @Test
    void qrmCommandsKeepWorkingUnchanged() {
        CommandNode<CommandSourceStack> qrm = dispatcher(true).getRoot().getChild("qrm");
        assertEquals(Set.of("character", "balance", "job", "duty", "org", "pay", "admin"), names(qrm));
        assertEquals(Set.of("grant", "take", "audit", "job", "perm", "org", "staff", "log", "point"), names(qrm.getChild("admin")));
    }
}
