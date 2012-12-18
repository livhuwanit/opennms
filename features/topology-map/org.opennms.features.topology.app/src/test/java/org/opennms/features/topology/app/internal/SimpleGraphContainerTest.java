/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2012 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2012 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.features.topology.app.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.opennms.features.topology.api.EditableGraphProvider;
import org.opennms.features.topology.api.topo.Connector;
import org.opennms.features.topology.api.topo.Edge;
import org.opennms.features.topology.api.topo.EdgeRef;
import org.opennms.features.topology.api.topo.GraphProvider;
import org.opennms.features.topology.api.topo.Vertex;
import org.opennms.features.topology.api.topo.VertexRef;
import org.opennms.features.topology.app.internal.SimpleGraphContainer.GEdge;
import org.opennms.features.topology.app.internal.SimpleGraphContainer.GVertex;

import com.vaadin.data.Item;
import com.vaadin.data.Container.ItemSetChangeEvent;
import com.vaadin.data.Container.ItemSetChangeListener;
import com.vaadin.data.util.BeanItem;

public class SimpleGraphContainerTest {

    @Test
    public void testSimpleGraphContainer() {
        SimpleGraphContainer graphContainer = new SimpleGraphContainer(graphProvider());
        Collection<? extends Vertex> vertexIds = graphContainer.getBaseTopology().getVertices();
        Collection<? extends Edge> edgeIds = graphContainer.getBaseTopology().getEdges();
        
        Edge edge = edgeIds.iterator().next();
        
        assertEquals("e0", edge.getId());
        
        assertEquals(2, vertexIds.size());
        assertEquals(1, edgeIds.size());
        
        
    }

	private EditableGraphProvider graphProvider() {
		return new TestTopologyProvider("test");
	}
    
    @Test
    public void testGraph() {
        SimpleGraphContainer graphContainer = new SimpleGraphContainer(graphProvider());
        TopoGraph graph = graphContainer.getCompleteGraph();
        
        List<TopoVertex> vertices = graph.getVertices();
        assertEquals(2, vertices.size());
        
        List<TopoEdge> edges = graph.getEdges();
        assertEquals(1, edges.size());
        
    }
    
    @Test
    public void testGroupingVertices() {
        EditableGraphProvider topologyProvider = graphProvider();
        
        Vertex groupId = topologyProvider.addGroup(this.getClass().getSimpleName(), "groupIcon.jpg");
        topologyProvider.setParent("v0", groupId);
        topologyProvider.setParent("v1", groupId);
        
        SimpleGraphContainer graphContainer = new SimpleGraphContainer(topologyProvider);
        graphContainer.setLayoutAlgorithm(new SimpleLayoutAlgorithm());
        graphContainer.setSemanticZoomLevel(0);
        
        Collection<Vertex> gcIds = graphContainer.getBaseTopology().getVertices();
        assertEquals(3, gcIds.size());
        
        for(Vertex gcId : gcIds) {
            Item gcItem = gcId.getItem();
            Boolean leaf = (Boolean) gcItem.getItemProperty("leaf").getValue();
            System.out.println("Expecting gcItem: " + gcItem + " id: " + gcId + " leaf is true: " + leaf);
            if(leaf) {
                Object parentId = graphContainer.getBaseTopology().getParent(gcId);
                assertNotNull(parentId);
                
                Object semanticZoomLevel = gcItem.getItemProperty("semanticZoomLevel").getValue();
                assertEquals(1, semanticZoomLevel);
            }
        }
        
        
        TopoGraph graph = graphContainer.getCompleteGraph();
        
        List<TopoVertex> vertices = graph.getVertices();
        assertEquals(3, vertices.size());
        for(TopoVertex v : vertices) {
            if( graphContainer.getGroupId( v.getItemId() ) == null) {
                assertEquals(0, v.getSemanticZoomLevel());
            }else {
                assertEquals(1, v.getSemanticZoomLevel());
            }
        }
        
        List<TopoVertex> leafVertices = graph.getLeafVertices();
        assertEquals(2, leafVertices.size());
        for(TopoVertex v: leafVertices) {
            assertEquals(1, v.getSemanticZoomLevel());
        }
        
        List<TopoEdge> edges = graph.getEdges();
        assertEquals(1, edges.size());
    }
    
