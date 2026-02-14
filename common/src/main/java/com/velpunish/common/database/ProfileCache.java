package com.velpunish.common.database;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.velpunish.common.models.PlayerProfile;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ProfileCache {

    private final ProfileRepository profileRepository;

    private final Cache<UUID, PlayerProfile> uuidCache;

    private final Cache<String, UUID> usernameCache;

    public ProfileCache(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
        this.uuidCache = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofHours(1))
                .maximumSize(5000)
                .build();

        this.usernameCache = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofHours(1))
                .maximumSize(5000)
                .build();
    }

    public void addProfile(PlayerProfile profile) {
        uuidCache.put(profile.getUuid(), profile);
        usernameCache.put(profile.getUsername().toLowerCase(), profile.getUuid());
    }

    public void invalidate(UUID uuid) {
        PlayerProfile profile = uuidCache.getIfPresent(uuid);
        if (profile != null) {
            usernameCache.invalidate(profile.getUsername().toLowerCase());
        }
        uuidCache.invalidate(uuid);
    }

    public CompletableFuture<Optional<PlayerProfile>> getProfile(UUID uuid) {
        PlayerProfile cached = uuidCache.getIfPresent(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(Optional.of(cached));
        }

        return profileRepository.getProfileByUuid(uuid).thenApply(profileOpt -> {
            profileOpt.ifPresent(this::addProfile);
            return profileOpt;
        });
    }

    public CompletableFuture<Optional<PlayerProfile>> getProfile(String username) {
        String lowerName = username.toLowerCase();
        UUID cachedUuid = usernameCache.getIfPresent(lowerName);

        if (cachedUuid != null) {
            return getProfile(cachedUuid);
        }

        return profileRepository.getProfileByName(username).thenApply(profileOpt -> {
            profileOpt.ifPresent(this::addProfile);
            return profileOpt;
        });
    }
}
