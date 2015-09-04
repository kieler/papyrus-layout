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

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import de.cau.cs.kieler.core.alg.IKielerProgressMonitor;
import de.cau.cs.kieler.core.kgraph.KEdge;
import de.cau.cs.kieler.core.kgraph.KLabel;
import de.cau.cs.kieler.core.kgraph.KNode;
import de.cau.cs.kieler.kiml.klayoutdata.KEdgeLayout;
import de.cau.cs.kieler.kiml.klayoutdata.KLayoutData;
import de.cau.cs.kieler.kiml.klayoutdata.KShapeLayout;
import de.cau.cs.kieler.klay.layered.graph.LEdge;
import de.cau.cs.kieler.klay.layered.graph.LGraph;
import de.cau.cs.kieler.klay.layered.graph.LLabel;
import de.cau.cs.kieler.klay.layered.graph.LNode;
import de.cau.cs.kieler.klay.layered.graph.LPort;
import de.cau.cs.kieler.klay.layered.properties.InternalProperties;
import de.cau.cs.kieler.papyrus.sequence.ISequenceLayoutProcessor;
import de.cau.cs.kieler.papyrus.sequence.LayoutContext;
import de.cau.cs.kieler.papyrus.sequence.graph.SComment;
import de.cau.cs.kieler.papyrus.sequence.graph.SGraph;
import de.cau.cs.kieler.papyrus.sequence.graph.SGraphElement;
import de.cau.cs.kieler.papyrus.sequence.graph.SLifeline;
import de.cau.cs.kieler.papyrus.sequence.graph.SMessage;
import de.cau.cs.kieler.papyrus.sequence.properties.MessageType;
import de.cau.cs.kieler.papyrus.sequence.properties.NodeType;
import de.cau.cs.kieler.papyrus.sequence.properties.SequenceArea;
import de.cau.cs.kieler.papyrus.sequence.properties.SequenceDiagramProperties;
import de.cau.cs.kieler.papyrus.sequence.properties.SequenceExecution;
import de.cau.cs.kieler.papyrus.sequence.properties.SequenceExecutionType;

/**
 * Turns the KGraph of a layout context into an SGraph and an LGraph.
 * 
 * @author grh
 * @kieler.design 2012-11-20 cds, msp
 * @kieler.rating proposed yellow grh
 */
public final class KGraphImporter implements ISequenceLayoutProcessor {
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void process(final LayoutContext context, final IKielerProgressMonitor progressMonitor) {
        progressMonitor.begin("Graph import", 1);
        
        context.sgraph = importGraph(context.kgraph);
        context.lgraph = createLayeredGraph(context.sgraph);
        
        progressMonitor.done();
    }
    
    
    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // SGraph Creation
    
    /** A map from KNodes in the layout graph to the lifelines created for them. */
    private Map<KNode, SLifeline> lifelineMap = Maps.newHashMap();
    /** A map from KEdges in the layout graph to messages created for them in the SGraph. */
    private Map<KEdge, SMessage> messageMap = Maps.newHashMap();
    /** A map from element IDs to the corresponding executions. */
    private Map<Integer, SequenceExecution> executionIdMap = Maps.newHashMap();
    /** A map from element IDs to the corresponding sequence area. */
    private Map<Integer, SequenceArea> areaIdMap = Maps.newHashMap();
    
    
    /**
     * Builds a PGraph out of a given KGraph by associating every KNode to a PLifeline and every
     * KEdge to a PMessage.
     * 
     * @param topNode
     *            the surrounding interaction node.
     * @return the built SGraph
     */
    private SGraph importGraph(final KNode topNode) {
        // Create a graph object
        SGraph sgraph = new SGraph();
        
        // Create... well, as it says: sequence areas...
        createSequenceAreas(topNode, sgraph);

        // Create lifelines
        for (KNode node : topNode.getChildren()) {
            NodeType nodeType = node.getData(KShapeLayout.class).getProperty(
                    SequenceDiagramProperties.NODE_TYPE);
            
            if (nodeType == NodeType.LIFELINE) {
                createLifeline(sgraph, node);
            }
        }

        // Walk through lifelines (create their messages) and comments
        for (KNode node : topNode.getChildren()) {
            NodeType nodeType = node.getData(KShapeLayout.class).getProperty(
                    SequenceDiagramProperties.NODE_TYPE);
            
            if (nodeType == NodeType.LIFELINE) {
                // Create SMessages for each of the outgoing edges
                createMessages(sgraph, node);

                // Handle found messages (incoming messages)
                createIncomingMessages(sgraph, node);
            } else if (nodeType == NodeType.COMMENT
                    || nodeType == NodeType.CONSTRAINT
                    || nodeType == NodeType.DURATION_OBSERVATION
                    || nodeType == NodeType.TIME_OBSERVATION) {
                
                createCommentLikeNode(sgraph, node);
            }
        }

        // Reset graph size to zero before layouting
        sgraph.getSize().x = 0;
        sgraph.getSize().y = 0;
        
        // Clear maps
        lifelineMap.clear();
        messageMap.clear();
        executionIdMap.clear();

        return sgraph;
    }
    
    
    //////////////////////////////////////////////////////////////
    // Areas
    
