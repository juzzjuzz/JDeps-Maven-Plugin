package org.codefx.maven.plugin.jdeps.rules;

import org.codefx.maven.plugin.jdeps.rules.TypeNameHierarchyMapDependencyJudge.TypeNameMapDependencyJudgeBuilder;

/**
 * Tests for {@link TypeNameHierarchyMapDependencyJudge}.
 */
public class TypeNameHierarchyMapDependencyJudgeTest extends AbstractDependencyJudgeTest {

	@Override
	protected DependencyJudgeBuilder builder() {
		return new TypeNameMapDependencyJudgeBuilder();
	}
}
