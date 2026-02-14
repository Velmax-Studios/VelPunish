package com.velpunish.common.commands;

import com.velpunish.common.database.ProfileRepository;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class OfflinePlayerSuggestionProvider<C> implements SuggestionProvider<C> {

    private final ProfileRepository profileRepository;

    public OfflinePlayerSuggestionProvider(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Override
    public CompletableFuture<Iterable<Suggestion>> suggestionsFuture(CommandContext<C> context, CommandInput input) {
        String currentInput = input.readString().toLowerCase();

        return profileRepository.getAllKnownNames()
                .thenApply(names -> names.stream()
                        .filter(name -> name.toLowerCase().startsWith(currentInput))
                        .map(Suggestion::simple)
                        .collect(Collectors.toList()));
    }
}
