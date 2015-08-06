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
package de.cau.cs.kieler.papyrus.sequence.p6export;

import java.util.List;

import de.cau.cs.kieler.core.alg.IKielerProgressMonitor;
import de.cau.cs.kieler.core.kgraph.KEdge;
import de.cau.cs.kieler.core.kgraph.KLabel;
import de.cau.cs.kieler.core.kgraph.KNode;
import de.cau.cs.kieler.kiml.klayoutdata.KEdgeLayout;
import de.cau.cs.kieler.kiml.klayoutdata.KLayoutDataFactory;
import de.cau.cs.kieler.kiml.klayoutdata.KPoint;
import de.cau.cs.kieler.kiml.klayoutdata.KShapeLayout;
import de.cau.cs.kieler.klay.layered.properties.InternalProperties;
import de.cau.cs.kieler.papyrus.sequence.ISequenceLayoutProcessor;
import de.cau.cs.kieler.papyrus.sequence.LayoutContext;
import de.cau.cs.kieler.papyrus.sequence.SequenceLayoutConstants;
import de.cau.cs.kieler.papyrus.sequence.graph.SComment;
import de.cau.cs.kieler.papyrus.sequence.graph.SGraph;
import de.cau.cs.kieler.papyrus.sequence.graph.SLifeline;
import de.cau.cs.kieler.papyrus.sequence.graph.SMessage;
import de.cau.cs.kieler.papyrus.sequence.properties.LabelAlignment;
import de.cau.cs.kieler.papyrus.sequence.properties.MessageType;
import de.cau.cs.kieler.papyrus.sequence.properties.SequenceDiagramProperties;
import de.cau.cs.kieler.papyrus.sequence.properties.SequenceExecution;
import de.cau.cs.kieler.papyrus.sequence.properties.SequenceExecution.SequenceExecutionType;

/**
 * Applies the layout results back to the original KGraph using the standard KGraph coordinate system.
 * 
 * @author cds
 */
public final class KGraphExporter implements ISequenceLayoutProcessor {

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(final LayoutContext context, final IKielerProgressMonitor progressMonitor) {
        progressMonitor.begin("Applying Layout Results", 1);
        
        // The height of the diagram (the surrounding interaction)
        double diagramHeight = context.sgraph.getSize().y + context.messageSpacing
                + context.lifelineHeader + context.lifelineYPos + context.borderSpacing;
        
        // Set position for lifelines/nodes
        for (SLifeline lifeline : context.lifelineOrder) {
            // Dummy lifelines don't need any layout
            if (lifeline.isDummy()) {
                continue;
            }

            KNode node = (KNode) lifeline.getProperty(InternalProperties.ORIGIN);
            KShapeLayout nodeLayout = node.getData(KShapeLayout.class);

            // Handle messages of the lifeline and their labels
            applyMessageCoordinates(context, lifeline);

            // Apply execution coordinates and adjust positions of messages attached to these executions
            applyExecutionCoordinates(context, lifeline);

            // Set position and height for the lifeline.
            nodeLayout.setYpos((float) lifeline.getPosition().y);
            nodeLayout.setXpos((float) lifeline.getPosition().x);
            nodeLayout.setHeight((float) lifeline.getSize().y);

            // Place destruction if existing
            KNode destruction = lifeline.getProperty(SequenceDiagramProperties.DESTRUCTION);
            if (destruction != null) {
                KShapeLayout destructLayout = destruction.getData(KShapeLayout.class);
                double destructionXPos = nodeLayout.getWidth() / 2 - destructLayout.getWidth() / 2;
                double destructionYPos = nodeLayout.getHeight() - destructLayout.getHeight();
                destructLayout.setPos((float) destructionXPos, (float) destructionYPos);
            }
        }

        // Place all comments
        placeComments(context.sgraph);

        // Set position and size of surrounding interaction
        KShapeLayout parentLayout = context.kgraph.getData(KShapeLayout.class);
        parentLayout.setWidth((float) context.sgraph.getSize().x);
        parentLayout.setHeight((float) diagramHeight);
        parentLayout.setPos((float) context.borderSpacing, (float) context.borderSpacing);
        
        progressMonitor.done();
    }
    
    
    /////////////////////////////////////////////////////////////////////////////////////////////////
    // Messages

