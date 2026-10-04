package com.hiddenobjectexaminer;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(HiddenObjectExaminerConfig.GROUP)
public interface HiddenObjectExaminerConfig extends Config
{
	String GROUP = "hiddenobjectexaminer";

	@ConfigItem(
		keyName = "showHighlights",
		name = "Highlight hidden objects",
		description = "Outline scenery whose Examine entry is absent from the normal right-click menu",
		position = 0
	)
	default boolean showHighlights()
	{
		return true;
	}

	@ConfigItem(
		keyName = "restoreExamine",
		name = "Restore Examine",
		description = "Add the game's normal Examine option while the pointer is over hidden scenery",
		position = 1
	)
	default boolean restoreExamine()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "highlightColor",
		name = "Highlight color",
		description = "Color used to outline hidden scenery",
		position = 2
	)
	default Color highlightColor()
	{
		return new Color(0, 255, 255, 55);
	}

	@Range(min = 1, max = 50)
	@ConfigItem(
		keyName = "highlightDistance",
		name = "Highlight distance",
		description = "Maximum number of tiles away to highlight hidden scenery",
		position = 3
	)
	default int highlightDistance()
	{
		return 24;
	}
}