    /**
     * Creates all sequence areas for the given graph.
     * 
     * @param topNode
     *            the surrounding interaction node.
     * @param sgraph
     *            the Sequence Graph
     */
    private void createSequenceAreas(final KNode topNode, final SGraph sgraph) {
        // Initialize the list of areas (fragments and such)
        List<SequenceArea> areas = Lists.newArrayList();
        
        // Find nodes that represent areas
        for (KNode node : topNode.getChildren()) {
            KLayoutData layoutData = node.getData(KLayoutData.class);
            NodeType nodeType = layoutData.getProperty(SequenceDiagramProperties.NODE_TYPE);
            
            if (nodeType == NodeType.COMBINED_FRAGMENT || nodeType == NodeType.INTERACTION_USE) {
                SequenceArea area = new SequenceArea(node);
                areas.add(area);
                areaIdMap.put(layoutData.getProperty(SequenceDiagramProperties.ELEMENT_ID), area);
            }
        }
        
        // Now that all areas have been created, find nodes that represent nested areas
        for (KNode node : topNode.getChildren()) {
            KLayoutData layoutData = node.getData(KLayoutData.class);
            NodeType nodeType = layoutData.getProperty(SequenceDiagramProperties.NODE_TYPE);
            
            if (nodeType == NodeType.COMBINED_FRAGMENT || nodeType == NodeType.INTERACTION_USE) {
                int parentId = layoutData.getProperty(SequenceDiagramProperties.PARENT_AREA_ID);
                
                if (parentId != -1) {
                    SequenceArea parentArea = areaIdMap.get(parentId);
                    SequenceArea childArea = areaIdMap.get(
                            layoutData.getProperty(SequenceDiagramProperties.ELEMENT_ID));
                    
                    if (parentArea != null && childArea != null) {
                        parentArea.getContainedAreas().add(childArea);
                    }
                    // TODO Possibly throw an exception in the else case
                }
            }
        }
        
        // Remember the areas
        sgraph.setProperty(SequenceDiagramProperties.AREAS, areas);
    }


    //////////////////////////////////////////////////////////////
    // Lifelines