    /**
     * Apply the calculated coordinates of the messages that are connected to the given lifeline. If
     * there are any incoming create or destroy messages, the lifeline's height and y coordinate may be
     * changed here as well.
     * 
     * @param context
     *            the layout context that contains all relevant information for the current layout run.
     * @param lifeline
     *            the lifeline whose messages are handled
     */
    private void applyMessageCoordinates(final LayoutContext context, final SLifeline lifeline) {
        
        for (SMessage message : lifeline.getOutgoingMessages()) {
            applyOutgoingMessageCoordinates(lifeline, message, context);
        }

        for (SMessage message : lifeline.getIncomingMessages()) {
            applyIncomingMessageCoordinates(lifeline, message, context);
        }
    }

    /**
     * Applies the source coordinates of the given message starting at the given lifeline.
     * 
     * @param lifeline the lifeline the message starts at.
     * @param message the message whose coordinates to apply.
     * @param context layout context of the current layout run.
     */
    private void applyOutgoingMessageCoordinates(final SLifeline lifeline, final SMessage message,
            final LayoutContext context) {
        
        assert lifeline == message.getSource();
        
        KEdge edge = (KEdge) message.getProperty(InternalProperties.ORIGIN);
        KEdgeLayout edgeLayout = edge.getData(KEdgeLayout.class);

        MessageType messageType = message.getProperty(SequenceDiagramProperties.MESSAGE_TYPE);
        
        // Compute the horizontal center of the lifeline to be used later
        double llCenter = lifeline.getPosition().x + lifeline.getSize().x / 2;
        
        // Clear the bend points of all edges (this is safe to do here since we will only be adding
        // bend points for self loops, which we encounter first as outgoing messages, so we're not
        // clearing bend points set by the incoming message handling)
        edgeLayout.getBendPoints().clear();
        
        // Apply source point position
        KPoint sourcePoint = edgeLayout.getSourcePoint();
        sourcePoint.setY((float) message.getSourceYPos());
        sourcePoint.setX((float) llCenter);

        // Check if the message connects to executions
        List<SequenceExecution> executions = lifeline.getProperty(SequenceDiagramProperties.EXECUTIONS);
        if (executions != null) {
            for (SequenceExecution execution : executions) {
                if (execution.getMessages().contains(message)) {
                    // Adjust the execution's vertical extend
                    double sourceYPos = message.getSourceYPos();
                    if (execution.getPosition().y == 0) {
                        // If this is the first message to encounter this execution, initialize the
                        // execution's y coordinate and height
                        execution.getPosition().y = sourceYPos;
                        execution.getSize().y = 0;
                    } else {
                        // The execution already has a position and size; adjust.
                        if (sourceYPos < execution.getPosition().y) {
                            if (message.getSource() != message.getTarget()) {
                                double delta = execution.getPosition().y - sourceYPos;
                                execution.getPosition().y = sourceYPos;
                                execution.getSize().y += delta;
                            }
                        } else if (sourceYPos > execution.getPosition().y + execution.getSize().y) {
                            execution.getSize().y = sourceYPos - execution.getPosition().y;
                        }
                    }
                }
            }
        }

        // Lost messages end between their source and the next lifeline
        if (messageType == MessageType.LOST) {
            edgeLayout.getTargetPoint().setX((float)
                    (lifeline.getPosition().x + lifeline.getSize().x + context.lifelineSpacing / 2));
        }
        
        // Specify bend points for self loops
        if (message.getSource() == message.getTarget()) {
            KPoint bendPoint = KLayoutDataFactory.eINSTANCE.createKPoint();
            bendPoint.setX((float) (llCenter + context.messageSpacing / 2));
            bendPoint.setY(edgeLayout.getSourcePoint().getY());
            edgeLayout.getBendPoints().add(bendPoint);
        }

        // Walk through the labels and adjust their position
        placeLabels(context, message, edge);
    }

