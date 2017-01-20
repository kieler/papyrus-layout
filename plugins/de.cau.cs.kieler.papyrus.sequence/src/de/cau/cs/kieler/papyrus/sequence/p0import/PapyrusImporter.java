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
package de.cau.cs.kieler.papyrus.sequence.p0import;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.elk.alg.layered.graph.LEdge;
import org.eclipse.elk.alg.layered.graph.LGraph;
import org.eclipse.elk.alg.layered.graph.LLabel;
import org.eclipse.elk.alg.layered.graph.LNode;
import org.eclipse.elk.alg.layered.graph.LPort;
import org.eclipse.elk.alg.layered.properties.InternalProperties;
import org.eclipse.elk.core.util.IElkProgressMonitor;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.ElkEdgeSection;
import org.eclipse.elk.graph.ElkLabel;
import org.eclipse.elk.graph.ElkNode;
import org.eclipse.elk.graph.util.ElkGraphUtil;

import com.google.common.collect.Maps;

import de.cau.cs.kieler.papyrus.sequence.ISequenceLayoutProcessor;
import de.cau.cs.kieler.papyrus.sequence.LayoutContext;
import de.cau.cs.kieler.papyrus.sequence.graph.SComment;
import de.cau.cs.kieler.papyrus.sequence.graph.SGraph;
import de.cau.cs.kieler.papyrus.sequence.graph.SGraphElement;
import de.cau.cs.kieler.papyrus.sequence.graph.SLifeline;
import de.cau.cs.kieler.papyrus.sequence.graph.SMessage;
import de.cau.cs.kieler.papyrus.sequence.properties.InternalSequenceProperties;
import de.cau.cs.kieler.papyrus.sequence.properties.MessageType;
import de.cau.cs.kieler.papyrus.sequence.properties.NodeType;
import de.cau.cs.kieler.papyrus.sequence.properties.SequenceArea;
import de.cau.cs.kieler.papyrus.sequence.properties.SequenceDiagramOptions;
import de.cau.cs.kieler.papyrus.sequence.properties.SequenceExecution;

/**
 * Turns the KGraph of a layout context into an SGraph and an LGraph.
 * 
 * @author grh
 * @kieler.design 2012-11-20 cds, msp
 * @kieler.rating proposed yellow grh
 */
public final class PapyrusImporter implements ISequenceLayoutProcessor {

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(final LayoutContext context, final IElkProgressMonitor progressMonitor) {
        progressMonitor.begin("Graph import", 1);
        
        context.sgraph = importGraph(context.elkgraph);
        context.lgraph = createLayeredGraph(context.sgraph);
        
        progressMonitor.done();
    }
    
    
    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // SGraph Creation
    
    /**
     * Builds a PGraph out of a given KGraph by associating every ElkNode to a PLifeline and every
     * KEdge to a PMessage.
     * 
     * @param topNode
     *            the KGraphElement, that holds the nodes
     * @return the built SGraph
     */
    private SGraph importGraph(final ElkNode topNode) {
        // Create a graph object
        SGraph sgraph = new SGraph();
        
        // Initialize node-lifeline and edge-message maps
        HashMap<ElkNode, SLifeline> nodeMap = Maps.newHashMap();
        HashMap<ElkEdge, SMessage> edgeMap = Maps.newHashMap();
        
        // Get the list of areas
        List<SequenceArea> areas = topNode.getProperty(SequenceDiagramOptions.AREAS);

        // Create lifeline objects
        for (ElkNode node : topNode.getChildren()) {
            createLifeline(sgraph, nodeMap, node);
        }

        // Walk through lifelines (create their messages) and comments
        for (ElkNode node : topNode.getChildren()) {
            NodeType nodeType = node.getProperty(SequenceDiagramOptions.NODE_TYPE);
            if (nodeType == NodeType.LIFELINE) {
                // Node is a lifeline

                // Create SMessages for each of the outgoing edges
                createMessages(sgraph, nodeMap, edgeMap, areas, node);

                // Handle found messages (incoming messages)
                createIncomingMessages(sgraph, nodeMap, edgeMap, node);
            } else if (nodeType == NodeType.COMMENT
                    || nodeType == NodeType.CONSTRAINT
                    || nodeType == NodeType.DURATION_OBSERVATION
                    || nodeType == NodeType.TIME_OBSERVATION) {
                
                // Node is comment, constraint or time observation/constraint 
                createCommentLikeNode(sgraph, nodeMap, edgeMap, node);
            }
        }

        // Check areas that have no messages in it
        if (areas != null) {
            for (SequenceArea area : areas) {
                if (area.getMessages().size() == 0) {
                    handleEmptyArea(sgraph, area);
                }
            }
        }

        // Copy the areas property to the SGraph
        sgraph.setProperty(SequenceDiagramOptions.AREAS, areas);

        // Reset graph size to zero before layouting
        sgraph.getSize().x = 0;
        sgraph.getSize().y = 0;

        return sgraph;
    }

