package com.hiddenobjectexaminer;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

final class HiddenObjectExaminerOverlay extends Overlay
{
	private final Client client;
	private final HiddenObjectExaminerPlugin plugin;
	private final HiddenObjectExaminerConfig config;
	private final Set<TileObject> nearbyObjects = Collections.newSetFromMap(new IdentityHashMap<>());

	@Inject
	private HiddenObjectExaminerOverlay(
		Client client,
		HiddenObjectExaminerPlugin plugin,
		HiddenObjectExaminerConfig config)
	{
		super(plugin);
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showHighlights() || client.getGameState() != GameState.LOGGED_IN)
		{
			return null;
		}

		Player player = client.getLocalPlayer();
		if (player == null)
		{
			return null;
		}

		Color color = config.highlightColor();
		Color border = new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(160, color.getAlpha()));
		Stroke previousStroke = graphics.getStroke();
		graphics.setStroke(new BasicStroke(2f));
		LocalPoint playerLocation = player.getLocalLocation();
		plugin.collectHiddenObjectsNear(nearbyObjects, player.getWorldView(), player.getWorldLocation().getPlane(),
			playerLocation.getSceneX(), playerLocation.getSceneY(), config.highlightDistance());
		for (TileObject object : nearbyObjects)
		{
			if (object.getWorldLocation().distanceTo2D(player.getWorldLocation()) > config.highlightDistance())
			{
				continue;
			}

			Shape shape = object.getClickbox();
			if (shape == null)
			{
				shape = object.getCanvasTilePoly();
			}
			if (shape != null)
			{
				graphics.setColor(color);
				graphics.fill(shape);
				graphics.setColor(border);
				graphics.draw(shape);
			}
		}
		graphics.setStroke(previousStroke);
		return null;
	}
}
