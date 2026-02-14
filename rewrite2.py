import re
import sys

def rewrite_required(filepath, is_server):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    ctype = "CommandSender" if is_server else "CommandSource"
    
    # regex to match .required("player", stringParser()) and replace
    p1 = r'\.required\("player", stringParser\(\)\)'
    repl1 = f'.required(CommandComponent.<{ctype}, String>builder().name("player").parser(stringParser()).suggestionProvider(new OfflinePlayerSuggestionProvider<>(plugin.getProfileRepository())).build())'
    
    p2 = r'\.required\("player_or_id", stringParser\(\)\)'
    repl2 = f'.required(CommandComponent.<{ctype}, String>builder().name("player_or_id").parser(stringParser()).suggestionProvider(new OfflinePlayerSuggestionProvider<>(plugin.getProfileRepository())).build())'
    
    content = re.sub(p1, repl1, content)
    content = re.sub(p2, repl2, content)
    
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write(content)

rewrite_required(r'server/src/main/java/com/velpunish/server/commands/CommandSystem.java', True)
rewrite_required(r'velocity/src/main/java/com/velpunish/velocity/commands/CommandSystem.java', False)
print("Done")
