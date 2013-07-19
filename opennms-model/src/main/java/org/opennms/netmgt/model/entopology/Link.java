package org.opennms.netmgt.model.entopology;

import java.util.HashSet;
import java.util.Set;

/**
 * This class represents a physical link between 2 network end points
 * such as an Ethernet connection or a virtual link between 2 end points
 * such as an IP address connection to a subnetwork.  Can also be used
 * to represent a network service between to service end points.
 *  
 * @author antonio
 *
 */
public abstract class Link extends Pollable {
	

    public static final String INPUT_LINK_DISPLAY        = "user";
    public static final String LLDP_LINK_DISPLAY         = "lldp" ;
    public static final String CDP_LINK_DISPLAY          = "cdp" ;
    public static final String OSPF_LINK_DISPLAY         = "ospf" ;
    public static final String STP_LINK_DISPLAY          = "spanning-tree" ;
    public static final String DOT1DTPFDB_LINK_DISPLAY   = "dot1d-bridge-forwarding-table" ;
    public static final String DOT1QTPFDB_LINK_DISPLAY   = "dot1q-bridge-forwarding-table" ;
    public static final String PSEUDOBRIDGE_LINK_DISPLAY = "pseudo-bridge" ;
    public static final String PSEUDOMAC_LINK_DISPLAY    = "pseudo-mac" ;


	private Set<EndPoint> m_endpoints = new HashSet<EndPoint>(2);
	
	private Integer m_id;
	
	public Link(EndPoint a, EndPoint b, Integer sourceNode) {
		super(sourceNode);
		a.setLink(this);
		b.setLink(this);
		m_endpoints.add(a);
		m_endpoints.add(b);
	}
	
	public int getId() {
		return m_id;
	}

	public void setId(int id) {
		m_id = id;
	}

	public Set<EndPoint> getEndpoints() {
		return m_endpoints;
	}

	public void setEndpoints(Set<EndPoint> endpoints) {
		m_endpoints = endpoints;
	}

	public abstract String displayLinkType();
	
}