    /**
     * Applies the target coordinates of the given message ending at the given lifeline.
     * 
     * @param lifeline the lifeline the message ends at.
     * @param message the message whose coordinates to apply.
     * @param context layout context of the current layout run.
     */
    private void applyIncomingMessageCoordinates(final SLifeline lifeline, final SMessage message,
            final LayoutContext context) {
        
        assert lifeline == message.getTarget();
        
        KEdge edge = (KEdge) message.getProperty(InternalProperties.ORIGIN);
        KEdgeLayout edgeLayout = edge.getData(KEdgeLayout.class);

        MessageType messageType = message.getProperty(SequenceDiagramProperties.MESSAGE_TYPE);
        
        // Compute the horizontal center of the lifeline to be used later
        double llCenter = lifeline.getPosition().x + lifeline.getSize().x / 2;
        
        // Apply target point position
        KPoint targetPoint = edgeLayout.getTargetPoint();
        targetPoint.setY((float) message.getTargetYPos());
        targetPoint.setX((float) llCenter);
        
        if (messageType == MessageType.CREATE) {
            // Set lifeline's yPos to the yPos of the create-message and modify lifeline height
            // accordingly
            double delta = message.getTargetYPos() - context.lifelineHeader / 2
                    - lifeline.getPosition().y;
            
            lifeline.getPosition().y += delta;
            lifeline.getSize().y -= delta;
            
            // Reset x-position of create message because it leads to the header and not the line
            targetPoint.setX((float) lifeline.getPosition().x);
        } else if (messageType == MessageType.DELETE) {
            // If the lifeline extends beyond the message target position, shorten the lifeline
            if (lifeline.getPosition().y + lifeline.getSize().y > targetPoint.getY()) {
                lifeline.getSize().y = targetPoint.getY() - lifeline.getPosition().y;
            }
        }

        // Check if the message connects to executions
        List<SequenceExecution> executions = lifeline.getProperty(SequenceDiagramProperties.EXECUTIONS);
        if (executions != null) {
            for (SequenceExecution execution : executions) {
                if (execution.getMessages().contains(message)) {
                    // Adjust the execution's vertical extend
                    double targetYPos = message.getTargetYPos();
                    if (execution.getPosition().y == 0) {
                        // If this is the first message to encounter this execution, initialize the
                        // execution's y coordinate and height
                        execution.getPosition().y = targetYPos;
                        execution.getSize().y = 0;
                    } else {
                        // The execution already has a position and size; adjust.
                        if (targetYPos < execution.getPosition().y) {
                            double delta = execution.getPosition().y - targetYPos;
                            execution.getPosition().y = targetYPos;
                            execution.getSize().y += delta;
                        } else if (targetYPos > execution.getPosition().y + execution.getSize().y) {
                            execution.getSize().y = targetYPos - execution.getPosition().y;
                        }
                    }
                }
            }
        }

        // Found messages start between their target and the previous lifeline
        if (messageType == MessageType.FOUND) {
            edgeLayout.getSourcePoint().setX((float)
                    (lifeline.getPosition().x - context.lifelineSpacing / 2));
        }
        
        // Specify bend points for self loops
        if (message.getSource() == message.getTarget()) {
            KPoint bendPoint = KLayoutDataFactory.eINSTANCE.createKPoint();
            bendPoint.setX((float) (llCenter + context.messageSpacing / 2));
            bendPoint.setY(edgeLayout.getTargetPoint().getY());
            edgeLayout.getBendPoints().add(bendPoint);
        }
    }
    
    
    /////////////////////////////////////////////////////////////////////////////////////////////////
    // Labels

