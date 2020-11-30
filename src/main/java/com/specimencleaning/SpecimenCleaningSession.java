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

import java.awt.Color;
import java.time.Instant;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.ItemID;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;

@Slf4j
@Singleton
public class SpecimenCleaningSession
{
	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private SpecimenCleaningConfig config;

	@Getter
	private int uncleanedFindCount;

	@Getter
	private int artefactCount;

	@Getter
	private int antiqueLampCount;

	@Getter
	@Setter
	private Instant lastActionTime = null;

	void incrementItemObtained(int itemId)
	{
		switch (itemId)
		{
			case ItemID.UNCLEANED_FIND:
				uncleanedFindCount++;
				break;
			case ItemID.ARROWHEADS:
			case ItemID.JEWELLERY:
			case ItemID.POTTERY:
			case ItemID.OLD_CHIPPED_VASE:
				artefactCount++;
				break;
			case ItemID.ANTIQUE_LAMP_11185:
			case ItemID.ANTIQUE_LAMP_11186:
			case ItemID.ANTIQUE_LAMP_11187:
			case ItemID.ANTIQUE_LAMP_11188:
			case ItemID.ANTIQUE_LAMP_11189:
				antiqueLampCount++;

				if (config.showNotifs())
				{
					final String formattedMessage = new ChatMessageBuilder()
						.append(Color.RED, "You found an antique lamp!")
						.build();

					chatMessageManager.queue(QueuedMessage.builder()
						.type(ChatMessageType.GAMEMESSAGE)
						.runeLiteFormattedMessage(formattedMessage)
						.build());
				}

				break;
		}
	}

	void resetTracker()
	{
		setLastActionTime(null);

		uncleanedFindCount = 0;
		artefactCount = 0;

		if (!config.neverResetLamps())
		{
			antiqueLampCount = 0;
		}

		if (config.showNotifs())
		{
			final String formattedMessage = new ChatMessageBuilder()
				.append("Specimen cleaning tracker reset.")
				.build();

			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.GAMEMESSAGE)
				.runeLiteFormattedMessage(formattedMessage)
				.build());
		}
	}
}
