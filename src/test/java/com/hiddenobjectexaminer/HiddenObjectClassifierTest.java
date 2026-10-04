package com.hiddenobjectexaminer;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HiddenObjectClassifierTest
{
	@Test
	public void acceptsMenuSuppressedSceneTags()
	{
		assertTrue(HiddenObjectExaminerPlugin.isMenuSuppressed(1L << 19));
		assertTrue(HiddenObjectExaminerPlugin.isMenuSuppressed((12345L << 20) | (1L << 19)));
	}

	@Test
	public void rejectsNormallyPickableSceneTags()
	{
		assertFalse(HiddenObjectExaminerPlugin.isMenuSuppressed(0));
		assertFalse(HiddenObjectExaminerPlugin.isMenuSuppressed(12345L << 20));
	}

	@Test
	public void includesObjectIdInMenuTarget()
	{
		assertTrue(HiddenObjectExaminerPlugin.targetFor(688).contains("688"));
		assertTrue(HiddenObjectExaminerPlugin.targetFor("Shield display", 12345).contains("Shield display (12345)"));
	}
}