    @Test
    public void testUpdateTopologyProviderUpdatesGraphContainer() {
        EditableGraphProvider topologyProvider = graphProvider();
        
        //Setup the graphcontainer
        SimpleGraphContainer graphContainer = new SimpleGraphContainer(topologyProvider);
        graphContainer.setLayoutAlgorithm(new SimpleLayoutAlgorithm());
        graphContainer.setSemanticZoomLevel(0);
        
        //Add another vertex to the TopologyProvider
        Vertex vertId = topologyProvider.addVertex();
        
        Collection<VertexRef> gcIds = graphContainer.getVertexIds();
        assertEquals(3, gcIds.size());
        
        Vertex vertId2 = topologyProvider.addVertex();
        assertEquals(4, gcIds.size());
       
        Object groupId = topologyProvider.addGroup(this.getClass().getSimpleName(), "iconofgroup.jpg");
        assertEquals(5, graphContainer.getVertexIds().size());
        
        topologyProvider.setParent(vertId, groupId);
        topologyProvider.setParent(vertId2, groupId);
        
        Collection<VertexRef> vertexKeys = graphContainer.getVertexIds();
        for(VertexRef vertexKey : vertexKeys) {
            Vertex vItem = graphContainer.getVertex(vertexKey);
            if(vItem.getId() == vertId.getId() || vItem.getId() == vertId2.getId()) {
                assertEquals(groupId, vItem.getParent());
            }
        }
        
        
    }
    
    @Test
    public void testGraphContainerSendUpdateEvents() {
        final AtomicInteger eventsReceived = new AtomicInteger(0);
        
        GraphProvider topologyProvider = graphProvider();
        
        //Setup the graphcontainer
        SimpleGraphContainer graphContainer = new SimpleGraphContainer(topologyProvider);
        graphContainer.setLayoutAlgorithm(new SimpleLayoutAlgorithm());
        graphContainer.setSemanticZoomLevel(0);
        
        graphContainer.addVertexListener(new ItemSetChangeListener() {
            
            @Override
            public void containerItemSetChange(ItemSetChangeEvent event) {
                eventsReceived.incrementAndGet();
            }
        });
        
        
        
        //Add another vertex to the TopologyProvider
        Vertex vertId = topologyProvider.addVertex(0, 0);
        assertEquals(2, eventsReceived.get());
        eventsReceived.set(0);
        
        Vertex vertId2 = topologyProvider.addVertex(0, 0);
        assertEquals(2, eventsReceived.get());
        eventsReceived.set(0);
       
        Vertex groupId = topologyProvider.addGroup(this.getClass().getSimpleName(), "iconofgroup.jpg");
        assertEquals(5, graphContainer.getVertexIds().size());
        assertEquals(2, eventsReceived.get());
        eventsReceived.set(0);
        
        topologyProvider.setParent(vertId, groupId);
        //assertEquals(2, eventsReceived.get());
        assertEquals(1, eventsReceived.get());
        eventsReceived.set(0);
        
        topologyProvider.setParent(vertId2, groupId);
        //assertEquals(2, eventsReceived.get());
        assertEquals(1, eventsReceived.get());
        eventsReceived.set(0);
        
        Collection<Vertex> vertices = graphContainer.getBaseTopology().getVertices();
        for(Vertex vertex : vertices) {
            if(vertex.getId() == vertId.getId() || vertex.getId() == vertId2.getId()) {
                assertEquals(groupId, vertex.getGroupId());
                assertEquals(1, vertex.getSemanticZoomLevel());
            }else {
                assertEquals(0, vertex.getSemanticZoomLevel());
            }
        }
    }
    