    /**
     * Create a comment object for comments or constraints (which are handled like comments).
     * 
     * @param sgraph
     *            the Sequence Graph
     * @param nodeMap
     *            the map of node-lifeline connections
     * @param edgeMap
     *            the map of edge-message connections
     * @param node
     *            the node to create a comment object from
     */
    private void createCommentLikeNode(final SGraph sgraph, final HashMap<ElkNode, SLifeline> nodeMap,
            final HashMap<ElkEdge, SMessage> edgeMap, final ElkNode node) {

        // Get the node's type
        NodeType nodeType = node.getProperty(SequenceDiagramOptions.NODE_TYPE);

        // Create comment object
        SComment comment = new SComment();
        comment.setProperty(InternalProperties.ORIGIN, node);
        comment.setProperty(SequenceDiagramOptions.NODE_TYPE, nodeType);
        comment.setProperty(SequenceDiagramOptions.ATTACHED_ELEMENT_TYPE,
                node.getProperty(SequenceDiagramOptions.ATTACHED_ELEMENT_TYPE));
        
        // Attach connected edge to comment
        if (!node.getOutgoingEdges().isEmpty()) {
            comment.setProperty(InternalSequenceProperties.COMMENT_CONNECTION,
                    node.getOutgoingEdges().get(0));
        }

        // Copy all the entries of the list of attached elements to the comment object
        List<Object> attachedTo = node.getProperty(
                SequenceDiagramOptions.ATTACHED_OBJECTS);
        if (attachedTo != null) {
            List<SGraphElement> attTo = comment.getAttachedTo();
            for (Object att : attachedTo) {
                if (att instanceof ElkNode) {
                    attTo.add(nodeMap.get(att));
                } else if (att instanceof ElkEdge) {
                    attTo.add(edgeMap.get(att));
                }
            }
        }

        // Copy layout information
        comment.getPosition().x = node.getX();
        comment.getPosition().y = node.getY();
        comment.getSize().x = node.getWidth();
        comment.getSize().y = node.getHeight();

        // Handle time observations
        if (nodeType == NodeType.TIME_OBSERVATION) {
            comment.getSize().x = 
                    sgraph.getProperty(SequenceDiagramOptions.TIME_OBSERVATION_WIDTH);

            // Find lifeline that is next to the time observation
            SLifeline nextLifeline = null;
            double smallestDistance = Double.MAX_VALUE;
            for (SLifeline lifeline : sgraph.getLifelines()) {
                double distance = Math.abs((lifeline.getPosition().x + lifeline.getSize().x / 2)
                        - (node.getX() + node.getWidth() / 2));
                if (distance < smallestDistance) {
                    smallestDistance = distance;
                    nextLifeline = lifeline;
                }
            }

            // Find message on the calculated lifeline that is next to the time observation
            SMessage nextMessage = null;
            smallestDistance = Double.MAX_VALUE;
            for (SMessage message : nextLifeline.getMessages()) {
                ElkEdge edge = (ElkEdge) message.getProperty(InternalProperties.ORIGIN);
                ElkEdgeSection edgeSection = ElkGraphUtil.firstEdgeSection(edge, false, false);
                double distance;
                if (message.getSource() == nextLifeline) {
                    distance = Math.abs((edgeSection.getStartY())
                            - (node.getY() + node.getHeight() / 2));
                } else {
                    distance = Math.abs((edgeSection.getEndY())
                            - (node.getY() + node.getHeight() / 2));
                }
                
                if (distance < smallestDistance) {
                    smallestDistance = distance;
                    nextMessage = message;
                }
            }

            // Set both, lifeline and message of the comment to indicate that it should be drawn
            // near to the event
            comment.setLifeline(nextLifeline);
            comment.setMessage(nextMessage);
        }

        sgraph.getComments().add(comment);
    }

