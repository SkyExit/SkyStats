package de.skystats.ui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFactory;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import java.net.URI;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HeadFactoryTest {
    private MockedStatic<Bukkit> bukkit;
    private HeadFactory heads;
    private final ExecutorService executor = mock(ExecutorService.class);
    private final List<Runnable> work = new ArrayList<>();
    private final List<PlayerProfile> profiles = new ArrayList<>();
    private final List<SkullMeta> metas = new ArrayList<>();

    @BeforeEach void setup() throws Exception {
        bukkit = mockStatic(Bukkit.class);
        ItemFactory factory = mock(ItemFactory.class);
        bukkit.when(Bukkit::getItemFactory).thenReturn(factory);
        when(factory.getItemMeta(Material.PLAYER_HEAD)).thenAnswer(call -> {
            SkullMeta meta = mock(SkullMeta.class); when(meta.clone()).thenReturn(meta); metas.add(meta); return meta;
        });
        when(factory.isApplicable(any(), eq(Material.PLAYER_HEAD))).thenReturn(true);
        when(factory.asMetaFor(any(), eq(Material.PLAYER_HEAD))).thenAnswer(call -> call.getArgument(0));
        when(factory.updateMaterial(any(), eq(Material.PLAYER_HEAD))).thenReturn(Material.PLAYER_HEAD);
        bukkit.when(() -> Bukkit.createPlayerProfile(nullable(UUID.class), nullable(String.class))).thenAnswer(call -> {
            PlayerProfile profile = mock(PlayerProfile.class);
            when(profile.getTextures()).thenReturn(mock(PlayerTextures.class));
            profiles.add(profile); return profile;
        });
        doAnswer(call -> { work.add(call.getArgument(0)); return null; }).when(executor).execute(any());
        heads = new HeadFactory(Logger.getAnonymousLogger(), executor);
    }
    @AfterEach void cleanup() { heads.close(); bukkit.close(); }

    @Test void arrowsCarryEmbeddedWoodTexturesWithoutAnyLookup() throws Exception {
        heads.arrow(false); heads.arrow(true);
        for (int i = 0; i < 2; i++) {
            PlayerProfile profile = profiles.get(i);
            PlayerTextures textures = profile.getTextures();
            String hash = i == 0 ? HeadFactory.LEFT_TEXTURE : HeadFactory.RIGHT_TEXTURE;
            verify(textures).setSkin(URI.create("https://textures.minecraft.net/texture/" + hash).toURL());
            verify(profile).setTextures(textures);
            verify(metas.get(i)).setOwnerProfile(profile);
            verify(profile, never()).update();
        }
        assertTrue(work.isEmpty());
    }

    @Test void waitsForResolvedTexturesCoalescesRequestsAndPreservesStatisticsUuid() throws Exception {
        UUID uuid = UUID.nameUUIDFromBytes("OfflinePlayer:Tester".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var first = heads.player(uuid, "Tester");
        var second = heads.player(uuid, "Tester");
        assertFalse(first.isDone()); assertEquals(1, work.size());
        PlayerProfile lookup = mock(PlayerProfile.class), resolved = mock(PlayerProfile.class), owner = mock(PlayerProfile.class);
        PlayerTextures textures = mock(PlayerTextures.class);
        when(textures.getSkin()).thenReturn(URI.create("https://textures.minecraft.net/texture/" + HeadFactory.RIGHT_TEXTURE).toURL());
        when(resolved.getTextures()).thenReturn(textures);
        when(lookup.update()).thenReturn(CompletableFuture.completedFuture(resolved));
        bukkit.when(() -> Bukkit.createPlayerProfile(null, "Tester")).thenReturn(lookup);
        bukkit.when(() -> Bukkit.createPlayerProfile(uuid, "Tester")).thenReturn(owner);
        work.get(0).run();
        assertEquals(Material.PLAYER_HEAD, first.get().getType());
        assertNotSame(first.get(), second.get());
        verify(lookup, times(1)).update(); verify(owner).setTextures(textures);
        for (SkullMeta meta : metas) verify(meta).setOwnerProfile(owner);
        heads.player(uuid, "Tester").get();
        assertEquals(1, work.size());
    }

    @Test void reusesLoggedInSkinsWithoutRemoteRequests() throws Exception {
        Player player = mock(Player.class); UUID uuid = UUID.randomUUID();
        PlayerProfile profile = mock(PlayerProfile.class); PlayerTextures textures = mock(PlayerTextures.class);
        when(player.getUniqueId()).thenReturn(uuid); when(player.getName()).thenReturn("Online");
        when(player.getPlayerProfile()).thenReturn(profile); when(profile.getTextures()).thenReturn(textures);
        when(textures.getSkin()).thenReturn(URI.create("https://textures.minecraft.net/texture/" + HeadFactory.LEFT_TEXTURE).toURL());
        heads.remember(player);
        assertTrue(heads.player(uuid, "Online").isDone());
        assertTrue(work.isEmpty()); verify(profile, never()).update();
        verify(metas.get(0)).setOwnerProfile(profile);
    }

    @Test void failedLookupsRemainUsableAndAreNotRepeatedOnEveryRender() throws Exception {
        UUID uuid = UUID.randomUUID(); var pending = heads.player(uuid, "Missing");
        PlayerProfile lookup = mock(PlayerProfile.class);
        when(lookup.update()).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("unavailable")));
        bukkit.when(() -> Bukkit.createPlayerProfile(uuid, "Missing")).thenReturn(lookup);
        work.get(0).run();
        assertEquals(Material.PLAYER_HEAD, pending.get().getType());
        assertEquals(Material.PLAYER_HEAD, heads.player(uuid, "Missing").get().getType());
        assertEquals(1, work.size()); assertTrue(metas.isEmpty());
    }

    @Test void shutdownCompletesQueuedLookupsWithoutApplyingIncompleteProfiles() throws Exception {
        var pending = heads.player(UUID.randomUUID(), "Queued");
        heads.close();
        assertEquals(Material.PLAYER_HEAD, pending.get().getType());
        work.get(0).run(); assertTrue(profiles.isEmpty());
        verify(executor).shutdownNow();
    }
}
