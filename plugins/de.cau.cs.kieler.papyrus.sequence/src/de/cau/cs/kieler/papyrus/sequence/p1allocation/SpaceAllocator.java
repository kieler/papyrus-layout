/*
 * KIELER - Kiel Integrated Environment for Layout Eclipse RichClient
 *
 * http://www.informatik.uni-kiel.de/rtsys/kieler/
 * 
 * Copyright 2015 by
 * + Kiel University
 *   + Department of Computer Science
 *     + Real-Time and Embedded Systems Group
 * 
 * This code is provided under the terms of the Eclipse Public License (EPL).
 * See the file epl-v10.html for the license text.
 */
package de.cau.cs.kieler.papyrus.sequence.p1allocation;

import java.util.LinkedList;
import java.util.List;

import de.cau.cs.kieler.core.alg.IKielerProgressMonitor;
import de.cau.cs.kieler.klay.layered.graph.LEdge;
import de.cau.cs.kieler.klay.layered.graph.LGraph;
import de.cau.cs.kieler.klay.layered.graph.LNode;
import de.cau.cs.kieler.klay.layered.graph.LPort;
import de.cau.cs.kieler.papyrus.sequence.ISequenceLayoutProcessor;
import de.cau.cs.kieler.papyrus.sequence.LayoutContext;
import de.cau.cs.kieler.papyrus.sequence.graph.SComment;
import de.cau.cs.kieler.papyrus.sequence.graph.SGraphElement;
import de.cau.cs.kieler.papyrus.sequence.graph.SMessage;
import de.cau.cs.kieler.papyrus.sequence.properties.SequenceArea;
import de.cau.cs.kieler.papyrus.sequence.properties.SequenceDiagramProperties;

/**
 * Allocates vertical space for various objects by introducing dummy nodes in the LGraph. Space is
 * required wherever messages cannot be allowed to be placed. This includes space required for message
 * comments or for headers of combined fragments.
 * 
 * @author grh
 * @author cds
 */
public final class SpaceAllocator implements ISequenceLayoutProcessor {

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(final LayoutContext context, final IKielerProgressMonitor progressMonitor) {
        progressMonitor.begin("Space Allocation", 1);
        
        // Allocate space for the header of combined fragments
        allocateSpaceForCombinedFragmentHeaders(context);

        // Allocate space for comments by introducing dummy nodes
        allocateSpaceForComments(context);

        // Allocate space for empty areas
        allocateSpaceForEmptyAreas(context);
        
        progressMonitor.done();
    }
    
    
    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Space Allocation

    /**
     * Allocate space for the header of combined fragments.
     * 
     * @param context
     *            the layout context that contains all relevant information for the current layout run.
     */
    private void allocateSpaceForCombinedFragmentHeaders(final LayoutContext context) {
        // Add dummy nodes before the first messages of combined fragments to have enough space
        // above the topmost message of the area
        List<SequenceArea> areas = context.sgraph.getProperty(SequenceDiagramProperties.AREAS);
        if (areas != null) {
            for (SequenceArea area : areas) {
                if (area.getSubAreas().size() > 0) {
                    // Find the uppermost message contained in the combined fragment
                    double minYPos = Double.MAX_VALUE;
                    SMessage uppermostMessage = null;
                    for (Object messageObj : area.getMessages()) {
                        SMessage message = (SMessage) messageObj;
                        if (message.getSourceYPos() < minYPos) {
                            minYPos = message.getSourceYPos();
                            uppermostMessage = message;
                        }
                    }
                    
                    if (uppermostMessage != null) {
                        LNode node = uppermostMessage.getProperty(
                                SequenceDiagramProperties.LAYERED_NODE);
                        createLGraphDummyNode(context.lgraph, node, true);
                    }
                }
            }
        }
    }

    /**
     * Allocate space for placing the comments near to their attached elements.
     * 
     * @param context
     *            the layout context that contains all relevant information for the current layout run.
     */
    private void allocateSpaceForComments(final LayoutContext context) {
        for (SComment comment : context.sgraph.getComments()) {
            // Check to which kind of object the comment is attached
            SMessage attachedMess = null;
            for (SGraphElement element : comment.getAttachedTo()) {
                if (element instanceof SMessage) {
                    attachedMess = (SMessage) element;
                }
            }
            
            if (attachedMess != null) {
                // Get height of the comment and calculate number of dummy nodes needed
                double height = comment.getSize().y;
                int dummys = (int) Math.ceil(height / context.messageSpacing);
                
                // Add dummy nodes in the layered graph
                LNode lnode = attachedMess.getProperty(SequenceDiagramProperties.LAYERED_NODE);
                if (lnode != null) {
                    for (int i = 0; i < dummys; i++) {
                        createLGraphDummyNode(context.lgraph, lnode, true);
                    }
                    comment.setMessage(attachedMess);
                    attachedMess.getComments().add(comment);
                }
            }
        }
    }

    /**
     * Add dummy nodes to the layered graph in order to allocate space for empty areas.
     * 
     * @param context
     *            the layout context that contains all relevant information for the current layout run.
     */
    private void allocateSpaceForEmptyAreas(final LayoutContext context) {
        List<SequenceArea> areas = context.sgraph.getProperty(SequenceDiagramProperties.AREAS);
        if (areas != null) {
            for (SequenceArea area : areas) {
                if (area.getMessages().size() == 0) {
                    Object nextMess = area.getNextMessage();
                    if (nextMess != null) {
                        LNode node = ((SMessage) nextMess)
                                .getProperty(SequenceDiagramProperties.LAYERED_NODE);
                        if (node != null) {
                            // Create two dummy nodes before node to have enough space for the empty
                            // area
                            createLGraphDummyNode(context.lgraph, node, true);
                            createLGraphDummyNode(context.lgraph, node, true);
                        }
                    }
                }
            }
        }
    }
    
    
    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Utility Methods

    /**
     * Creates a dummy node in the layered graph, that is placed near the given node. Every
     * connected edge of the original node is redirected to the dummy node.
     * 
     * @param lgraph
     *            the layered graph
     * @param node
     *            the node, that gets a predecessor
     * @param beforeNode
     *            if true, the dummy will be inserted before the node, behind the node otherwise
     */
    private void createLGraphDummyNode(final LGraph lgraph, final LNode node, final boolean beforeNode) {
        LNode dummy = new LNode(lgraph);
        
        LPort dummyIn = new LPort();
        dummyIn.setNode(dummy);
        
        LPort dummyOut = new LPort();
        dummyOut.setNode(dummy);
        
        LPort newPort = new LPort();
        newPort.setNode(node);

        LEdge dummyEdge = new LEdge();

        // To avoid concurrent modification, two lists are needed
        if (beforeNode) {
            List<LEdge> incomingEdges = new LinkedList<LEdge>();
            
            for (LEdge edge : node.getIncomingEdges()) {
                incomingEdges.add(edge);
            }
            
            for (LEdge edge : incomingEdges) {
                edge.setTarget(dummyIn);
            }
            
            dummyEdge.setSource(dummyOut);
            dummyEdge.setTarget(newPort);
        } else {
            List<LEdge> outgoingEdges = new LinkedList<LEdge>();
            
            for (LEdge edge : node.getOutgoingEdges()) {
                outgoingEdges.add(edge);
            }
            
            for (LEdge edge : outgoingEdges) {
                edge.setSource(dummyOut);
            }
            
            dummyEdge.setTarget(dummyIn);
            dummyEdge.setSource(newPort);
        }
        
        lgraph.getLayerlessNodes().add(dummy);
    }

}
