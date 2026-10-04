package com.hiddenobjectexaminer;

import com.google.inject.Provides;
import java.awt.Shape;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Point;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.WorldView;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.DecorativeObjectDespawned;
import net.runelite.api.events.DecorativeObjectSpawned;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.WallObjectDespawned;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.api.events.WorldViewLoaded;
import net.runelite.api.events.WorldViewUnloaded;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Hidden Object Examiner",
	description = "Highlights unnamed scenery and restores its Examine option",
	tags = {"examine", "objects", "scenery", "discovery", "exploration"}
)
public class HiddenObjectExaminerPlugin extends Plugin
{
	private final Set<TileObject> sceneObjects = Collections.newSetFromMap(new IdentityHashMap<>());

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private HiddenObjectExaminerConfig config;

	@Inject
	private HiddenObjectExaminerOverlay overlay;

	@Inject
	private OverlayManager overlayManager;

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		clientThread.invoke(this::rebuildSceneObjects);
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		sceneObjects.clear();
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (!config.restoreExamine() || client.getGameState() != GameState.LOGGED_IN || client.isMenuOpen())
		{
			return;
		}

		Point mouse = client.getMouseCanvasPosition();
		for (TileObject object : sceneObjects)
		{
			if (!isHiddenObject(object) || !containsMouse(object, mouse) || hasExamineEntry(object))
			{
				continue;
			}

			client.createMenuEntry(-1)
				.setOption("Examine")
				.setTarget(targetFor(object.getId()))
				.setIdentifier(object.getId())
				.setType(MenuAction.EXAMINE_OBJECT)
				.setParam0(object.getLocalLocation().getSceneX())
				.setParam1(object.getLocalLocation().getSceneY())
				.setWorldViewId(object.getWorldView().getId());
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			rebuildSceneObjects();
		}
		else if (event.getGameState() == GameState.LOADING || event.getGameState() == GameState.LOGIN_SCREEN)
		{
			sceneObjects.clear();
		}
	}

	@Subscribe
	public void onWorldViewLoaded(WorldViewLoaded event)
	{
		addWorldView(event.getWorldView());
	}

	@Subscribe
	public void onWorldViewUnloaded(WorldViewUnloaded event)
	{
		sceneObjects.removeIf(object -> object.getWorldView() == event.getWorldView());
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		add(event.getGameObject());
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		sceneObjects.remove(event.getGameObject());
	}

	@Subscribe
	public void onWallObjectSpawned(WallObjectSpawned event)
	{
		add(event.getWallObject());
	}

	@Subscribe
	public void onWallObjectDespawned(WallObjectDespawned event)
	{
		sceneObjects.remove(event.getWallObject());
	}

	@Subscribe
	public void onDecorativeObjectSpawned(DecorativeObjectSpawned event)
	{
		add(event.getDecorativeObject());
	}

	@Subscribe
	public void onDecorativeObjectDespawned(DecorativeObjectDespawned event)
	{
		sceneObjects.remove(event.getDecorativeObject());
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		add(event.getGroundObject());
	}

	@Subscribe
	public void onGroundObjectDespawned(GroundObjectDespawned event)
	{
		sceneObjects.remove(event.getGroundObject());
	}

	Set<TileObject> getSceneObjects()
	{
		return sceneObjects;
	}

	boolean isHiddenObject(TileObject object)
	{
		ObjectComposition composition = client.getObjectDefinition(object.getId());
		if (composition == null)
		{
			return false;
		}

		if (composition.getImpostorIds() != null)
		{
			composition = composition.getImpostor();
			if (composition == null)
			{
				return false;
			}
		}

		boolean anyShownOperation = false;
		for (int index = 0; index < 5; index++)
		{
			if (object.isOpShown(index))
			{
				anyShownOperation = true;
				break;
			}
		}

		return isHiddenDefinition(composition.getName(), composition.getActions(), anyShownOperation);
	}

	static boolean isHiddenDefinition(String name, String[] actions, boolean anyShownOperation)
	{
		if (name != null && !name.trim().isEmpty() && !"null".equalsIgnoreCase(name.trim()))
		{
			return false;
		}

		if (actions != null)
		{
			for (String action : actions)
			{
				if (action != null && !action.trim().isEmpty())
				{
					return false;
				}
			}
		}

		return !anyShownOperation;
	}

	static String targetFor(int objectId)
	{
		return "<col=00ffff>Unnamed object (" + objectId + ")</col>";
	}

	private boolean hasExamineEntry(TileObject object)
	{
		int sceneX = object.getLocalLocation().getSceneX();
		int sceneY = object.getLocalLocation().getSceneY();
		int worldViewId = object.getWorldView().getId();
		for (MenuEntry entry : client.getMenuEntries())
		{
			if (entry.getType() == MenuAction.EXAMINE_OBJECT
				&& entry.getIdentifier() == object.getId()
				&& entry.getParam0() == sceneX
				&& entry.getParam1() == sceneY
				&& entry.getWorldViewId() == worldViewId)
			{
				return true;
			}
		}
		return false;
	}

	private static boolean containsMouse(TileObject object, Point mouse)
	{
		Shape clickbox = object.getClickbox();
		if (clickbox == null)
		{
			clickbox = object.getCanvasTilePoly();
		}
		return clickbox != null && clickbox.contains(mouse.getX(), mouse.getY());
	}

	private void rebuildSceneObjects()
	{
		sceneObjects.clear();
		if (client.getGameState() == GameState.LOGGED_IN)
		{
			addWorldView(client.getTopLevelWorldView());
		}
	}

	private void addWorldView(WorldView worldView)
	{
		if (worldView == null || worldView.getScene() == null)
		{
			return;
		}

		for (Tile[][] plane : worldView.getScene().getTiles())
		{
			for (Tile[] row : plane)
			{
				for (Tile tile : row)
				{
					addTile(tile);
				}
			}
		}
	}

	private void addTile(Tile tile)
	{
		if (tile == null)
		{
			return;
		}

		add(tile.getWallObject());
		add(tile.getDecorativeObject());
		add(tile.getGroundObject());
		if (tile.getGameObjects() != null)
		{
			for (TileObject object : tile.getGameObjects())
			{
				add(object);
			}
		}
		addTile(tile.getBridge());
	}

	private void add(TileObject object)
	{
		if (object != null)
		{
			sceneObjects.add(object);
		}
	}

	@Provides
	HiddenObjectExaminerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(HiddenObjectExaminerConfig.class);
	}
}
