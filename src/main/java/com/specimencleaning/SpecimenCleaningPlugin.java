/*
 * Copyright (c) 2020 Adam Davies (https://github.com/acdvs)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.specimencleaning;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.inject.Provides;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Specimen Cleaning",
	description = "Show Varrock Museum specimen cleaning stats",
	tags = {"overlay", "minigame", "Varrock", "museum"}
)
@Slf4j
public class SpecimenCleaningPlugin extends Plugin
{
	private static final Set<Integer> REGIONS = ImmutableSet.of(12853, 13109);
	private static final Set<Integer> TILES_X = IntStream.rangeClosed(3253, 3267).boxed().collect(Collectors.toSet());
	private static final Set<Integer> TILES_Y = IntStream.rangeClosed(3442, 3446).boxed().collect(Collectors.toSet());

	private static final Set<Integer> ITEMS = ImmutableSet.of(
		ItemID.UNCLEANED_FIND, ItemID.ARROWHEADS, ItemID.JEWELLERY,
		ItemID.POTTERY, ItemID.OLD_CHIPPED_VASE, ItemID.ANTIQUE_LAMP_11185,
		ItemID.ANTIQUE_LAMP_11186, ItemID.ANTIQUE_LAMP_11187,
		ItemID.ANTIQUE_LAMP_11188, ItemID.ANTIQUE_LAMP_11189);

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private SpecimenCleaningOverlay overlay;

	@Inject
	private SpecimenCleaningConfig config;

	@Inject
	private SpecimenCleaningSession session;
	private Multiset<Integer> lastInventory;

	@Provides
	SpecimenCleaningConfig provideConfig(ConfigManager configManager) { return configManager.getConfig(SpecimenCleaningConfig.class); }

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		ItemContainer container = event.getItemContainer();

		if (!isPlayerInArea() || container != client.getItemContainer(InventoryID.INVENTORY))
		{
			return;
		}

		Multiset<Integer> currentInventory = HashMultiset.create();
		Arrays.stream(container.getItems())
			.filter(item -> ITEMS.contains(item.getId()))
			.forEach(item -> currentInventory.add(item.getId(), item.getQuantity()));

		if (lastInventory != null)
		{
			Multiset<Integer> delta = Multisets.difference(currentInventory, lastInventory);
			delta.forEach(itemId -> session.incrementItemObtained(itemId));

			if (delta.size() > 0) {
				session.setLastActionTime(Instant.now());
			}
		}

		lastInventory = currentInventory;
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (config.timeout() > 0 && session.getLastActionTime() != null)
		{
			final Duration timeoutMinutes = Duration.ofMinutes(config.timeout());
			final Duration sinceAction = Duration.between(session.getLastActionTime(), Instant.now());

			if (sinceAction.compareTo(timeoutMinutes) >= 0)
			{
				session.resetTracker();
			}
		}
	}

	boolean isPlayerInArea()
	{
		GameState gameState = client.getGameState();

		if (gameState != GameState.LOGGED_IN
			&& gameState != GameState.LOADING)
		{
			return false;
		}

		WorldPoint location = client.getLocalPlayer().getWorldLocation();
		final int currRegion = location.getRegionID();
		final int currX = location.getX();
		final int currY = location.getY();

		return REGIONS.contains(currRegion)
			&& TILES_X.contains(currX)
			&& TILES_Y.contains(currY);
	}
}
