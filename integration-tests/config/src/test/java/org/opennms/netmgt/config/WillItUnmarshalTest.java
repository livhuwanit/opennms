package org.opennms.netmgt.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.opennms.core.test.ConfigurationTestUtils.getDaemonEtcDirectory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import junit.framework.AssertionFailedError;

import org.apache.commons.io.FileUtils;
import org.exolab.castor.util.LocalConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.opennms.core.test.ConfigurationTestUtils;
import org.opennms.core.xml.CastorUtils;
import org.opennms.core.xml.JaxbUtils;
import org.opennms.features.reporting.model.basicreport.LegacyLocalReportsDefinition;
import org.opennms.features.reporting.model.jasperreport.LocalJasperReports;
import org.opennms.features.reporting.model.remoterepository.RemoteRepositoryConfig;
import org.opennms.netmgt.alarmd.northbounder.syslog.SyslogNorthbounderConfig;
import org.opennms.netmgt.config.accesspointmonitor.AccessPointMonitorConfig;
import org.opennms.netmgt.config.ackd.AckdConfiguration;
import org.opennms.netmgt.config.actiond.ActiondConfiguration;
import org.opennms.netmgt.config.ami.AmiConfig;
import org.opennms.netmgt.config.archiver.events.EventsArchiverConfiguration;
import org.opennms.netmgt.config.capsd.CapsdConfiguration;
import org.opennms.netmgt.config.categories.Catinfo;
import org.opennms.netmgt.config.charts.ChartConfiguration;
import org.opennms.netmgt.config.collectd.CollectdConfiguration;
import org.opennms.netmgt.config.collectd.jmx.JmxDatacollectionConfig;
import org.opennms.netmgt.config.collectd.jmx.Mbeans;
import org.opennms.netmgt.config.datacollection.DatacollectionConfig;
import org.opennms.netmgt.config.datacollection.DatacollectionGroup;
import org.opennms.netmgt.config.destinationPaths.DestinationPaths;
import org.opennms.netmgt.config.discovery.DiscoveryConfiguration;
import org.opennms.netmgt.config.enlinkd.EnlinkdConfiguration;
import org.opennms.netmgt.config.eventd.EventdConfiguration;
import org.opennms.netmgt.config.filter.DatabaseSchema;
import org.opennms.netmgt.config.groups.Groupinfo;
import org.opennms.netmgt.config.httpdatacollection.HttpDatacollectionConfig;
import org.opennms.netmgt.config.javamail.JavamailConfiguration;
import org.opennms.netmgt.config.jdbc.JdbcDataCollectionConfig;
import org.opennms.netmgt.config.kscReports.ReportsList;
import org.opennms.netmgt.config.linkd.LinkdConfiguration;
import org.opennms.netmgt.config.mailtransporttest.MailTransportTest;
import org.opennms.netmgt.config.map.adapter.MapsAdapterConfiguration;
import org.opennms.netmgt.config.microblog.MicroblogConfiguration;
import org.opennms.netmgt.config.monitoringLocations.MonitoringLocationsConfiguration;
import org.opennms.netmgt.config.notifd.NotifdConfiguration;
import org.opennms.netmgt.config.notificationCommands.NotificationCommands;
import org.opennms.netmgt.config.notifications.Notifications;
import org.opennms.netmgt.config.opennmsDataSources.DataSourceConfiguration;
import org.opennms.netmgt.config.poller.PollerConfiguration;
import org.opennms.netmgt.config.poller.outages.Outages;
import org.opennms.netmgt.config.provisiond.ProvisiondConfiguration;
import org.opennms.netmgt.config.rancid.adapter.RancidConfiguration;
import org.opennms.netmgt.config.reportd.ReportdConfiguration;
import org.opennms.netmgt.config.reporting.opennms.OpennmsReports;
import org.opennms.netmgt.config.rtc.RTCConfiguration;
import org.opennms.netmgt.config.rws.RwsConfiguration;
import org.opennms.netmgt.config.scriptd.ScriptdConfiguration;
import org.opennms.netmgt.config.server.LocalServer;
import org.opennms.netmgt.config.service.ServiceConfiguration;
import org.opennms.netmgt.config.siteStatusViews.SiteStatusViewConfiguration;
import org.opennms.netmgt.config.snmp.SnmpConfig;
import org.opennms.netmgt.config.snmpAsset.adapter.SnmpAssetAdapterConfiguration;
import org.opennms.netmgt.config.snmpinterfacepoller.SnmpInterfacePollerConfiguration;
import org.opennms.netmgt.config.statsd.StatisticsDaemonConfiguration;
import org.opennms.netmgt.config.surveillanceViews.SurveillanceViewConfiguration;
import org.opennms.netmgt.config.syslogd.SyslogdConfiguration;
import org.opennms.netmgt.config.threshd.ThreshdConfiguration;
import org.opennms.netmgt.config.threshd.ThresholdingConfig;
import org.opennms.netmgt.config.tl1d.Tl1dConfiguration;
import org.opennms.netmgt.config.translator.EventTranslatorConfiguration;
import org.opennms.netmgt.config.trapd.TrapdConfiguration;
import org.opennms.netmgt.config.users.Userinfo;
import org.opennms.netmgt.config.vacuumd.VacuumdConfiguration;
import org.opennms.netmgt.config.viewsdisplay.Viewinfo;
import org.opennms.netmgt.config.vmware.VmwareConfig;
import org.opennms.netmgt.config.vmware.cim.VmwareCimDatacollectionConfig;
import org.opennms.netmgt.config.vmware.vijava.VmwareDatacollectionConfig;
import org.opennms.netmgt.config.wmi.WmiConfig;
import org.opennms.netmgt.config.wmi.WmiDatacollectionConfig;
import org.opennms.netmgt.config.xmlrpcd.XmlrpcdConfiguration;
import org.opennms.netmgt.provision.persist.requisition.Requisition;
import org.opennms.netmgt.xml.eventconf.Events;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * This is an integration test checking if all provided example XML files can be
 * unmarshalled.
 * 
 * For each file to test, an entry in the {@link #files()} list must exist.
 * During test run, all tests methods are executed for each test.
 * 
 * To ensure, that all provided files are covered, a meta test is used
 * ({@link WillItUnmarshalMetaTest}).
 * 
 * The name of this class is a tribute to
 * <a href="http://www.willitblend.com/">www.willitblend.com</a>.
 * 
 * @author Dustin Frisch<fooker@lab.sh>
 * 
 * @see WillItUnmarshalMetaTest
 */
@RunWith(value = Parameterized.class)
public class WillItUnmarshalTest {
    private static final String CASTOR_LENIENT_SEQUENCE_ORDERING_PROPERTY = "org.exolab.castor.xml.lenient.sequence.order";

    /**
     * Possible implementations for resource loading.
     */
    public static enum Source {
        CONFIG,
        EXAMPLE,
        SPRING,
        ABSOLUTE,
    }

    /**
     * Possible implementation used for unmarshalling.
     */
    public static enum Impl {
        JAXB,
        CASTOR
    }

    /**
     * A list of test parameters to execute.
     * 
     * See {@link #files()} for detailed information.
     */
    public static final ArrayList<Object[]> FILES = new ArrayList<Object[]>();

    private static void addFile(final Source source, final String file, final Class<?> clazz, final Impl impl, final boolean lenient, final String exceptionMessage) {
        FILES.add(new Object[] {source, file, clazz, impl, lenient, exceptionMessage});
    }

    private static void addFile(final Source source, final String file, final Class<?> clazz, final Impl impl, final String exceptionMessage) {
        addFile(source, file, clazz, impl, false, exceptionMessage);
    }

    private static void addFile(final Source source, final String file, final Class<?> clazz, final Impl impl, final boolean lenient) {
        addFile(source, file, clazz, impl, lenient, null);
    }

    private static void addFile(final Source source, final String file, final Class<?> clazz, final Impl impl) {
        addFile(source, file, clazz, impl, false, null);
    }
    
    static {
        addFile(Source.SPRING, "eventconf-good-ordering.xml", Events.class, Impl.JAXB);
        addFile(Source.SPRING, "eventconf-bad-ordering.xml", Events.class, Impl.JAXB, true);

        addFile(Source.SPRING, "eventconf-bad-element.xml", Events.class, Impl.JAXB, "Invalid content was found starting with element 'bad-element'.");

        addFile(Source.CONFIG, "access-point-monitor-configuration.xml", AccessPointMonitorConfig.class, Impl.CASTOR);
        addFile(Source.CONFIG, "actiond-configuration.xml", ActiondConfiguration.class, Impl.JAXB);
        addFile(Source.CONFIG, "ami-config.xml", AmiConfig.class, Impl.JAXB);
        addFile(Source.CONFIG, "availability-reports.xml", OpennmsReports.class, Impl.CASTOR);
        addFile(Source.CONFIG, "capsd-configuration.xml", CapsdConfiguration.class, Impl.CASTOR);
        addFile(Source.EXAMPLE, "capsd-configuration.xml", CapsdConfiguration.class, Impl.CASTOR);
        addFile(Source.CONFIG, "categories.xml", Catinfo.class, Impl.CASTOR);
        addFile(Source.CONFIG, "chart-configuration.xml", ChartConfiguration.class, Impl.CASTOR);
        addFile(Source.CONFIG, "collectd-configuration.xml", CollectdConfiguration.class, Impl.JAXB);
        addFile(Source.EXAMPLE, "collectd-configuration.xml", CollectdConfiguration.class, Impl.JAXB);
        addFile(Source.CONFIG, "database-reports.xml", LegacyLocalReportsDefinition.class, Impl.JAXB);
        addFile(Source.CONFIG, "database-schema.xml", DatabaseSchema.class, Impl.CASTOR);
        addFile(Source.CONFIG, "datacollection-config.xml", DatacollectionConfig.class, Impl.JAXB);
        addFile(Source.EXAMPLE, "old-datacollection-config.xml", DatacollectionConfig.class, Impl.JAXB);
        addFile(Source.CONFIG, "destinationPaths.xml", DestinationPaths.class, Impl.CASTOR);
        addFile(Source.EXAMPLE, "destinationPaths.xml", DestinationPaths.class, Impl.CASTOR);
        addFile(Source.CONFIG, "discovery-configuration.xml", DiscoveryConfiguration.class, Impl.CASTOR);
        addFile(Source.EXAMPLE, "discovery-configuration.xml", DiscoveryConfiguration.class, Impl.CASTOR);
        addFile(Source.CONFIG, "eventconf.xml", Events.class, Impl.CASTOR);
        addFile(Source.CONFIG, "events-archiver-configuration.xml", EventsArchiverConfiguration.class, Impl.CASTOR);
        addFile(Source.CONFIG, "groups.xml", Groupinfo.class, Impl.CASTOR);
        addFile(Source.EXAMPLE, "groups.xml", Groupinfo.class, Impl.CASTOR);
        addFile(Source.CONFIG, "http-datacollection-config.xml", HttpDatacollectionConfig.class, Impl.CASTOR);
        addFile(Source.EXAMPLE, "devices/motorola_cpei_150_wimax_gateway/http-datacollection-config.xml", HttpDatacollectionConfig.class, Impl.CASTOR);
        addFile(Source.CONFIG, "jasper-reports.xml", LocalJasperReports.class, Impl.JAXB);
        addFile(Source.CONFIG, "jmx-datacollection-config.xml", JmxDatacollectionConfig.class, Impl.CASTOR);
        addFile(Source.CONFIG, "ksc-performance-reports.xml", ReportsList.class, Impl.CASTOR);
        addFile(Source.CONFIG, "linkd-configuration.xml", LinkdConfiguration.class, Impl.CASTOR);
        addFile(Source.CONFIG, "enlinkd-configuration.xml", EnlinkdConfiguration.class, Impl.CASTOR);
        addFile(Source.EXAMPLE, "linkd-configuration.xml", LinkdConfiguration.class, Impl.CASTOR);
        addFile(Source.EXAMPLE, "mail-transport-test.xml", MailTransportTest.class, Impl.JAXB);
        addFile(Source.EXAMPLE, "hyperic-integration/imports-HQ.xml", Requisition.class, Impl.JAXB);
        addFile(Source.EXAMPLE, "hyperic-integration/imports-opennms-admin.xml", Requisition.class, Impl.JAXB);
        addFile(Source.CONFIG, "monitoring-locations.xml", MonitoringLocationsConfiguration.class, Impl.JAXB);
        addFile(Source.EXAMPLE, "monitoring-locations.xml", MonitoringLocationsConfiguration.class, Impl.JAXB);
        addFile(Source.CONFIG, "notifd-configuration.xml", NotifdConfiguration.class, Impl.CASTOR);
        addFile(Source.CONFIG, "notificationCommands.xml", NotificationCommands.class, Impl.CASTOR);
        addFile(Source.EXAMPLE, "notificationCommands.xml", NotificationCommands.class, Impl.CASTOR);
        addFile(Source.CONFIG, "notifications.xml", Notifications.class, Impl.CASTOR);
        addFile(Source.EXAMPLE, "notifications.xml", Notifications.class, Impl.CASTOR);
        addFile(Source.CONFIG, "opennms-datasources.xml", DataSourceConfiguration.class, Impl.CASTOR);
        addFile(Source.CONFIG, "opennms-server.xml", LocalServer.class, Impl.JAXB);
        addFile(Source.EXAMPLE, "opennms-server.xml", LocalServer.class, Impl.JAXB);
        addFile(Source.CONFIG, "poll-outages.xml", Outages.class, Impl.JAXB);
        addFile(Source.EXAMPLE, "poll-outages.xml", Outages.class, Impl.JAXB);
        addFile(Source.CONFIG, "poller-configuration.xml", PollerConfiguration.class, Impl.JAXB);
        addFile(Source.EXAMPLE, "poller-configuration.xml", PollerConfiguration.class, Impl.JAXB);
        addFile(Source.CONFIG, "rtc-configuration.xml", RTCConfiguration.class, Impl.CASTOR);
        addFile(Source.CONFIG, "scriptd-configuration.xml", ScriptdConfiguration.class, Impl.CASTOR);
        addFile(Source.CONFIG, "syslog-northbounder-configuration.xml", SyslogNorthbounderConfig.class, Impl.JAXB);
        addFile(Source.EXAMPLE, "scriptd-configuration.xml", ScriptdConfiguration.class, Impl.CASTOR);
        addFile(Source.EXAMPLE, "event-proxy/Proxy.events.xml", Events.class, Impl.CASTOR);
        addFile(Source.EXAMPLE, "event-proxy/scriptd-configuration.xml", ScriptdConfiguration.class, Impl.CASTOR);
        addFile(Source.EXAMPLE, "event-proxy/vacuumd-configuration.xml", VacuumdConfiguration.class, Impl.JAXB);
        addFile(Source.CONFIG, "site-status-views.xml", SiteStatusViewConfiguration.class, Impl.CASTOR);
        addFile(Source.CONFIG, "snmp-config.xml", SnmpConfig.class, Impl.JAXB);
        addFile(Source.EXAMPLE, "snmp-config.xml", SnmpConfig.class, Impl.JAXB);
        addFile(Source.CONFIG, "snmp-interface-poller-configuration.xml", SnmpInterfacePollerConfiguration.class, Impl.CASTOR);
        addFile(Source.CONFIG, "statsd-configuration.xml", StatisticsDaemonConfiguration.class, Impl.CASTOR);
        addFile(Source.CONFIG, "surveillance-views.xml", SurveillanceViewConfiguration.class, Impl.CASTOR);
        addFile(Source.EXAMPLE, "surveillance-views.xml", SurveillanceViewConfiguration.class, Impl.CASTOR);
        addFile(Source.CONFIG, "syslogd-configuration.xml", SyslogdConfiguration.class, Impl.CASTOR);
        addFile(Source.CONFIG, "threshd-configuration.xml", ThreshdConfiguration.class, Impl.CASTOR);
        addFile(Source.EXAMPLE, "threshd-configuration.xml", ThreshdConfiguration.class, Impl.CASTOR);
        addFile(Source.CONFIG, "thresholds.xml", ThresholdingConfig.class, Impl.CASTOR);
        addFile(Source.EXAMPLE, "thresholds.xml", ThresholdingConfig.class, Impl.CASTOR);
        addFile(Source.CONFIG, "tl1d-configuration.xml", Tl1dConfiguration.class, Impl.CASTOR);
        addFile(Source.CONFIG, "translator-configuration.xml", EventTranslatorConfiguration.class, Impl.CASTOR);
        addFile(Source.CONFIG, "trapd-configuration.xml", TrapdConfiguration.class, Impl.CASTOR);
        addFile(Source.CONFIG, "users.xml", Userinfo.class, Impl.CASTOR);
        addFile(Source.CONFIG, "vacuumd-configuration.xml", VacuumdConfiguration.class, Impl.JAXB);
        addFile(Source.CONFIG, "xmlrpcd-configuration.xml", XmlrpcdConfiguration.class, Impl.CASTOR);
        addFile(Source.EXAMPLE, "xmlrpcd-configuration.xml", XmlrpcdConfiguration.class, Impl.CASTOR);
        addFile(Source.CONFIG, "eventd-configuration.xml", EventdConfiguration.class, Impl.CASTOR);
        addFile(Source.CONFIG, "service-configuration.xml", ServiceConfiguration.class, Impl.JAXB);
        addFile(Source.CONFIG, "viewsdisplay.xml", Viewinfo.class, Impl.CASTOR);
        addFile(Source.EXAMPLE, "viewsdisplay.xml", Viewinfo.class, Impl.CASTOR);
        addFile(Source.EXAMPLE, "tl1d-configuration.xml", Tl1dConfiguration.class, Impl.CASTOR);
        addFile(Source.CONFIG, "wmi-config.xml", WmiConfig.class, Impl.CASTOR);
        addFile(Source.CONFIG, "wmi-datacollection-config.xml", WmiDatacollectionConfig.class, Impl.CASTOR);
        addFile(Source.CONFIG, "javamail-configuration.xml", JavamailConfiguration.class, Impl.CASTOR);
        addFile(Source.CONFIG, "ackd-configuration.xml", AckdConfiguration.class, Impl.CASTOR);
        addFile(Source.CONFIG, "provisiond-configuration.xml", ProvisiondConfiguration.class, Impl.CASTOR);
        addFile(Source.CONFIG, "reportd-configuration.xml", ReportdConfiguration.class, Impl.CASTOR);
        addFile(Source.CONFIG, "rws-configuration.xml", RwsConfiguration.class, Impl.CASTOR);
        addFile(Source.EXAMPLE, "rws-configuration.xml", RwsConfiguration.class, Impl.CASTOR);
        addFile(Source.CONFIG, "mapsadapter-configuration.xml", MapsAdapterConfiguration.class, Impl.CASTOR);
        addFile(Source.EXAMPLE, "mapsadapter-configuration.xml", MapsAdapterConfiguration.class, Impl.CASTOR);
        addFile(Source.CONFIG, "rancid-configuration.xml", RancidConfiguration.class, Impl.CASTOR);
        addFile(Source.EXAMPLE, "rancid-configuration.xml", RancidConfiguration.class, Impl.CASTOR);
        addFile(Source.CONFIG, "microblog-configuration.xml", MicroblogConfiguration.class, Impl.CASTOR);
        addFile(Source.CONFIG, "snmp-asset-adapter-configuration.xml", SnmpAssetAdapterConfiguration.class, Impl.CASTOR);
        addFile(Source.CONFIG, "jdbc-datacollection-config.xml", JdbcDataCollectionConfig.class, Impl.JAXB);
        addFile(Source.CONFIG, "remote-repository.xml", RemoteRepositoryConfig.class, Impl.JAXB);
        addFile(Source.CONFIG, "vmware-config.xml", VmwareConfig.class, Impl.JAXB);
        addFile(Source.CONFIG, "vmware-datacollection-config.xml", VmwareDatacollectionConfig.class, Impl.JAXB);
        addFile(Source.CONFIG, "vmware-cim-datacollection-config.xml", VmwareCimDatacollectionConfig.class, Impl.JAXB);
        addFile(Source.EXAMPLE, "jvm-datacollection/collectd-configuration.xml", CollectdConfiguration.class, Impl.JAXB);
        addFile(Source.EXAMPLE, "jvm-datacollection/jmx-datacollection-config.xml", JmxDatacollectionConfig.class, Impl.JAXB);
        addFile(Source.EXAMPLE, "jvm-datacollection/jmx-datacollection/ActiveMQ/5.6/ActiveMQBasic0.xml", Mbeans.class, Impl.JAXB);
        addFile(Source.EXAMPLE, "jvm-datacollection/jmx-datacollection/Cassandra/1.1.2/CassandraBasic0.xml", Mbeans.class, Impl.JAXB);
        addFile(Source.EXAMPLE, "jvm-datacollection/jmx-datacollection/JBoss/4/JBossBasic0.xml", Mbeans.class, Impl.JAXB);
        addFile(Source.EXAMPLE, "jvm-datacollection/jmx-datacollection/Jvm/1.6/JvmBasic0.xml", Mbeans.class, Impl.JAXB);
        addFile(Source.EXAMPLE, "jvm-datacollection/jmx-datacollection/Jvm/1.6/JvmLegacy.xml", Mbeans.class, Impl.JAXB);
        addFile(Source.EXAMPLE, "jvm-datacollection/jmx-datacollection/OpenNMS/1.10/OpenNMSBasic0.xml", Mbeans.class, Impl.JAXB);
        addFile(Source.EXAMPLE, "jvm-datacollection/jmx-datacollection/OpenNMS/1.10/OpenNMSLegacy.xml", Mbeans.class, Impl.JAXB);

        // Add all event files
        for (final File file : FileUtils.listFiles(new File(getDaemonEtcDirectory(), "events"),
                                                   new String[] { "xml" },
                                                   true)) {
            addFile(Source.ABSOLUTE,
                    file.getPath(),
                    Events.class,
                    Impl.CASTOR);
        }

        // Add all datacollection group files
        for (final File file : FileUtils.listFiles(new File(getDaemonEtcDirectory(), "datacollection"),
                                                   new String[] { "xml" },
                                                   true)) {
            addFile(Source.ABSOLUTE,
                    file.getPath(),
                    DatacollectionGroup.class,
                    Impl.JAXB);
        }
    }

    /**
     * The list of files to test.
     * 
     * For each XML file to test, this method must return an entry in the list.
     * Each entry consists of the following parts:
     * <ul>
     *   <li>The source to load the resource from</li>
     *   <li>The file to test</li>
     *   <li>The class used for unmarshaling</li>
     *   <li>The implementation to use for unmarshalling</li>
     *   <li>Flag for being lenient</li>
     *   <li>An expected exception message</li>
     * </ul>
     * 
     * The returned file list is stored in {@link #FILES} which is filled in the
     * static constructor.
     * 
     * @return list of parameters for the test
     */
    @Parameterized.Parameters
    public static Collection<Object[]> files() {
        return FILES;
    }

    private final Source source;
    private final String file;
    private final Class<?> clazz;
    private final Impl impl;
    private final boolean lenient;
    private final String exception;

    public WillItUnmarshalTest(final Source source,
            final String file,
            final Class<?> clazz,
            final Impl impl,
            final boolean lenient,
            final String exception) {
        this.source = source;
        this.file = file;
        this.clazz = clazz;
        this.impl = impl;
        this.lenient = lenient;
        this.exception = exception;
    }

    @Before
    public void setUp() throws Exception {
        // Reload castor properties every time
        LocalConfiguration.getInstance().getProperties().clear();
        LocalConfiguration.getInstance().getProperties().load(ConfigurationTestUtils.getInputStreamForResource(this, "/castor.properties"));
    }

    @Test
    public void testUnmarshalling() {
        // Be conservative about what we ship, so don't be lenient
        if (this.lenient == false) {
            LocalConfiguration.getInstance().getProperties().remove(CASTOR_LENIENT_SEQUENCE_ORDERING_PROPERTY);
        }

        final Resource resource = this.createResource();

        // Assert that resource is valied
        assertNotNull("Resource must not be null", resource);

        // Unmarshall the config file
        Object result = null;
        try {
            switch (impl) {
            case CASTOR:
                result = CastorUtils.unmarshal(this.clazz, resource);
                break;

            case JAXB:
                result = JaxbUtils.unmarshal(this.clazz, resource);
                break;

            default:
                fail("Implementation unknown: " + this.impl);
            }

            // Assert that unmarshalling returned a valid result
            assertNotNull("Unmarshalled instance must not be null", result);

        } catch(AssertionFailedError ex) {
            throw ex;

        } catch(Exception ex) {
            // If we have an expected exception, the returned exception muss
            // match - if not the test failed
            if (this.exception != null) {
                assertEquals(this.exception, exception.toString());

            } else {
                fail("Unexpected exception: " + ex);
            }
        }
    }

    /**
     * Create a resource for the config file to unmarshall using the configured
     * source.
     * 
     * @return the Resource 
     */
    public final Resource createResource() {
        // Create a resource for the config file to unmarshall using the
        // configured source
        switch (this.source) {
        case CONFIG:
            return new FileSystemResource(ConfigurationTestUtils.getFileForConfigFile(file));

        case EXAMPLE:
            return new FileSystemResource(ConfigurationTestUtils.getFileForConfigFile("examples/" + file));

        case SPRING:
            return ConfigurationTestUtils.getSpringResourceForResource(this, this.file);

        case ABSOLUTE:
            return new FileSystemResource(this.file);

        default:
            throw new RuntimeException("Source unknown: " + this.source);
        }
    }
}
