package de.laurinhummel.skystats.managers;

import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PlayerStatisticsTest {
    @Test
    void capturesValuesOnceAndDoesNotRetainLivePlayerState() {
        Player player = mock(Player.class);
        PlayerProfile profile = mock(PlayerProfile.class);
        PlayerProfile copy = mock(PlayerProfile.class);
        when(player.getName()).thenReturn("Alice");
        when(player.getPlayerProfile()).thenReturn(profile);
        when(profile.clone()).thenReturn(copy);
        when(copy.clone()).thenReturn(mock(PlayerProfile.class));
        when(player.getStatistic(Statistic.MOB_KILLS)).thenReturn(10);
        PlayerStatistics snapshot = PlayerStatistics.capture(player);
        verify(player, never()).getStatistic(Statistic.MINE_BLOCK);
        when(player.getStatistic(Statistic.MOB_KILLS)).thenReturn(100);
        clearInvocations(player);
        assertEquals("Alice", snapshot.getName());
        assertEquals(10, snapshot.getStatistic(Statistic.MOB_KILLS));
        assertNotSame(profile, snapshot.getProfile());
        verifyNoInteractions(player);
    }
}