    /**
     * Creates the SLifeline for the given KNode, sets up its properties, and looks through its children
     * to setup destructions and executions.
     * 
     * @param sgraph
     *            the Sequence Graph
     * @param klifeline
     *            the KNode to create a lifeline for
     */
    private void createLifeline(final SGraph sgraph, final KNode klifeline) {
        KShapeLayout klayout = klifeline.getData(KShapeLayout.class);
        
        assert klayout.getProperty(SequenceDiagramProperties.NODE_TYPE) == NodeType.LIFELINE;
        
        // Node is lifeline
        SLifeline slifeline = new SLifeline();
        if (klifeline.getLabels().size() > 0) {
            slifeline.setName(klifeline.getLabels().get(0).getText());
        }
        
        slifeline.setProperty(InternalProperties.ORIGIN, klifeline);
        lifelineMap.put(klifeline, slifeline);
        sgraph.addLifeline(slifeline);

        // Copy layout information to lifeline
        slifeline.getPosition().x = klayout.getXpos();
        slifeline.getPosition().y = klayout.getYpos();
        slifeline.getSize().x = klayout.getWidth();
        slifeline.getSize().y = klayout.getHeight();
        
        // Iterate through the lifeline's children to collect destructions and executions
        List<SequenceExecution> executions = Lists.newArrayList();
        
        for (KNode kchild : klifeline.getChildren()) {
            KShapeLayout kchildLayout = kchild.getData(KShapeLayout.class);
            NodeType kchildNodeType = kchildLayout.getProperty(SequenceDiagramProperties.NODE_TYPE);
            
            if (kchildNodeType.isExecutionType()) {
                // Create a new sequence execution for this thing
                SequenceExecution execution = new SequenceExecution(kchild);
                execution.setType(SequenceExecutionType.fromNodeType(kchildNodeType));
                executions.add(execution);
                executionIdMap.put(
                        kchildLayout.getProperty(SequenceDiagramProperties.ELEMENT_ID), execution);
            } else if (kchildNodeType == NodeType.DESTRUCTION_EVENT) {
                slifeline.setProperty(SequenceDiagramProperties.DESTRUCTION, kchild);
            }
        }
        
        slifeline.setProperty(SequenceDiagramProperties.EXECUTIONS, executions);
        
        // Check if the lifeline has any empty areas
        List<Integer> areaIds = klayout.getProperty(SequenceDiagramProperties.AREA_IDS);
        for (Integer areaId : areaIds) {
            SequenceArea area = areaIdMap.get(areaId);
            if (area != null) {
                area.getLifelines().add(slifeline);
            }
        }
    }


    //////////////////////////////////////////////////////////////
    // Messages

    /**
     * Walk through the lifeline's outgoing edges and create SMessages for each of them.
     * 
     * @param sgraph
     *            the Sequence Graph
     * @param klifeline
     *            the KNode to search its outgoing edges
     */
    private void createMessages(final SGraph sgraph, final KNode klifeline) {
        for (KEdge kedge : klifeline.getOutgoingEdges()) {
            SLifeline sourceLL = lifelineMap.get(kedge.getSource());
            SLifeline targetLL = lifelineMap.get(kedge.getTarget());

            // Lost-messages and messages to the surrounding interaction don't have a lifeline, so
            // create dummy lifeline
            if (targetLL == null) {
                SLifeline sdummy = new SLifeline();
                sdummy.setDummy(true);
                sdummy.setGraph(sgraph);
                targetLL = sdummy;
            }

            // Create message object
            SMessage smessage = new SMessage(sourceLL, targetLL);
            smessage.setProperty(InternalProperties.ORIGIN, kedge);

            KEdgeLayout kedgelayout = kedge.getData(KEdgeLayout.class);
            smessage.setSourceYPos(kedgelayout.getSourcePoint().getY());
            smessage.setTargetYPos(kedgelayout.getTargetPoint().getY());

            // Read size of the attached labels
            double maxLabelLength = 0;
            for (KLabel klabel : kedge.getLabels()) {
                KShapeLayout klabelLayout = klabel.getData(KShapeLayout.class);
                if (klabelLayout.getWidth() > maxLabelLength) {
                    maxLabelLength = klabelLayout.getWidth();
                }
            }
            smessage.setLabelWidth(maxLabelLength);

            // Add message to the source and the target lifeline's list of messages
            sourceLL.addMessage(smessage);
            targetLL.addMessage(smessage);

            // Put edge and message into the edge map
            messageMap.put(kedge, smessage);
            
            // Check if the edge connects to executions
            SequenceExecution sourceExecution = executionIdMap.get(
                    kedgelayout.getProperty(SequenceDiagramProperties.SOURCE_EXECUTION_ID));
            if (sourceExecution != null) {
                sourceExecution.addMessage(smessage);
            }
            
            SequenceExecution targetExecution = executionIdMap.get(
                    kedgelayout.getProperty(SequenceDiagramProperties.TARGET_EXECUTION_ID));
            if (targetExecution != null) {
                targetExecution.addMessage(smessage);
            }

            // Append the message type of the edge to the message
            MessageType messageType = kedgelayout.getProperty(SequenceDiagramProperties.MESSAGE_TYPE);
            if (messageType == MessageType.ASYNCHRONOUS
                    || messageType == MessageType.CREATE
                    || messageType == MessageType.DELETE
                    || messageType == MessageType.SYNCHRONOUS
                    || messageType == MessageType.LOST) {
                
                smessage.setProperty(SequenceDiagramProperties.MESSAGE_TYPE, messageType);
            }

            // Outgoing messages to the surrounding interaction are drawn to the right and therefore
            // their target lifeline should have highest position
            if (targetLL.isDummy() && messageType != MessageType.LOST) {
                targetLL.setHorizontalSlot(sgraph.getLifelines().size() + 1);
            }

            // Check if message is in any area
            for (Integer areaId : kedgelayout.getProperty(SequenceDiagramProperties.AREA_IDS)) {
                SequenceArea area = areaIdMap.get(areaId);
                if (area != null) {
                    area.getMessages().add(smessage);
                    area.getLifelines().add(smessage.getSource());
                    area.getLifelines().add(smessage.getTarget());
                }
                // TODO Possibly throw an exception in the else case
            }
            
            // Check if this message has an empty area that is to be placed directly above it
            int upperEmptyAreaId = kedgelayout.getProperty(
                    SequenceDiagramProperties.UPPER_EMPTY_AREA_ID);
            SequenceArea upperArea = areaIdMap.get(upperEmptyAreaId);
            if (upperArea != null) {
                upperArea.setNextMessage(smessage);
            }
        }
    }

