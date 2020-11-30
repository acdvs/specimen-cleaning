package com.specimencleaning;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class SpecimenCleaningPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(SpecimenCleaningPlugin.class);
		RuneLite.main(args);
	}
}