    @Test
    public void testGraphContainerUpdatesGraph() {
        final AtomicInteger eventsReceived = new AtomicInteger(0);
        
        GraphProvider topologyProvider = graphProvider();
        
        //Setup the graphcontainer
        SimpleGraphContainer graphContainer = new SimpleGraphContainer(topologyProvider);
        graphContainer.setLayoutAlgorithm(new SimpleLayoutAlgorithm());
        graphContainer.setSemanticZoomLevel(0);
        

        final TopoGraph graph = graphContainer.getCompleteGraph();
        
        graphContainer.addVertexListener(new ItemSetChangeListener() {
            
            @Override
            public void containerItemSetChange(ItemSetChangeEvent event) {
                graph.update();
            }
        });
        
        
        
        //Add another vertex to the TopologyProvider
        Vertex vertId = topologyProvider.addVertex();
        TopoVertex v = graph.getVertexByItemId(findByItemId(graphContainer, vertId));
        assertNotNull(v);
        
        Vertex vertId2 = topologyProvider.addVertex();
        TopoVertex v2 = graph.getVertexByItemId(findByItemId(graphContainer, vertId2));
        assertNotNull(v2);
        
        Vertex groupId = topologyProvider.addGroup(this.getClass().getSimpleName(), "iconofgroup.jpg");
        assertEquals(5, graphContainer.getVertexIds().size());
        TopoVertex g = graph.getVertexByItemId(findByItemId(graphContainer, groupId));
        assertNotNull(g);
        
        topologyProvider.setParent(vertId, groupId);
        v = graph.getVertexByItemId(findByItemId(graphContainer, vertId));
        assertEquals( g.getItemId(), graphContainer.getGroupId( v.getItemId() ));
        
        topologyProvider.setParent(vertId2, groupId);
        v2 = graph.getVertexByItemId(findByItemId(graphContainer, vertId2));
        assertEquals( g.getItemId(), graphContainer.getGroupId( v2.getItemId() ));
        
        Collection<Vertex> vertices = graphContainer.getBaseTopology().getVertices();
        for(Vertex vertex : vertices) {
            if(vertex.getId() == vertId.getId() || vertex.getId() == vertId2.getId()) {
                assertEquals(groupId, vertex.getGroupId());
                assertEquals(1, vertex.getSemanticZoomLevel());
            }else {
                assertEquals(0, vertex.getSemanticZoomLevel());
            }
        }
    }
    
    public Object findByItemId(SimpleGraphContainer graphContainer, Object vertexId) {
        
        Collection<Object> vertexKeys = graphContainer.getVertexIds();
        for(Object vertexKey : vertexKeys) {
            BeanItem<GVertex> vItem = graphContainer.getVertexContainer().getItem(vertexKey);
            GVertex gVert = vItem.getBean();
            if(gVert.getItemId().equals(vertexId)) {
                return gVert.getKey();
            }
        }
        
        return null;
    }
    
    
    @Test
    public void testUpdateTopologyProviderUpdatesGraph() {
        GraphProvider topologyProvider = graphProvider();
        
        //Setup the graphcontainer
        SimpleGraphContainer graphContainer = new SimpleGraphContainer(topologyProvider);
        graphContainer.setLayoutAlgorithm(new SimpleLayoutAlgorithm());
        graphContainer.setSemanticZoomLevel(0);
        
        TopoGraph graph = graphContainer.getCompleteGraph();
        List<TopoVertex> vertices = graph.getVertices();
        assertEquals(2, vertices.size());
        
        //Add another vertex to the TopologyProvider
        topologyProvider.addVertex();
        graph.update();
        
        assertEquals(3, topologyProvider.getVertices().size());
        assertEquals(3, graphContainer.getBaseTopology().getVertices().size());
        assertEquals(3, graph.getVertices().size());
        
        Collection<Object> gcIds = graphContainer.getVertexIds();
        assertEquals(3, gcIds.size());
        
        topologyProvider.addVertex();
        graph.update();
        assertEquals(4, gcIds.size());
        
        assertEquals(4, graph.getVertices().size());
        
    }
    