    /**
     * Place the label(s) of the given message.
     * 
     * @param context
     *            the layout context that contains all relevant information for the current layout run.
     * @param message
     *            the message whose labels to place
     * @param edge
     *            the edge representing the message in the original graph
     */
    private void placeLabels(final LayoutContext context, final SMessage message, final KEdge edge) {
        for (KLabel label : edge.getLabels()) {
            KShapeLayout labelLayout = label.getData(KShapeLayout.class);
            
            SLifeline messageTarget = message.getTarget();
            SLifeline messageSource = message.getSource();
            
            if (messageSource.getHorizontalSlot() < messageTarget.getHorizontalSlot()) {
                placeRightPointingMessageLabels(context, message, labelLayout);
            } else if (messageSource.getHorizontalSlot() > messageTarget.getHorizontalSlot()) {
                placeLeftPointingMessageLabels(context, message, labelLayout);
            } else {
                // The message is a self loop, so place labels to its right
                KEdgeLayout edgeLayout = edge.getData(KEdgeLayout.class);
                double xPos;
                if (edgeLayout.getBendPoints().size() > 0) {
                    KPoint firstBend = edgeLayout.getBendPoints().get(0);
                    xPos = firstBend.getX();
                } else {
                    xPos = edgeLayout.getSourcePoint().getX();
                }
                labelLayout.setYpos((float)
                        (message.getSourceYPos() + SequenceLayoutConstants.LABELSPACING));
                labelLayout.setXpos((float)
                        (xPos + SequenceLayoutConstants.LABELMARGIN / 2));
            }
        }
    }


    /**
     * Place a label of the given rightwards pointing message.
     * 
     * @param context
     *            the layout context that contains all relevant information for the current layout run.
     * @param message
     *            the message whose label to place
     * @param labelLayout
     *            layout of the label to be placed where the layout information will be stored
     */
    private void placeRightPointingMessageLabels(final LayoutContext context, final SMessage message,
            final KShapeLayout labelLayout) {
        
        SLifeline srcLifeline = message.getSource();
        double llCenter = srcLifeline.getPosition().x + srcLifeline.getSize().x / 2;
        
        // Labels are placed above messages pointing rightwards
        labelLayout.setYpos((float) (message.getSourceYPos() - labelLayout.getHeight() - 2));
        
        // For the horizontal alignment, we need to check which alignment strategy to use
        LabelAlignment alignment = context.labelAlignment;
        
        if (alignment == LabelAlignment.SOURCE_CENTER
                && srcLifeline.getHorizontalSlot() + 1 == context.lifelineOrder.size()) {
            
            // This is a lost message; fall back to source placement
            alignment = LabelAlignment.SOURCE;
        } else if (message.getProperty(SequenceDiagramProperties.MESSAGE_TYPE) == MessageType.CREATE) {
            // Create messages always use SOURCE placement to avoid overlapping the target lifeline
            // header
            alignment = LabelAlignment.SOURCE;
        }
        
        // Actually calculate the horizontal position
        switch (alignment) {
        case SOURCE_CENTER:
            // Place label centered between the source lifeline and the next lifeline
            SLifeline nextLL = context.lifelineOrder.get(srcLifeline.getHorizontalSlot() + 1);
            double center = (llCenter + nextLL.getPosition().x + nextLL.getSize().x / 2) / 2;
            labelLayout.setXpos((float) (center - labelLayout.getWidth() / 2));
            break;
        case SOURCE:
            // Place label near the source lifeline
            labelLayout.setXpos((float) llCenter + SequenceLayoutConstants.LABELSPACING);
            break;
        case CENTER:
            // Place label at the center of the message
            double targetCenter = message.getTarget().getPosition().x
                    + message.getTarget().getSize().x / 2;
            labelLayout.setXpos((float) ((llCenter + targetCenter) / 2 - labelLayout.getWidth() / 2));
            break;
        }
    }