    /**
     * Check, where to place an empty area.
     * 
     * @param sgraph
     *            the Sequence Graph
     * @param area
     *            the area
     */
    private void handleEmptyArea(final SGraph sgraph, final SequenceArea area) {
        // Check which lifelines are involved
        for (SLifeline lifeline : sgraph.getLifelines()) {
            if (isLifelineContained(lifeline, area)) {
                area.getLifelines().add(lifeline);
            }
        }

        double lowerEnd = area.getPosition().y + area.getSize().y;
        SMessage nextMessage = null;
        double uppermostPosition = Double.MAX_VALUE;
        // Check which message is the next one below the area
        for (Object lifelineObj : area.getLifelines()) {
            SLifeline lifeline = (SLifeline) lifelineObj;
            for (SMessage message : lifeline.getIncomingMessages()) {
                Object originObj = message.getProperty(InternalProperties.ORIGIN);
                if (originObj instanceof ElkEdge) {
                    ElkEdge edge = (ElkEdge) originObj;
                    ElkEdgeSection section = ElkGraphUtil.firstEdgeSection(edge, false, false);
                    double yPos = section.getEndY();
                    if (yPos > lowerEnd && yPos < uppermostPosition) {
                        nextMessage = message;
                        uppermostPosition = yPos;
                    }
                }
            }
            for (SMessage message : lifeline.getOutgoingMessages()) {
                Object originObj = message.getProperty(InternalProperties.ORIGIN);
                if (originObj instanceof ElkEdge) {
                    ElkEdge edge = (ElkEdge) originObj;
                    ElkEdgeSection section = ElkGraphUtil.firstEdgeSection(edge, false, false);
                    double yPos = section.getStartY();
                    if (yPos > lowerEnd && yPos < uppermostPosition) {
                        nextMessage = message;
                        uppermostPosition = yPos;
                    }
                }
            }
            if (nextMessage != null) {
                area.setNextMessage(nextMessage);
            }
        }
    }