    @Test
    public void testGraphContainerElementIds() {
        GraphProvider topologyProvider = graphProvider();

        EdgeRef toprEdgeId = topologyProvider.getEdgeIds().iterator().next();
        
        Edge testEdge = (TestEdge) topologyProvider.getEdge(toprEdgeId);
        Connector sourceVertex = testEdge.getSource();
        Connector targetVertex = testEdge.getTarget();
        
        
        SimpleGraphContainer graphContainer = new SimpleGraphContainer(topologyProvider);
        
        GVertex sourceGVertex = findGVertexWithToprId(graphContainer, sourceVertex.getId());
        GVertex targetGVertex = findGVertexWithToprId(graphContainer, targetVertex.getId());
        
        GEdge gEdge = findGEdgeWithToprId(graphContainer, testEdge.getId());
        
        assertEquals(sourceGVertex.getItemId(), gEdge.getSource().getItemId());
        assertEquals(targetGVertex.getItemId(), gEdge.getTarget().getItemId());
        assertEquals(sourceGVertex.getKey(), gEdge.getSource().getKey());
        assertEquals(targetGVertex.getKey(), gEdge.getTarget().getKey());
        
        
        Vertex groupId = topologyProvider.addGroup(this.getClass().getSimpleName(), "iconGroup.png");
        topologyProvider.setParent(sourceVertex.getId(), groupId);
        topologyProvider.setParent(targetVertex.getId(), groupId);
        
        
        sourceGVertex = findGVertexWithToprId(graphContainer, sourceVertex.getId());
        targetGVertex = findGVertexWithToprId(graphContainer, targetVertex.getId());
        
        gEdge = findGEdgeWithToprId(graphContainer, testEdge.getId());
        
        assertEquals(sourceGVertex.getItemId(), gEdge.getSource().getItemId());
        assertEquals(targetGVertex.getItemId(), gEdge.getTarget().getItemId());
        assertEquals(sourceGVertex.getKey(), gEdge.getSource().getKey());
        assertEquals(targetGVertex.getKey(), gEdge.getTarget().getKey());
        
    }
    
    private GEdge findGEdgeWithToprId(SimpleGraphContainer graphContainer, String id) {
        for(Object edgeId : graphContainer.getEdgeIds()) {
            GEdge edge = (GEdge) graphContainer.getEdgeContainer().getItem(edgeId).getBean();
            if (id.equals(edge.getItemId())) {
                return edge;
            }
        }
        return null;
    }

    private GVertex findGVertexWithToprId(SimpleGraphContainer graphContainer, String id) {
        for(Object vertexId : graphContainer.getVertexIds()) {
            GVertex vertex = (GVertex) graphContainer.getVertexContainer().getItem(vertexId).getBean();
            if (id.equals(vertex.getItemId())) {
                return vertex;
            }
        }
        return null;
    }
    
