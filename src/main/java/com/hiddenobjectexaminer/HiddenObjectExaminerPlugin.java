package com.hiddenobjectexaminer;

import com.google.inject.Provides;
import java.awt.Shape;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.GameObject;
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
	private final Set<TileObject> confirmedHiddenObjects = Collections.newSetFromMap(new IdentityHashMap<>());
	private final Set<TileObject> confirmedNativeObjects = Collections.newSetFromMap(new IdentityHashMap<>());

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
		confirmedHiddenObjects.clear();
		confirmedNativeObjects.clear();
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
			if (!containsMouse(object, mouse))
			{
				continue;
			}

			ObjectComposition composition = getActiveComposition(object);
			if (composition == null)
			{
				continue;
			}

			int examineId = composition.getId();
			Point menuPoint = getMenuPoint(object);
			if (hasExamineEntry(object, examineId, menuPoint))
			{
				confirmedNativeObjects.add(object);
				confirmedHiddenObjects.remove(object);
				continue;
			}

			confirmedHiddenObjects.add(object);
			confirmedNativeObjects.remove(object);

			client.createMenuEntry(-1)
				.setOption("Examine")
				.setTarget(targetFor(composition.getName(), examineId))
				.setIdentifier(examineId)
				.setType(MenuAction.EXAMINE_OBJECT)
				.setParam0(menuPoint.getX())
				.setParam1(menuPoint.getY())
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
			confirmedHiddenObjects.clear();
			confirmedNativeObjects.clear();
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
		confirmedHiddenObjects.removeIf(object -> object.getWorldView() == event.getWorldView());
		confirmedNativeObjects.removeIf(object -> object.getWorldView() == event.getWorldView());
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		add(event.getGameObject());
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		remove(event.getGameObject());
	}

	@Subscribe
	public void onWallObjectSpawned(WallObjectSpawned event)
	{
		add(event.getWallObject());
	}

	@Subscribe
	public void onWallObjectDespawned(WallObjectDespawned event)
	{
		remove(event.getWallObject());
	}

	@Subscribe
	public void onDecorativeObjectSpawned(DecorativeObjectSpawned event)
	{
		add(event.getDecorativeObject());
	}

	@Subscribe
	public void onDecorativeObjectDespawned(DecorativeObjectDespawned event)
	{
		remove(event.getDecorativeObject());
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		add(event.getGroundObject());
	}

	@Subscribe
	public void onGroundObjectDespawned(GroundObjectDespawned event)
	{
		remove(event.getGroundObject());
	}

	Set<TileObject> getSceneObjects()
	{
		return sceneObjects;
	}

	boolean isHiddenObject(TileObject object)
	{
		if (confirmedNativeObjects.contains(object))
		{
			return false;
		}
		if (confirmedHiddenObjects.contains(object))
		{
			return true;
		}

		ObjectComposition composition = getActiveComposition(object);
		if (composition == null)
		{
			return false;
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

	static String targetFor(String name, int objectId)
	{
		if (name == null || name.trim().isEmpty() || "null".equalsIgnoreCase(name.trim()))
		{
			return targetFor(objectId);
		}
		return "<col=00ffff>" + name + " (" + objectId + ")</col>";
	}

	private ObjectComposition getActiveComposition(TileObject object)
	{
		ObjectComposition composition = client.getObjectDefinition(object.getId());
		if (composition != null && composition.getImpostorIds() != null)
		{
			composition = composition.getImpostor();
		}
		return composition;
	}

	private boolean hasExamineEntry(TileObject object, int examineId, Point menuPoint)
	{
		int worldViewId = object.getWorldView().getId();
		for (MenuEntry entry : client.getMenuEntries())
		{
			if (entry.getType() == MenuAction.EXAMINE_OBJECT
				&& (entry.getIdentifier() == object.getId() || entry.getIdentifier() == examineId)
				&& entry.getParam0() == menuPoint.getX()
				&& entry.getParam1() == menuPoint.getY()
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

	private static Point getMenuPoint(TileObject object)
	{
		if (object instanceof GameObject)
		{
			Point sceneMin = ((GameObject) object).getSceneMinLocation();
			if (sceneMin != null)
			{
				return sceneMin;
			}
		}
		return new Point(object.getLocalLocation().getSceneX(), object.getLocalLocation().getSceneY());
	}

	private void rebuildSceneObjects()
	{
		sceneObjects.clear();
		confirmedHiddenObjects.clear();
		confirmedNativeObjects.clear();
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

	private void remove(TileObject object)
	{
		sceneObjects.remove(object);
		confirmedHiddenObjects.remove(object);
		confirmedNativeObjects.remove(object);
	}

	@Provides
	HiddenObjectExaminerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(HiddenObjectExaminerConfig.class);
	}
}
