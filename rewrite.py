import os, re

def rewrite_command_system(filepath, is_server):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Add imports
    content = content.replace('import java.util.UUID;', 'import java.util.UUID;\nimport com.velpunish.common.models.PlayerProfile;\nimport org.incendo.cloud.component.CommandComponent;\nimport com.velpunish.common.commands.OfflinePlayerSuggestionProvider;\nimport java.util.function.Consumer;')

    # Add resolveTarget method right after constructor
    resolve_target = """
    private void resolveTarget(String targetName, Consumer<PlayerProfile> callback, Runnable onNotFound) {
        plugin.getProfileCache().getProfile(targetName).thenAccept(profileOpt -> {
            if (profileOpt.isPresent()) {
                callback.accept(profileOpt.get());
            } else {
                onNotFound.run();
            }
        });
    }

    private void registerCommands() {"""
    content = content.replace("    private void registerCommands() {", resolve_target)

    # Replace required("player", stringParser()) -> required(CommandComponent.<C, String>builder().name("player").parser(stringParser()).suggestionProvider(new OfflinePlayerSuggestionProvider<>(plugin.getProfileRepository())).build())
    if is_server:
        ctype = "CommandSender"
    else:
        ctype = "CommandSource"

    provider_str = f'CommandComponent.<{ctype}, String>builder().name("player").parser(stringParser()).suggestionProvider(new OfflinePlayerSuggestionProvider<>(plugin.getProfileRepository())).build()'
    content = content.replace('.required("player", stringParser())', f'.required({provider_str})')

    provider_str_2 = f'CommandComponent.<{ctype}, String>builder().name("player_or_id").parser(stringParser()).suggestionProvider(new OfflinePlayerSuggestionProvider<>(plugin.getProfileRepository())).build()'
    content = content.replace('.required("player_or_id", stringParser())', f'.required({provider_str_2})')

    # Replace specific handlers... Wait, this is quite complex to do safely with just text replace in python without regex.
    # It's better to just do it via a straight replace of the handler block.

    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

rewrite_command_system(r'c:\Users\Aarav Roy\Desktop\Projects\VelPunish\velocity\src\main\java\com\velpunish\velocity\commands\CommandSystem.java', False)
rewrite_command_system(r'c:\Users\Aarav Roy\Desktop\Projects\VelPunish\server\src\main\java\com\velpunish\server\commands\CommandSystem.java', True)
