package com.hiddenobjectexaminer;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HiddenObjectClassifierTest
{
	@Test
	public void acceptsUnnamedActionlessObjects()
	{
		assertTrue(HiddenObjectExaminerPlugin.isHiddenDefinition("null", new String[5], false));
		assertTrue(HiddenObjectExaminerPlugin.isHiddenDefinition(null, null, false));
	}

	@Test
	public void rejectsNamedOrInteractiveObjects()
	{
		assertFalse(HiddenObjectExaminerPlugin.isHiddenDefinition("Door", new String[5], false));
		assertFalse(HiddenObjectExaminerPlugin.isHiddenDefinition("null", new String[]{"Open"}, false));
		assertFalse(HiddenObjectExaminerPlugin.isHiddenDefinition("null", new String[5], true));
	}

	@Test
	public void includesObjectIdInMenuTarget()
	{
		assertTrue(HiddenObjectExaminerPlugin.targetFor(688).contains("688"));
	}
}
