package org.opennms.features.topology.plugins.ncs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.opennms.features.topology.api.GraphContainer;
import org.opennms.features.topology.api.OperationContext;
import org.opennms.features.topology.api.SelectionManager;
import org.opennms.features.topology.api.support.VertexHopGraphProvider.VertexHopCriteria;
import org.opennms.features.topology.api.topo.AbstractSearchProvider;
import org.opennms.features.topology.api.topo.Criteria;
import org.opennms.features.topology.api.topo.Edge;
import org.opennms.features.topology.api.topo.SearchProvider;
import org.opennms.features.topology.api.topo.SearchQuery;
import org.opennms.features.topology.api.topo.SearchResult;
import org.opennms.features.topology.api.topo.VertexRef;
import org.opennms.features.topology.plugins.ncs.internal.NCSCriteriaServiceManager;
import org.opennms.netmgt.model.ncs.NCSComponent;
import org.opennms.netmgt.model.ncs.NCSComponentRepository;

public class NCSSearchProvider extends AbstractSearchProvider implements SearchProvider {

    public class NCSHopCriteria extends VertexHopCriteria{

        private final Set<VertexRef> m_vertices;

        public NCSHopCriteria(String id, HashSet<VertexRef> vertexRefs, String label) {
            setId(id);
            m_vertices = vertexRefs;
            setLabel(label);
        }

        @Override
        public Set<VertexRef> getVertices() {
            return m_vertices;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        @Override
        public int hashCode() {
            return m_vertices.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if(obj instanceof NCSHopCriteria){
                NCSHopCriteria c = (NCSHopCriteria) obj;
                return c.m_vertices.equals(m_vertices);
            }
            return false;
        }
    }

    private static final String NAMESPACE = "ncs";
    private NCSComponentRepository m_ncsComponentRepository;
	private NCSEdgeProvider m_ncsEdgeProvider;
    private NCSCriteriaServiceManager m_serviceManager;
    NCSServiceContainer m_container;

    public void setNcsComponentRepository(NCSComponentRepository ncsComponentRepository) {
		m_ncsComponentRepository = ncsComponentRepository;
        m_container = new NCSServiceContainer(m_ncsComponentRepository);
	}

    protected void selectVerticesForEdge(Criteria criteria, SelectionManager selectionManager) {
	    selectionManager.setSelectedVertexRefs(getVertexRefsForEdges(criteria));
	    
    }

    protected void deselectVerticesForEgde(Criteria criteria, SelectionManager selectionManager) {
        selectionManager.deselectVertexRefs(getVertexRefsForEdges(criteria));
    }

    private List<VertexRef> getVertexRefsForEdges(Criteria criteria) {
        List<VertexRef> vertexRefs = new ArrayList<VertexRef>();
        List<Edge> edges = m_ncsEdgeProvider.getEdges(criteria);
        for(Edge ncsEdge : edges) {
            vertexRefs.add(ncsEdge.getSource().getVertex());
            vertexRefs.add(ncsEdge.getTarget().getVertex());
        }
        return vertexRefs;
    }

	public void setNcsEdgeProvider(NCSEdgeProvider ncsEdgeProvider) {
        m_ncsEdgeProvider = ncsEdgeProvider;
    }
	
	public void setNcsCriteriaServiceManager(NCSCriteriaServiceManager manager) {
	    m_serviceManager = manager;
	}

    @Override
    public String getSearchProviderNamespace() {
        return NAMESPACE;
    }

    @Override
    public boolean contributesTo(String namespace) {
        return "nodes".equals(namespace);
    }

    @Override
    public List<SearchResult> query(SearchQuery searchQuery) {
        List<SearchResult> searchResults = new ArrayList<SearchResult>();

        List<NCSComponent> components = m_ncsComponentRepository.findByType("Service");
        for (NCSComponent component : components) {
            if(searchQuery.matches(component.getName())) {
                searchResults.add(new SearchResult(String.valueOf(component.getId()), NAMESPACE, component.getName()));
            }

        }
        return searchResults;
    }

    @Override
    public void onFocusSearchResult(SearchResult searchResult, OperationContext operationContext) {
        Criteria criteria = NCSEdgeProvider.createCriteria(Collections.singletonList(Long.parseLong(searchResult.getId())));

        if(m_serviceManager.isCriteriaRegistered(NAMESPACE, operationContext.getGraphContainer().getSessionId())) {
            m_serviceManager.unregisterCriteria(NAMESPACE, operationContext.getGraphContainer().getSessionId());
        }
        m_serviceManager.registerCriteria(criteria, operationContext.getGraphContainer().getSessionId());

        //Juniper specifically asked for the selection of NCS services
        selectVerticesForEdge(criteria, operationContext.getGraphContainer().getSelectionManager());

    }

    @Override
    public void onDefocusSearchResult(SearchResult searchResult, OperationContext operationContext) {
        if(m_serviceManager.isCriteriaRegistered(NAMESPACE, operationContext.getGraphContainer().getSessionId())) {
            m_serviceManager.unregisterCriteria(NAMESPACE, operationContext.getGraphContainer().getSessionId());
        }
    }

    @Override
    public boolean supportsPrefix(String searchPrefix) {
        return searchPrefix.equals("ncs=");
    }

    @Override
    public List<VertexRef> getVertexRefsBy(SearchResult searchResult) {
        Criteria criteria = NCSEdgeProvider.createCriteria(Collections.singletonList(Long.parseLong(searchResult.getId())));
        return getVertexRefsForEdges(criteria);
    }

    @Override
    public void addVertexHopCriteria(SearchResult searchResult, GraphContainer container) {
        Criteria criteria = NCSEdgeProvider.createCriteria(Collections.singletonList(Long.parseLong(searchResult.getId())));
        container.addCriteria(new NCSHopCriteria(searchResult.getId(), new HashSet<VertexRef>(getVertexRefsForEdges(criteria)), searchResult.getLabel()));
    }

    @Override
    public void removeVertexHopCriteria(SearchResult searchResult, GraphContainer container) {
        Criteria criteria = NCSEdgeProvider.createCriteria(Collections.singletonList(Long.parseLong(searchResult.getId())));
        container.removeCriteria(new NCSHopCriteria(searchResult.getId(), new HashSet<VertexRef>(getVertexRefsForEdges(criteria)), searchResult.getLabel()));

        if(m_serviceManager.isCriteriaRegistered(NAMESPACE, container.getSessionId())) {
            m_serviceManager.unregisterCriteria(NAMESPACE, container.getSessionId());
        }
    }

    @Override
    public void onCenterSearchResult(SearchResult searchResult, GraphContainer graphContainer) {
        Criteria criteria = NCSEdgeProvider.createCriteria(Collections.singletonList(Long.parseLong(searchResult.getId())));

        if(m_serviceManager.isCriteriaRegistered(NAMESPACE, graphContainer.getSessionId())) {
            m_serviceManager.unregisterCriteria(NAMESPACE, graphContainer.getSessionId());
        }
        m_serviceManager.registerCriteria(criteria, graphContainer.getSessionId());
    }
}