    @Test
    public void testGraphElementIds() {
        GraphProvider topologyProvider = graphProvider();

        Object toprEdgeId = topologyProvider.getEdgeIds().iterator().next();
        
        TestEdge testEdge = (TestEdge) topologyProvider.getEdgeContainer().getItem(toprEdgeId).getBean();
        TestVertex sourceTestVertex = testEdge.getSource();
        TestVertex targetTestVertex = testEdge.getTarget();
        
        
        SimpleGraphContainer graphContainer = new SimpleGraphContainer(topologyProvider);
        TopoGraph graph = graphContainer.getCompleteGraph();
        
        TopoVertex sourceVertex = findVertexWithToprId(graph, graphContainer, sourceTestVertex.getId());
        TopoVertex targetVertex = findVertexWithToprId(graph, graphContainer, targetTestVertex.getId());
        
        TopoEdge edge = findEdgeWithToprId(graph, graphContainer, testEdge.getId());
        
        assertNotNull(edge);
        assertNotNull(edge.getSourceVertex());
        assertNotNull(edge.getTargetVertex());
        assertEquals(sourceVertex.getItemId(), edge.getSourceVertex().getItemId());
        assertEquals(targetVertex.getItemId(), edge.getTargetVertex().getItemId());
        assertEquals(sourceVertex.getKey(), edge.getSourceVertex().getKey());
        assertEquals(targetVertex.getKey(), edge.getTargetVertex().getKey());
        
        
        Object groupId = topologyProvider.addGroup(this.getClass().getSimpleName(), "iconGroup.png");
        topologyProvider.setParent(sourceTestVertex.getId(), groupId);
        topologyProvider.setParent(targetTestVertex.getId(), groupId);
        
        // Need to call graph.update since the topology component is not here to do it
        graph.update();
        graph.update();
        graph.update();
        
        System.err.printf("Vertices: %s\n", graph.getVertices());
        System.err.printf("Edges: %s\n", graph.getEdges());
        
        
        sourceVertex = findVertexWithToprId(graph, graphContainer, sourceTestVertex.getId());
        targetVertex = findVertexWithToprId(graph, graphContainer, targetTestVertex.getId());
        
        edge = findEdgeWithToprId(graph, graphContainer, testEdge.getId());
     
        assertEquals(sourceVertex.getItemId(), edge.getSourceVertex().getItemId());
        assertEquals(targetVertex.getItemId(), edge.getTargetVertex().getItemId());
        assertEquals(sourceVertex.getKey(), edge.getSourceVertex().getKey());
        assertEquals(targetVertex.getKey(), edge.getTargetVertex().getKey());
        
    }
    
    @Test
    public void testRemoveAllItemsUpdateCorrectly() {
        GraphProvider topologyProvider = graphProvider();
     
        //Setup the graphcontainer
        SimpleGraphContainer graphContainer = new SimpleGraphContainer(topologyProvider);
        graphContainer.setLayoutAlgorithm(new SimpleLayoutAlgorithm());
        graphContainer.setSemanticZoomLevel(0);
        
        TopoGraph graph = graphContainer.getCompleteGraph();
        List<TopoVertex> vertices = graph.getVertices();
        
        assertEquals(2, vertices.size());
        assertEquals(2, topologyProvider.getVertices().size());
        assertEquals(1, topologyProvider.getEdges().size());
        assertEquals(2, graphContainer.getVertexIds().size());
        assertEquals(1, graphContainer.getEdgeIds().size());
        assertEquals(2, graph.getVertices().size());
        
        topologyProvider.load(null);
        
        assertEquals(2, topologyProvider.getVertices().size());
        assertEquals(1, topologyProvider.getEdges().size());
        assertEquals(2, graphContainer.getVertexIds().size());
        assertEquals(1, graphContainer.getEdgeIds().size());
        assertEquals(2, graph.getVertices().size());
        
        
    }

    private TopoEdge findEdgeWithToprId(TopoGraph graph, SimpleGraphContainer gc, String edgeToprId) {
        GEdge gEdge = findGEdgeWithToprId(gc, edgeToprId);
        
        assertNotNull(gEdge);
        for(TopoEdge e : graph.getEdges()) {
            if (e.getItemId().equals(gEdge.getKey())) {
                return e;
            }
        }
        return null;
    }

    private TopoVertex findVertexWithToprId(TopoGraph graph, SimpleGraphContainer gc, String vertexToprId) {
        
        GVertex gVertex = findGVertexWithToprId(gc, vertexToprId);
        assertNotNull(gVertex);
        
        for(TopoVertex v : graph.getVertices()) {
            if (v.getItemId().equals(gVertex.getKey())) {
                return v;
            }
        }
        return null;
    }


}
