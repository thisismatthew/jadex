package jadex.platform.service;

import org.junit.Ignore;

import jadex.base.test.ComponentTestSuite;
import junit.framework.Test;

/**
 *  Run all component tests in test folder.
 */
// Doesn't make sense in platform project (who put this here???)
@Ignore
public class ManagementExtensionTest extends ComponentTestSuite
{
	/**
	 *  Constructor called by Maven JUnit runner.
	 */
	public ManagementExtensionTest()	throws Exception
	{
		super("jadex-platform-extension-management",
			// Exclude failing tests to allow maven build.
			new String[]
			{
				"Persistable",	// persistence currently disabled
				"BPMNRecoveryTest",	// persistence currently disabled
				"HelloUser",	// cannot be started without hello service
				"ComponentRegistry",	// cannot be started without some args (rspublish, context service)
				"DaemonResponder",	// cannot be started without receiver parameter
				"ManualUser",	// extends user test to allow manual testing with gui.
				"TestSubprocessStartEvent",	// part of test and sometimes produces exception when started alone.
				"TestIntermediateEvent",	// part of test and sometimes produces exception when started alone.

				"CliEmail", "BPMNRecoveryTest", "globalservicepool/Initiator", "globalservicepool\\Initiator", "JavaWrapperTest", "IntermediateTest" // all are failing in stable branch (11.09.2017)
			}, true);
	}
	
//	/**
//	 *  Constructor called by JadexInstrumentor for Android tests.
//	 */
//	public ManagementExtensionTest(File cproot)	throws Exception
//	{
//		super(cproot, cproot,
//			// Exclude failing tests to allow maven build.
//			new String[]
//		{
//			"ManualUser",	// extends user to allow manual testing with gui.
//			"TestSubprocessStartEvent",	// part of test and sometimes produces exception when started alone.
//			"TestIntermediateEvent"	// part of test and sometimes produces exception when started alone.
//		});
//	}
	
	/**
	 *  Static method called by eclipse JUnit runner.
	 */
	public static Test suite() throws Exception
	{
		return new ManagementExtensionTest();
	}
}