    /**
     * Place a label of the given leftwards pointing message.
     * 
     * @param context
     *            the layout context that contains all relevant information for the current layout run.
     * @param message
     *            the message whose label to place
     * @param labelLayout
     *            layout of the label to be placed where the layout information will be stored
     */
    private void placeLeftPointingMessageLabels(final LayoutContext context, final SMessage message,
            final KShapeLayout labelLayout) {

        SLifeline srcLifeline = message.getSource();
        double llCenter = srcLifeline.getPosition().x + srcLifeline.getSize().x / 2;

        // Labels are placed below messages pointing leftwards
        labelLayout.setYpos((float) (message.getSourceYPos() + 2));
        
        // For the horizontal alignment, we need to check which alignment strategy to use
        LabelAlignment alignment = context.labelAlignment;
        
        if (alignment == LabelAlignment.SOURCE_CENTER && srcLifeline.getHorizontalSlot() == 0) {
            // This is a found message; fall back to source placement
            alignment = LabelAlignment.SOURCE;
        }
        
        // Actually calculate the horizontal position
        switch (alignment) {
        case SOURCE_CENTER:
            // Place label centered between the source lifeline and the previous lifeline
            SLifeline lastLL = context.lifelineOrder.get(srcLifeline.getHorizontalSlot() - 1);
            double center = (llCenter + lastLL.getPosition().x + lastLL.getSize().x / 2) / 2;
            labelLayout.setXpos((float) (center - labelLayout.getWidth() / 2));
            break;
        case SOURCE:
            // Place label near the source lifeline
            labelLayout.setXpos((float)
                    (llCenter - labelLayout.getWidth() - SequenceLayoutConstants.LABELSPACING));
            break;
        case CENTER:
            // Place label at the center of the message
            double targetCenter = message.getTarget().getPosition().x
                    + message.getTarget().getSize().x / 2;
            labelLayout.setXpos((float) ((llCenter + targetCenter) / 2 - labelLayout.getWidth() / 2));
            break;
        }
    }
    
    
    /////////////////////////////////////////////////////////////////////////////////////////////////
    // Executions

