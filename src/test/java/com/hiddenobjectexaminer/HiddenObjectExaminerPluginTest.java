package com.hiddenobjectexaminer;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class HiddenObjectExaminerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(HiddenObjectExaminerPlugin.class);
		RuneLite.main(args);
	}
}
