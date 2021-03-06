package org.codefx.mvn.jdeps.rules;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.lang.String.format;

/**
 * Tests {@link DependencyRule}.
 */
public class DependenyRuleTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void checkName_nameNull_throwsException() throws Exception {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("The rule (null -> sun.misc.Unsafe: FAIL) defines no dependent.");

		letRuleCheckName(null);
	}

	@Test
	public void checkName_nameEmpty_throwsException() throws Exception {
		String faultyName = "";

		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage(format("The rule (%s -> sun.misc.Unsafe: FAIL) defines no dependent.", faultyName));

		letRuleCheckName(faultyName);
	}

	@Test
	public void checkName_firstPartEmpty_throwsException() throws Exception {
		String faultyName = ".foo.bar";

		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage(format(
				"In the rule (%s -> sun.misc.Unsafe: FAIL) the name '%s' contains one empty part.",
				faultyName, faultyName));

		letRuleCheckName(faultyName);
	}

	@Test
	public void checkName_middlePartEmpty_throwsException() throws Exception {
		String faultyName = "com..bar";

		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage(format(
				"In the rule (%s -> sun.misc.Unsafe: FAIL) the name '%s' contains one empty part.",
				faultyName, faultyName));

		letRuleCheckName(faultyName);
	}

	@Test
	public void checkName_lastPartEmpty_throwsException() throws Exception {
		String faultyName = "com.foo.";

		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage(format(
				"In the rule (%s -> sun.misc.Unsafe: FAIL) the name '%s' contains one empty part.",
				faultyName, faultyName));

		letRuleCheckName(faultyName);
	}

	@Test
	public void checkName_illegalJavaIdentifierStart_throwsException() throws Exception {
		String faultyName = "1com.foo.bar";

		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage(format(
				"In the rule (%s -> sun.misc.Unsafe: FAIL) "
						+ "a part of the name '%s' starts with the invalid character '1'.",
				faultyName, faultyName));

		letRuleCheckName(faultyName);
	}

	@Test
	public void checkName_illegalJavaIdentifierPart_throwsException() throws Exception {
		String faultyName = "com.fo-o.bar";

		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage(format(
				"In the rule (%s -> sun.misc.Unsafe: FAIL) the name '%s' contains the invalid character '-'.",
				faultyName, faultyName));

		letRuleCheckName(faultyName);
	}

	@Test
	public void checkName_nameIsWildcard_throwsNoException() throws Exception {
		letRuleCheckName(DependencyRule.ALL_TYPES_WILDCARD);
	}

	private static void letRuleCheckName(String faultyName) throws IllegalArgumentException {
		DependencyRule.checkName(faultyName, format("(%s -> sun.misc.Unsafe: FAIL)", faultyName), "dependent");
	}

}