    /**
     * Apply execution coordinates and adjust positions of messages attached to these executions.
     * 
     * @param context
     *            the layout context that contains all relevant information for the current layout run.
     * @param lifeline
     *            the lifeline, whose executions are placed
     */
    private void applyExecutionCoordinates(final LayoutContext context, final SLifeline lifeline) {
        List<SequenceExecution> executions = lifeline.getProperty(SequenceDiagramProperties.EXECUTIONS);
        if (executions == null) {
            return;
        }

        // Set xPos, maxXPos and height / maxYPos
        arrangeExecutions(executions, lifeline.getSize().x);

        // Get the layout data of the execution
        KNode node = (KNode) lifeline.getProperty(InternalProperties.ORIGIN);
        KShapeLayout nodeLayout = node.getData(KShapeLayout.class);

        // Walk through the lifeline's executions
        nodeLayout.setProperty(SequenceDiagramProperties.EXECUTIONS, executions);
        for (SequenceExecution execution : executions) {
            Object executionObj = execution.getOrigin();

            if (executionObj instanceof KNode) {
                if (execution.getType() == SequenceExecutionType.DURATION
                        || execution.getType() == SequenceExecutionType.TIME_CONSTRAINT) {
                    
                    execution.getPosition().y += SequenceLayoutConstants.TWENTY;
                }

                // Apply calculated coordinates to the execution
                KNode executionNode = (KNode) executionObj;
                KShapeLayout shapelayout = executionNode.getData(KShapeLayout.class);
                shapelayout.setXpos((float) execution.getPosition().x);
                shapelayout.setYpos((float) (execution.getPosition().y - context.lifelineYPos));
                shapelayout.setWidth((float) execution.getSize().x);
                shapelayout.setHeight((float) execution.getSize().y);

                // Determine max and min y-pos of messages
                double minYPos = lifeline.getSize().y;
                double maxYPos = 0;
                for (Object messObj : execution.getMessages()) {
                    if (messObj instanceof SMessage) {
                        SMessage message = (SMessage) messObj;
                        double messageYPos;
                        if (message.getSource() == lifeline) {
                            messageYPos = message.getSourceYPos();
                        } else {
                            messageYPos = message.getTargetYPos();
                        }
                        if (messageYPos < minYPos) {
                            minYPos = messageYPos;
                        }
                        if (messageYPos > maxYPos) {
                            maxYPos = messageYPos;
                        }
                    }
                }

                /*
                 * TODO set executionFactor to one if the Papyrus team fixes the bug. Calculate
                 * conversion factor. Conversion is necessary because Papyrus stores the
                 * y-coordinates in a very strange way. When the message starts or ends at an
                 * execution, y-coordinates must be given relative to the execution. However, these
                 * relative coordinates must be scaled as if the execution was having the height of
                 * its lifeline.
                 */
                double effectiveHeight = lifeline.getSize().y - SequenceLayoutConstants.TWENTY;
                double executionHeight = maxYPos - minYPos;
                double executionFactor = effectiveHeight / executionHeight;

                // Walk through execution's messages and adjust their position
                for (Object messObj : execution.getMessages()) {
                    if (messObj instanceof SMessage) {
                        SMessage mess = (SMessage) messObj;
                        boolean toLeft = false;
                        if (mess.getSource().getHorizontalSlot() > mess.getTarget()
                                .getHorizontalSlot()) {
                            // Message leads leftwards
                            toLeft = true;
                        }

                        KEdge edge = (KEdge) mess.getProperty(InternalProperties.ORIGIN);
                        KEdgeLayout edgeLayout = edge.getData(KEdgeLayout.class);
                        double newXPos = lifeline.getPosition().x + execution.getPosition().x;
                        if (mess.getSource() == mess.getTarget()) {
                            // Selfloop: insert bend points
                            edgeLayout.getBendPoints().get(0).setY(edgeLayout.getSourcePoint().getY());
                            edgeLayout.getBendPoints().get(1).setY(edgeLayout.getTargetPoint().getY());
                            edgeLayout.getTargetPoint().setX((float) (newXPos + execution.getSize().x));
                            edgeLayout.getTargetPoint().setY(0);
                        } else if (mess.getSource() == lifeline) {
                            if (!toLeft) {
                                newXPos += execution.getSize().x;
                            }
                            edgeLayout.getSourcePoint().setX((float) newXPos);

                            // Calculate the message's height relative to the execution
                            double relHeight = mess.getSourceYPos() - minYPos;
                            if (relHeight == 0) {
                                edgeLayout.getSourcePoint().setY(0);
                            } else {
                                edgeLayout.getSourcePoint().setY(
                                        (float) (context.lifelineHeader + relHeight * executionFactor));
                            }
                        } else {
                            if (toLeft) {
                                newXPos += execution.getSize().x;
                            }
                            edgeLayout.getTargetPoint().setX((float) newXPos);

                            // Calculate the message's height relative to the execution
                            double relHeight = mess.getTargetYPos() - minYPos;
                            if (relHeight == 0) {
                                edgeLayout.getTargetPoint().setY(0);
                            } else {
                                edgeLayout.getTargetPoint().setY(
                                        (float) (context.lifelineHeader + relHeight * executionFactor));
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Set x position and width of an execution and check for minimum height.
     * 
     * @param executions
     *            List of {@link SequenceExecution} at the given {@link SLifeline}
     * @param parentWidth
     *            Width of the {@link SLifeline}
     */
    private void arrangeExecutions(final List<SequenceExecution> executions, final double parentWidth) {
        final double minHeight = 20;
        final double executionWidth = 16;

        // Initially set horizontal position and height of empty executions
        for (SequenceExecution execution : executions) {
            execution.getPosition().x = (parentWidth - executionWidth) / 2;
            // Give executions without messages their original height and yPos
            if (execution.getMessages().size() == 0) {
                KShapeLayout shapelayout = execution.getOrigin().getData(KShapeLayout.class);
                execution.getPosition().y = shapelayout.getYpos();
                execution.getSize().y = shapelayout.getHeight();
            }
        }

        if (executions.size() > 1) {
            // reset xPos if execution is attached to another execution
            for (SequenceExecution execution : executions) {
                if (execution.getType() == SequenceExecutionType.DURATION
                        || execution.getType() == SequenceExecutionType.TIME_CONSTRAINT) {
                    continue;
                }
                
                int pos = 0;
                for (SequenceExecution otherExecution : executions) {
                    if (execution != otherExecution) {
                        if (execution.getPosition().y > otherExecution.getPosition().y
                                && execution.getPosition().y + execution.getSize().y < otherExecution
                                        .getPosition().y + otherExecution.getSize().y) {
                            pos++;
                        }
                    }
                }
                if (pos > 0) {
                    execution.getPosition().x = execution.getPosition().x + pos * executionWidth
                            / 2;
                }
            }
        }

        // Check minimum height of executions and set width
        for (SequenceExecution execution : executions) {
            if (execution.getSize().y < minHeight) {
                execution.getSize().y = minHeight;
            }
            
            if (execution.getType() == SequenceExecutionType.DURATION
                    || execution.getType() == SequenceExecutionType.TIME_CONSTRAINT) {
                
                continue;
            }
            execution.getSize().x = executionWidth;
        }
    }
    
    
    /////////////////////////////////////////////////////////////////////////////////////////////////
    // Comments

    /**
     * Place the comment objects (comments, constraints) according to their calculated coordinates.
     * 
     * @param graph
     *            the Sequence Graph
     */
    private void placeComments(final SGraph graph) {
        for (SComment comment : graph.getComments()) {
            Object origin = comment.getProperty(InternalProperties.ORIGIN);
            KShapeLayout commentLayout = ((KNode) origin).getData(KShapeLayout.class);
            commentLayout.setPos((float) comment.getPosition().x, (float) comment.getPosition().y);
            if (comment.getMessage() != null) {
                // Connected comments

                // Set coordinates for the connection of the comment
                double edgeSourceXPos, edgeSourceYPos, edgeTargetXPos, edgeTargetYPos;
                String attachedElement = comment.getProperty(
                        SequenceDiagramProperties.ATTACHED_ELEMENT_TYPE);
                if (attachedElement.toLowerCase().startsWith("lifeline")
                        || attachedElement.toLowerCase().contains("execution")) {
                    
                    // Connections to lifelines or executions are drawn horizontally
                    SLifeline lifeline = comment.getLifeline();
                    edgeSourceXPos = comment.getPosition().x;
                    edgeSourceYPos = comment.getPosition().y + comment.getSize().y / 2;
                    edgeTargetXPos = lifeline.getPosition().x + lifeline.getSize().x / 2;
                    edgeTargetYPos = edgeSourceYPos;
                } else {
                    // Connections to messages are drawn vertically
                    edgeSourceXPos = comment.getPosition().x + comment.getSize().x / 2;
                    edgeTargetXPos = edgeSourceXPos;
                    KEdge edge = (KEdge) comment.getMessage().getProperty(InternalProperties.ORIGIN);
                    KEdgeLayout edgeLayout = edge.getData(KEdgeLayout.class);
                    KPoint targetPoint = edgeLayout.getTargetPoint();
                    KPoint sourcePoint = edgeLayout.getSourcePoint();
                    edgeSourceYPos = comment.getPosition().y + comment.getSize().y;
                    edgeTargetYPos = (targetPoint.getY() + sourcePoint.getY()) / 2;
                }

                // Apply connection coordinates to layout
                KEdgeLayout edgelayout = comment.getProperty(
                        SequenceDiagramProperties.COMMENT_CONNECTION).getData(KEdgeLayout.class);
                edgelayout.getSourcePoint().setPos((float) edgeSourceXPos, (float) edgeSourceYPos);
                edgelayout.getTargetPoint().setPos((float) edgeTargetXPos, (float) edgeTargetYPos);
            }
        }
    }

}