    /**
     * Walk through the node's outgoing edges and create SMessages for each of them.
     * 
     * @param sgraph
     *            the Sequence Graph
     * @param nodeMap
     *            the map of node-lifeline connections
     * @param edgeMap
     *            the map of edge-message connections
     * @param areas
     *            the list of areas
     * @param node
     *            the ElkNode to search its outgoing edges
     */
    private void createMessages(final SGraph sgraph, final HashMap<ElkNode, SLifeline> nodeMap,
            final HashMap<ElkEdge, SMessage> edgeMap, final List<SequenceArea> areas, final ElkNode node) {
        
        for (ElkEdge edge : node.getOutgoingEdges()) {
            SLifeline sourceLL = nodeMap.get(edge.getSources().get(0));
            SLifeline targetLL = nodeMap.get(edge.getTargets().get(0));

            // Lost-messages and messages to the surrounding interaction don't have a lifeline, so
            // create dummy lifeline
            if (targetLL == null) {
                SLifeline dummy = new SLifeline();
                dummy.setDummy(true);
                dummy.setGraph(sgraph);
                targetLL = dummy;
            }

            // Create message object
            SMessage message = new SMessage(sourceLL, targetLL);
            message.setProperty(InternalProperties.ORIGIN, edge);
            message.setComments(new LinkedList<SComment>());

            ElkEdgeSection section = ElkGraphUtil.firstEdgeSection(edge, false, false);
            message.setSourceYPos(section.getStartY());
            message.setTargetYPos(section.getEndY());

            // Read size of the attached label
            double maxLabelLength = 0;
            for (ElkLabel label : edge.getLabels()) {
                if (label.getWidth() > maxLabelLength) {
                    maxLabelLength = label.getWidth();
                }
            }
            message.setLabelWidth(maxLabelLength);

            // Add message to the source and the target lifeline's list of messages
            sourceLL.addMessage(message);
            targetLL.addMessage(message);

            // Put edge and message into the edge map
            edgeMap.put(edge, message);

            // Replace KEdge by its SMessage if it appears in one of the lifeline's executions. It
            // is better to do it this way than running through the list of executions since that
            // would lead to concurrent modification exceptions.
            if (sourceLL.getProperty(SequenceDiagramOptions.EXECUTIONS) != null) {
                for (SequenceExecution execution : sourceLL.getProperty(
                        SequenceDiagramOptions.EXECUTIONS)) {
                    
                    if (execution.getMessages().remove(edge)) {
                        execution.addMessage(message);
                    }
                }
            }
            
            if (targetLL.getProperty(SequenceDiagramOptions.EXECUTIONS) != null) {
                for (SequenceExecution execution : targetLL.getProperty(
                        SequenceDiagramOptions.EXECUTIONS)) {
                    
                    if (execution.getMessages().remove(edge)) {
                        execution.addMessage(message);
                    }
                }
            }

            // Append the message type of the edge to the message
            MessageType messageType = edge.getProperty(SequenceDiagramOptions.MESSAGE_TYPE);
            if (messageType == MessageType.ASYNCHRONOUS
                    || messageType == MessageType.CREATE
                    || messageType == MessageType.DELETE
                    || messageType == MessageType.SYNCHRONOUS
                    || messageType == MessageType.LOST) {
                
                message.setProperty(SequenceDiagramOptions.MESSAGE_TYPE, messageType);
            }

            // Outgoing messages to the surrounding interaction are drawn to the right and therefore
            // their target lifeline should have highest position
            if (targetLL.isDummy() && messageType != MessageType.LOST) {
                targetLL.setHorizontalSlot(sgraph.getLifelines().size() + 1);
            }

            // check if message is in any area
            if (areas != null) {
                for (SequenceArea area : areas) {
                    if (isInArea(section.getStartX(), section.getStartY(), area)
                            && isInArea(section.getEndX(), section.getEndY(),  area)) {
                        
                        area.getMessages().add(message);
                        area.getLifelines().add(message.getSource());
                        area.getLifelines().add(message.getTarget());
                        
                        for (SequenceArea subArea : area.getSubAreas()) {
                            if (isInArea(section.getStartX(), section.getStartY(), subArea)
                                    && isInArea(section.getEndX(), section.getEndY(), subArea)) {
                                
                                subArea.getMessages().add(message);
                                subArea.getLifelines().add(message.getSource());
                                subArea.getLifelines().add(message.getTarget());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Walk through incoming edges and check if there are found messages or messages that come from
     * the surrounding interaction. If so, create the corresponding SMessage.
     * 
     * @param sgraph
     *            the Sequence Graph
     * @param nodeMap
     *            the map of node-lifeline connections
     * @param edgeMap
     *            the map of edge-message connections
     * @param node
     *            the ElkNode to search its incoming edges.
     */
    private void createIncomingMessages(final SGraph sgraph, final HashMap<ElkNode, SLifeline> nodeMap,
            final HashMap<ElkEdge, SMessage> edgeMap, final ElkNode node) {
        
        for (ElkEdge edge : node.getIncomingEdges()) {
            ElkEdgeSection section = ElkGraphUtil.firstEdgeSection(edge, false, false);

            SLifeline sourceLL = nodeMap.get(edge.getSources().get(0));
            if (sourceLL == null) {
                // TODO consider connections to comments and constraints!
                // Create dummy lifeline as source since the message has no source lifeline
                SLifeline dummy = new SLifeline();
                dummy.setDummy(true);
                dummy.setGraph(sgraph);
                sourceLL = dummy;
                SLifeline targetLL = nodeMap.get(edge.getTargets().get(0));

                // Create message object
                SMessage message = new SMessage(sourceLL, targetLL);
                message.setProperty(InternalProperties.ORIGIN, edge);
                message.setComments(new LinkedList<SComment>());
                message.setTargetYPos(section.getEndY());

                // Add the message to the source and target lifeline's list of messages
                sourceLL.addMessage(message);
                targetLL.addMessage(message);

                // Put edge and message into the edge map
                edgeMap.put(edge, message);

                // Append the message type of the edge to the message
                MessageType messageType = edge.getProperty(SequenceDiagramOptions.MESSAGE_TYPE);
                if (messageType == MessageType.FOUND) {
                    message.setProperty(SequenceDiagramOptions.MESSAGE_TYPE, MessageType.FOUND);
                } else {
                    // Since incoming messages come from the left side of the surrounding
                    // interaction, give its dummy lifeline position -1
                    sourceLL.setHorizontalSlot(-1);

                    if (messageType == MessageType.ASYNCHRONOUS
                            || messageType == MessageType.CREATE
                            || messageType == MessageType.DELETE
                            || messageType == MessageType.SYNCHRONOUS) {
                        
                        message.setProperty(SequenceDiagramOptions.MESSAGE_TYPE, messageType);
                    }
                }

                // replace KEdge by its SMessage if it appears in one of the lifeline's
                // executions
                if (sourceLL.getProperty(SequenceDiagramOptions.EXECUTIONS) != null) {
                    for (SequenceExecution execution : sourceLL.getProperty(
                            SequenceDiagramOptions.EXECUTIONS)) {
                        
                        if (execution.getMessages().remove(edge)) {
                            execution.addMessage(message);
                        }
                    }
                }
                
                if (targetLL.getProperty(SequenceDiagramOptions.EXECUTIONS) != null) {
                    for (SequenceExecution execution : targetLL.getProperty(
                            SequenceDiagramOptions.EXECUTIONS)) {
                        
                        if (execution.getMessages().remove(edge)) {
                            execution.addMessage(message);
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates the SLifeline for the ElkNode and copies its properties.
     * 
     * @param sgraph
     *            the Sequence Graph
     * @param nodeMap
     *            the map of node-lifeline connections
     * @param node
     *            the ElkNode to create a lifeline for
     */
    private void createLifeline(final SGraph sgraph, final HashMap<ElkNode, SLifeline> nodeMap,
            final ElkNode node) {
        
        if (node.getProperty(SequenceDiagramOptions.NODE_TYPE) == NodeType.LIFELINE) {
            // Node is lifeline
            SLifeline lifeline = new SLifeline();
            if (node.getLabels().size() > 0) {
                lifeline.setName(node.getLabels().get(0).getText());
            }
            lifeline.setProperty(InternalProperties.ORIGIN, node);
            nodeMap.put(node, lifeline);
            sgraph.addLifeline(lifeline);

            // Copy layout information to lifeline
            lifeline.getPosition().x = node.getX();
            lifeline.getPosition().y = node.getY();
            lifeline.getSize().x = node.getWidth();
            lifeline.getSize().y = node.getHeight();

            // Copy executions to lifeline
            List<SequenceExecution> executions = node.getProperty(
                    SequenceDiagramOptions.EXECUTIONS);
            lifeline.setProperty(SequenceDiagramOptions.EXECUTIONS, executions);

            lifeline.setComments(new LinkedList<SComment>());

            // If the DESTRUCTION property is set, simply copy it over
            ElkNode destructionNode = node.getProperty(SequenceDiagramOptions.DESTRUCTION_NODE);
            if (destructionNode != null) {
                lifeline.setProperty(SequenceDiagramOptions.DESTRUCTION_NODE, destructionNode);
            }
        }
    }

    /**
     * Checks, if a given lifeline's center is horizontally within an area.
     * 
     * @param lifeline
     *            the lifeline
     * @param area
     *            the area
     * @return true, if the lifeline is inside the area, false otherwise
     */
    private boolean isLifelineContained(final SLifeline lifeline, final SequenceArea area) {
        double lifelineCenter = lifeline.getPosition().x + lifeline.getSize().x / 2;
        double leftEnd = area.getPosition().x;
        double rightEnd = area.getPosition().x + area.getSize().x;

        return (lifelineCenter >= leftEnd && lifelineCenter <= rightEnd);
    }

    /**
     * Checks if a given KPoint is inside the borders of a given SequenceArea.
     * 
     * @param point
     *            the KPoint
     * @param area
     *            the SequenceArea
     * @return true if the point is inside the area
     */
    private boolean isInArea(final double x, final double y, final SequenceArea area) {
        if (x < area.getPosition().x) {
            return false;
        }
        
        if (x > area.getPosition().x + area.getSize().x) {
            return false;
        }
        
        if (y < area.getPosition().y) {
            return false;
        }
        
        if (y > area.getPosition().y + area.getSize().y) {
            return false;
        }
        
        return true;
    }
    
    
    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // LGraph Creation

    /**
     * Builds a layered graph that contains every message as a node. Edges are representations of
     * the relative order of the messages.
     * 
     * @param sgraph
     *            the given SGraph
     * @param progressMonitor
     *            the progress monitor
     * @return the layeredGraph
     */
    private LGraph createLayeredGraph(final SGraph sgraph) {
        LGraph lgraph = new LGraph();

        // Build a node for every message.
        int i = 0;
        for (SLifeline lifeline : sgraph.getLifelines()) {
            for (SMessage message : lifeline.getOutgoingMessages()) {
                LNode node = new LNode(lgraph);
                node.getLabels().add(new LLabel("Node" + i++));
                node.setProperty(InternalProperties.ORIGIN, message);
                message.setProperty(InternalSequenceProperties.LAYERED_NODE, node);
                lgraph.getLayerlessNodes().add(node);
            }
            // Handle found messages (they have no source lifeline)
            for (SMessage message : lifeline.getIncomingMessages()) {
                if (message.getSource().isDummy()) {
                    LNode node = new LNode(lgraph);
                    node.getLabels().add(new LLabel("Node" + i++));
                    node.setProperty(InternalProperties.ORIGIN, message);
                    message.setProperty(InternalSequenceProperties.LAYERED_NODE, node);
                    lgraph.getLayerlessNodes().add(node);
                }
            }
        }

        // Add an edge for every neighbored pair of messages at every lifeline
        // indicating the relative order of the messages.
        for (SLifeline lifeline : sgraph.getLifelines()) {
            List<SMessage> messages = lifeline.getMessages();
            for (int j = 1; j < messages.size(); j++) {
                // Add an edge from the node belonging to message j-1 to the node belonging to
                // message j
                LNode sourceNode = messages.get(j - 1).getProperty(
                        InternalSequenceProperties.LAYERED_NODE);
                LNode targetNode = messages.get(j).getProperty(
                        InternalSequenceProperties.LAYERED_NODE);
                
                if (sourceNode != targetNode) {
                    LPort sourcePort = new LPort();
                    sourcePort.setNode(sourceNode);
                    
                    LPort targetPort = new LPort();
                    targetPort.setNode(targetNode);
                    
                    LEdge edge = new LEdge();
                    
                    edge.setSource(sourcePort);
                    edge.setTarget(targetPort);
                    
                    edge.setProperty(InternalSequenceProperties.BELONGS_TO_LIFELINE, lifeline);
                }
            }
        }

        return lgraph;
    }
    
}
