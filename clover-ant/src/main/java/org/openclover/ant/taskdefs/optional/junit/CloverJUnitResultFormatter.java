package org.openclover.ant.taskdefs.optional.junit;

import org.apache.tools.ant.taskdefs.optional.junit.JUnitVersionHelper;
import org.apache.tools.ant.taskdefs.optional.junit.PlainJUnitResultFormatter;

import com_atlassian_clover.TestNameSniffer;

import com.atlassian.clover.Logger;
import com.atlassian.clover.recorder.TestNameSnifferHelper;

import junit.framework.Test;

/**
 * <p>
 * Motivation: Ant-Task &lt;junit&gt;<br>
 * ... <br>
 * &lt;formatter 
 * classname="org.openclover.ant.taskdefs.optional.junit.CloverJUnitResultFormatter"/&gt;
 * <br>
 * &lt;/junit&gt;<br>
 * into the ant build file &quot;build.xml&quot; of the project
 * </p>
 * <p>
 * Take a look into javadoc
 * <a href="https://ant.apache.org/manual/Tasks/junit.html#formatter">Ant-Task
 * Junit <i>formatter</i></a><br>
 * <br>
 * Note: attribute <i>classname</i> - name of a custom formatter class<br>
 * Hint: it can be uses more the one XML-nodes "formatter" in one task.
 * </p>
 */
public class CloverJUnitResultFormatter extends PlainJUnitResultFormatter {

	/**
	 * It handles the call of {@link TestNameSniffer} before and after the testcase run.
	 *
	 * @param test the junit test case
	 * @param start is <code>true</code>  otherwise <code>false</code>
	 */
	private final void callCloverTestNameSniffer(final Test test, final boolean start) {

		final String testClassName = JUnitVersionHelper.getTestCaseClassName(test);

		try {
			Class<?> testClass = Class.forName(testClassName);
			TestNameSniffer testNameSniffer = TestNameSnifferHelper.lookupTestSnifferField(testClass);

			if (null != testNameSniffer) {
				if (start) {
					testNameSniffer.setTestName(JUnitVersionHelper.getTestCaseName(test));
				} else {
					testNameSniffer.clearTestName();
				}
			}
		} catch (ClassNotFoundException ex) {
			Logger.getInstance()
					.debug("[ERROR][clover-ant](taskdefs.optional.junit) JUnitResultFormatter on " + testClassName, ex);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void endTest(Test test) {
		callCloverTestNameSniffer(test, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void startTest(Test test) {
		callCloverTestNameSniffer(test, true);
	}
}