    /**
     * Walk through incoming edges of the given lifeline and check if there are found messages
     * ormessages that come from the surrounding interaction. If so, create the corresponding
     * SMessage.
     * 
     * @param sgraph
     *            the Sequence Graph
     * @param klifeline
     *            the KNode to search its incoming edges.
     */
    private void createIncomingMessages(final SGraph sgraph, final KNode klifeline) {
        for (KEdge kedge : klifeline.getIncomingEdges()) {
            KEdgeLayout kedgelayout = kedge.getData(KEdgeLayout.class);

            SLifeline sourceLL = lifelineMap.get(kedge.getSource());
            
            // We are only interested in messages that don't come from a lifeline
            if (sourceLL != null) {
                continue;
            }
            
            // TODO consider connections to comments and constraints!
            
            // Create dummy lifeline as source since the message has no source lifeline
            // TODO We could think about using a single dummy lifeline for all found messages
            SLifeline sdummy = new SLifeline();
            sdummy.setDummy(true);
            sdummy.setGraph(sgraph);
            sourceLL = sdummy;
            
            SLifeline targetLL = lifelineMap.get(kedge.getTarget());

            // Create message object
            SMessage smessage = new SMessage(sourceLL, targetLL);
            smessage.setProperty(InternalProperties.ORIGIN, kedge);
            smessage.setTargetYPos(kedgelayout.getTargetPoint().getY());

            // Add the message to the source and target lifeline's list of messages
            sourceLL.addMessage(smessage);
            targetLL.addMessage(smessage);

            // Put edge and message into the edge map
            messageMap.put(kedge, smessage);

            // Append the message type of the edge to the message
            MessageType messageType = kedgelayout.getProperty(SequenceDiagramProperties.MESSAGE_TYPE);
            if (messageType == MessageType.FOUND) {
                smessage.setProperty(SequenceDiagramProperties.MESSAGE_TYPE, MessageType.FOUND);
            } else {
                // Since incoming messages come from the left side of the surrounding
                // interaction, give its dummy lifeline position -1
                sourceLL.setHorizontalSlot(-1);

                if (messageType == MessageType.ASYNCHRONOUS
                        || messageType == MessageType.CREATE
                        || messageType == MessageType.DELETE
                        || messageType == MessageType.SYNCHRONOUS) {
                    
                    smessage.setProperty(SequenceDiagramProperties.MESSAGE_TYPE, messageType);
                }
            }

            // Check if the message connects to a target execution
            SequenceExecution targetExecution = executionIdMap.get(
                    kedgelayout.getProperty(SequenceDiagramProperties.SOURCE_EXECUTION_ID));
            if (targetExecution != null) {
                targetExecution.addMessage(smessage);
            }
        }
    }
    
    
    //////////////////////////////////////////////////////////////
    // Comment-like Nodes

