/*
 * KIELER - Kiel Integrated Environment for Layout Eclipse RichClient
 *
 * http://www.informatik.uni-kiel.de/rtsys/kieler/
 * 
 * Copyright 2012 by
 * + Christian-Albrechts-University of Kiel
 *   + Department of Computer Science
 *     + Real-Time and Embedded Systems Group
 * 
 * This code is provided under the terms of the Eclipse Public License (EPL).
 * See the file epl-v10.html for the license text.
 */
package de.cau.cs.kieler.papyrus.sequence.p2cycles;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.elk.alg.layered.graph.LEdge;
import org.eclipse.elk.alg.layered.graph.LGraph;
import org.eclipse.elk.alg.layered.graph.LNode;
import org.eclipse.elk.alg.layered.properties.InternalProperties;
import org.eclipse.elk.core.util.IElkProgressMonitor;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.util.ElkGraphUtil;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import de.cau.cs.kieler.papyrus.sequence.ISequenceLayoutProcessor;
import de.cau.cs.kieler.papyrus.sequence.LayoutContext;
import de.cau.cs.kieler.papyrus.sequence.graph.SLifeline;
import de.cau.cs.kieler.papyrus.sequence.graph.SMessage;
import de.cau.cs.kieler.papyrus.sequence.properties.InternalSequenceProperties;

/**
 * Heuristic implementation of cycle breaking. Breaks the cycles in the layered graph of the layout
 * context. The cycle breakers of the KLay Layered algorithm break cycles by reversing edges. That's not
 * what we want here. Instead, we break cycles by splitting one of the two end points of an edge that
 * would otherwise be reversed. While a node usually represents both the start and end point of a
 * message, split nodes only represent one of the end points each. With that, the corresponding message
 * is not drawn horizontally anymore.
 * 
 * @author grh
 * @kieler.design proposed grh
 * @kieler.rating proposed yellow grh
 */
public final class SCycleBreaker implements ISequenceLayoutProcessor {
    /** A node with this ID was not visited yet. */
    private static final int NOT_VISITED = 0;
    /** A node with this ID was already visited, but not as part of the current path. */
    private static final int VISITED_OTHER_PATH = 1;
    /** A node with this ID was already visited on the current path. */
    private static final int VISITED_CURRENT_PATH = 2;
    
    /** The list of nodes that have to be split. */
    private Set<LNode> split;
    /** The list of nodes that were already visited in the current iteration. */
    private List<LNode> chain;
    

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(final LayoutContext context, final IElkProgressMonitor progressMonitor) {
        progressMonitor.begin("Cycle Breaking", 1);

        // The set of edges to be split after the cycle detecting phase
        split = Sets.newHashSet();
        chain = Lists.newArrayListWithCapacity(context.lgraph.getLayerlessNodes().size());

        // Use node IDs to indicate if a node was already visited
        for (LNode node : context.lgraph.getLayerlessNodes()) {
            node.id = NOT_VISITED;
        }

        // Start a dfs only when the node was not visited by any other earlier dfs
        for (LNode node : context.lgraph.getLayerlessNodes()) {
            if (node.id == NOT_VISITED) {
                dfs(node);
            }
        }

        // split all nodes in the hashSet
        for (LNode node : split) {
            splitNode(context.lgraph, node);
        }

        progressMonitor.done();
    }
    

    /**
     * Split the given node into two nodes for each of the corresponding lifelines. Rearrange edges
     * in order to have only edges showing the order at one lifeline.
     * 
     * @param node
     *            the node to be split
     */
    private void splitNode(final LGraph lgraph, final LNode node) {
        // Create new LNode in the LayeredGraph
        LNode newNode = new LNode(lgraph);
        lgraph.getLayerlessNodes().add(newNode);

        SMessage message = (SMessage) node.getProperty(InternalProperties.ORIGIN);
        SLifeline sourceLL = message.getSource();
        SLifeline targetLL = message.getTarget();
        Iterator<LEdge> oEdges = node.getConnectedEdges().iterator();
        while (oEdges.hasNext()) {
            LEdge edge = oEdges.next();
            SLifeline belongsTo = edge.getProperty(InternalSequenceProperties.BELONGS_TO_LIFELINE);
            if (belongsTo == targetLL) {
                // if edge belongs to targetLifeline, rebase it to newNode
                if (edge.getSource().getNode() == node) {
                    edge.getSource().setNode(newNode);
                } else if (edge.getTarget().getNode() == node) {
                    edge.getTarget().setNode(newNode);
                }
            }
            // if edge belongs to sourceLifeline, leave it as it was
        }
        node.setProperty(InternalSequenceProperties.BELONGS_TO_LIFELINE, sourceLL);
        newNode.setProperty(InternalSequenceProperties.BELONGS_TO_LIFELINE, targetLL);
        newNode.setProperty(InternalProperties.ORIGIN, message);
    }

    /**
     * Process a depth first search starting with the given node and check for cycles.
     * 
     * @param node
     *            the node to start with
     */
    private void dfs(final LNode node) {
        if (node.id == VISITED_CURRENT_PATH) {
            // This node was already visited in current path
            // Find uppermost LNode in current chain and add it to split
            addUppermostNode(node);
        } else {
            // This node has not been visited in current path
            chain.add(node);
            // Mark as visited
            node.id = VISITED_CURRENT_PATH;

            // Process successors
            for (LEdge edge : node.getOutgoingEdges()) {
                dfs(edge.getTarget().getNode());
            }
            // Mark as visited in previous path
            node.id = VISITED_OTHER_PATH;
            chain.remove(chain.size() - 1);
        }
    }

    /**
     * Find uppermost LNode in the current cyclic chain and add it to the set of LNodes to be split.
     * 
     * @param foundNode
     *            the uppermost node in the current chain
     */
    private void addUppermostNode(final LNode foundNode) {
        LNode uppermost = foundNode;
        double uppermostPos = Double.MAX_VALUE;
        int foundIndex = chain.indexOf(foundNode);
        for (int i = foundIndex; i < chain.size(); i++) {
            LNode node = chain.get(i);
            SMessage message = (SMessage) node.getProperty(InternalProperties.ORIGIN);
            ElkEdge edge = (ElkEdge) message.getProperty(InternalProperties.ORIGIN);
            // Compare only sourcePositions since messages can only lead downwards or horizontal
            double sourceYPos = ElkGraphUtil.firstEdgeSection(edge, false, false).getStartY();
            if (sourceYPos < uppermostPos) {
                uppermostPos = sourceYPos;
                uppermost = node;
            }
        }
        split.add(uppermost);
    }
    
}
