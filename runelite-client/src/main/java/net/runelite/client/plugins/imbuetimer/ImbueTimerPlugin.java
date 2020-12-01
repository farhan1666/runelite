/*
 * Copyright (c) 2017, Seth <Sethtroll3@gmail.com>
 * Copyright (c) 2018, Jordan Atwood <jordan.atwood423@gmail.com>
 * Copyright (c) 2019, winterdaze
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
package net.runelite.client.plugins.imbuetimer;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

import javax.inject.Inject;
import java.awt.*;

@PluginDescriptor(
	name = "Magic Imbue Accurate Timer",
	description = "Show various timers in an infobox",
	tags = {"combat", "items", "magic", "potions", "prayer", "overlay", "abyssal", "sire", "inferno", "fight", "caves", "cape", "timer", "tzhaar"}
)
@Slf4j
public class ImbueTimerPlugin extends Plugin
{
	private static final String MAGIC_IMBUE_EXPIRED_MESSAGE = "Your Magic Imbue charge has ended.";
	private static final String MAGIC_IMBUE_MESSAGE = "You are charged to combine runes!";
	private static final String MAGIC_IMBUE_WARNING = "Your Magic Imbue spell charge is running out...";
	private static final int MAGIC_IMBUE_DURATION = 20;
	private static final int MAGIC_IMBUE_WARNING_DURATION = 10;

	@Inject
	private SpriteManager spriteManager;

	@Inject
	private Client client;

	@Inject
	private TimersConfig config;

	@Inject
	private InfoBoxManager infoBoxManager;

	private TickCounter counter;

	boolean isFirstMessage = false;

	@Provides
    TimersConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TimersConfig.class);
	}

	@Override
	protected void shutDown() throws Exception
	{
		infoBoxManager.removeIf(t -> t instanceof TickCounter);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!config.showAccurateMagicImbue()) {
			removeTickCounter();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.SPAM && event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		if (config.showAccurateMagicImbue() && event.getMessage().equals(MAGIC_IMBUE_MESSAGE))
		{
			createTickCounter(MAGIC_IMBUE_DURATION);
			isFirstMessage = true;
		}

		if (config.showAccurateMagicImbue() && event.getMessage().equals(MAGIC_IMBUE_WARNING))
		{
			if (isFirstMessage)
			{
				if (counter == null)
					createTickCounter(MAGIC_IMBUE_WARNING_DURATION);
				else
					counter.setCount(MAGIC_IMBUE_WARNING_DURATION);
				isFirstMessage = false;
			}
		}

		if (event.getMessage().equals(MAGIC_IMBUE_EXPIRED_MESSAGE))
		{
			removeTickCounter();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (counter == null)
		{
			return;
		}

		if (counter.getCount() > -1) {
			if (counter.getCount() == 0)
			{
				counter.setTextColor(Color.RED);
			}
			counter.setCount(counter.getCount() - 1);
		}
		else
		{
			removeTickCounter();
		}
	}

	private void createTickCounter(int duration)
	{
		if (counter == null)
		{
			counter = new TickCounter(null, this, duration);
			spriteManager.getSpriteAsync(SpriteID.SPELL_MAGIC_IMBUE, 0, counter);
			counter.setTooltip("Magic imbue");
			infoBoxManager.addInfoBox(counter);
		}
		else
		{
			counter.setCount(duration);
			counter.setTextColor(Color.WHITE);
		}
	}

	private void removeTickCounter()
	{
		if (counter == null)
		{
			return;
		}

		infoBoxManager.removeInfoBox(counter);
		counter = null;
	}

}
