/*
 * Baseline Test Suite for JavaMail4EWS Library Upgrade
 * 
 * This test suite validates current functionality before dependency upgrades
 * to ensure no regressions are introduced during the upgrade process.
 */
package org.sourceforge.net.javamail4ews;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.sourceforge.net.javamail4ews.store.EwsStoreTest;
import org.sourceforge.net.javamail4ews.store.EwsFolderTest;
import org.sourceforge.net.javamail4ews.store.EwsMessageTest;
import org.sourceforge.net.javamail4ews.transport.EwsTransportTest;
import org.sourceforge.net.javamail4ews.util.UtilTest;
import org.sourceforge.net.javamail4ews.util.EwsMailConverterTest;

@Suite
@SelectClasses({
    EwsStoreTest.class,
    EwsFolderTest.class,
    EwsMessageTest.class,
    EwsTransportTest.class,
    UtilTest.class,
    EwsMailConverterTest.class
})
public class BaselineTestSuite {
    // Test suite aggregator - no implementation needed
}