    /**
     * Create a comment object for comments or constraints (which are handled like comments).
     * 
     * @param sgraph
     *            the Sequence Graph
     * @param node
     *            the node to create a comment object from
     */
    private void createCommentLikeNode(final SGraph sgraph, final KNode node) {
        KShapeLayout commentLayout = node.getData(KShapeLayout.class);

        // Get the node's type
        NodeType nodeType = commentLayout.getProperty(SequenceDiagramProperties.NODE_TYPE);

        // Create comment object
        SComment comment = new SComment();
        comment.setProperty(InternalProperties.ORIGIN, node);
        comment.setProperty(SequenceDiagramProperties.NODE_TYPE, nodeType);
        comment.setProperty(SequenceDiagramProperties.ATTACHED_ELEMENT_TYPE,
                commentLayout.getProperty(SequenceDiagramProperties.ATTACHED_ELEMENT_TYPE));
        
        // Attach connected edge to comment
        if (!node.getOutgoingEdges().isEmpty()) {
            comment.setProperty(SequenceDiagramProperties.COMMENT_CONNECTION,
                    node.getOutgoingEdges().get(0));
        }

        // Copy all the entries of the list of attached elements to the comment object
        List<Object> attachedTo = commentLayout.getProperty(SequenceDiagramProperties.ATTACHED_TO);
        if (attachedTo != null) {
            List<SGraphElement> attTo = comment.getAttachedTo();
            for (Object att : attachedTo) {
                if (att instanceof KNode) {
                    attTo.add(lifelineMap.get(att));
                } else if (att instanceof KEdge) {
                    attTo.add(messageMap.get(att));
                }
            }
        }

        // Copy layout information
        comment.getPosition().x = commentLayout.getXpos();
        comment.getPosition().y = commentLayout.getYpos();
        comment.getSize().x = commentLayout.getWidth();
        comment.getSize().y = commentLayout.getHeight();

        // Handle time observations
        if (nodeType == NodeType.TIME_OBSERVATION) {
            comment.getSize().x = 
                    sgraph.getProperty(SequenceDiagramProperties.TIME_OBSERVATION_WIDTH).doubleValue();

            // Find lifeline that is next to the time observation
            SLifeline nextLifeline = null;
            double smallestDistance = Double.MAX_VALUE;
            for (SLifeline lifeline : sgraph.getLifelines()) {
                double distance = Math.abs((lifeline.getPosition().x + lifeline.getSize().x / 2)
                        - (commentLayout.getXpos() + commentLayout.getWidth() / 2));
                if (distance < smallestDistance) {
                    smallestDistance = distance;
                    nextLifeline = lifeline;
                }
            }

            // Find message on the calculated lifeline that is next to the time observation
            SMessage nextMessage = null;
            smallestDistance = Double.MAX_VALUE;
            for (SMessage message : nextLifeline.getMessages()) {
                KEdge edge = (KEdge) message.getProperty(InternalProperties.ORIGIN);
                KEdgeLayout edgeLayout = edge.getData(KEdgeLayout.class);
                double distance;
                if (message.getSource() == nextLifeline) {
                    distance = Math.abs((edgeLayout.getSourcePoint().getY())
                            - (commentLayout.getYpos() + commentLayout.getHeight() / 2));
                } else {
                    distance = Math.abs((edgeLayout.getTargetPoint().getY())
                            - (commentLayout.getYpos() + commentLayout.getHeight() / 2));
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
                message.setProperty(SequenceDiagramProperties.LAYERED_NODE, node);
                lgraph.getLayerlessNodes().add(node);
            }
            // Handle found messages (they have no source lifeline)
            for (SMessage message : lifeline.getIncomingMessages()) {
                if (message.getSource().isDummy()) {
                    LNode node = new LNode(lgraph);
                    node.getLabels().add(new LLabel("Node" + i++));
                    node.setProperty(InternalProperties.ORIGIN, message);
                    message.setProperty(SequenceDiagramProperties.LAYERED_NODE, node);
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
                        SequenceDiagramProperties.LAYERED_NODE);
                LNode targetNode = messages.get(j).getProperty(
                        SequenceDiagramProperties.LAYERED_NODE);
                
                if (sourceNode != targetNode) {
                    LPort sourcePort = new LPort();
                    sourcePort.setNode(sourceNode);
                    
                    LPort targetPort = new LPort();
                    targetPort.setNode(targetNode);
                    
                    LEdge edge = new LEdge();
                    
                    edge.setSource(sourcePort);
                    edge.setTarget(targetPort);
                    
                    edge.setProperty(SequenceDiagramProperties.BELONGS_TO_LIFELINE, lifeline);
                }
            }
        }

        return lgraph;
    }
    
}
