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
package de.cau.cs.kieler.papyrus.sequence;

import java.util.List;

import com.google.common.collect.Lists;

import de.cau.cs.kieler.core.alg.IKielerProgressMonitor;
import de.cau.cs.kieler.core.kgraph.KEdge;
import de.cau.cs.kieler.core.kgraph.KLabel;
import de.cau.cs.kieler.core.kgraph.KNode;
import de.cau.cs.kieler.kiml.AbstractLayoutProvider;
import de.cau.cs.kieler.kiml.UnsupportedGraphException;
import de.cau.cs.kieler.kiml.klayoutdata.KEdgeLayout;
import de.cau.cs.kieler.kiml.klayoutdata.KLayoutData;
import de.cau.cs.kieler.kiml.klayoutdata.KPoint;
import de.cau.cs.kieler.kiml.klayoutdata.KShapeLayout;
import de.cau.cs.kieler.klay.layered.properties.InternalProperties;
import de.cau.cs.kieler.papyrus.sequence.graph.SComment;
import de.cau.cs.kieler.papyrus.sequence.graph.SGraph;
import de.cau.cs.kieler.papyrus.sequence.graph.SLifeline;
import de.cau.cs.kieler.papyrus.sequence.graph.SMessage;
import de.cau.cs.kieler.papyrus.sequence.graph.transform.KGraphImporter;
import de.cau.cs.kieler.papyrus.sequence.p1allocation.SpaceAllocator;
import de.cau.cs.kieler.papyrus.sequence.p2cycles.SCycleBreaker;
import de.cau.cs.kieler.papyrus.sequence.p3layering.MessageLayerer;
import de.cau.cs.kieler.papyrus.sequence.p4sorting.EqualDistributionLifelineSorter;
import de.cau.cs.kieler.papyrus.sequence.p4sorting.InteractiveLifelineSorter;
import de.cau.cs.kieler.papyrus.sequence.p4sorting.LayerBasedLifelineSorter;
import de.cau.cs.kieler.papyrus.sequence.p5coordinates.CoordinateCalculator;
import de.cau.cs.kieler.papyrus.sequence.properties.MessageType;
import de.cau.cs.kieler.papyrus.sequence.properties.NodeType;
import de.cau.cs.kieler.papyrus.sequence.properties.SequenceDiagramProperties;
import de.cau.cs.kieler.papyrus.sequence.properties.SequenceExecution;
import de.cau.cs.kieler.papyrus.sequence.properties.SequenceExecution.SequenceExecutionType;

/**
 * Layout algorithm for Papyrus sequence diagrams.
 * 
 * @author grh
 * @kieler.design 2012-11-20 cds, msp
 * @kieler.rating proposed yellow grh
 */
public final class SequenceDiagramLayoutProvider extends AbstractLayoutProvider {
    /** The layout provider's ID. */
    public static final String ID = "de.cau.cs.kieler.papyrus.sequence.layout";

    /** Constant that is needed to calculate some offsets. */
    private static final int TWENTY = 20;
    /** Constant that is needed to calculate some offsets. */
    private static final int FOURTY = 40;
    /** The vertical spacing between message and label. */
    private static final int LABELSPACING = 5;
    /** The horizontal margin for message labels. */
    private static final int LABELMARGIN = 10;
    
    
    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Core Algorithm

    @Override
    public void doLayout(final KNode parentNode, final IKielerProgressMonitor progressMonitor) {
        // Prevent the surrounding diagram from being laid out
        if (parentNode.getParent() == null) {
            throw new UnsupportedGraphException(
                    "Sequence diagram layout can only be run on surrounding interactions.");
        }

        progressMonitor.begin("Sequence Diagrem Layouter", 1 + 1 + 1 + 1);
        
        // Initialize our layout context
        KLayoutData parentNodeLayoutData = parentNode.getData(KLayoutData.class);
        LayoutContext context = LayoutContext.fromLayoutData(parentNodeLayoutData);

        // Convert the KGraph into an SGraph and into an LGraph
        KGraphImporter importer = new KGraphImporter();
        context.sgraph = importer.importGraph(parentNode, progressMonitor.subTask(1));
        context.lgraph = importer.createLayeredGraph(context.sgraph, progressMonitor.subTask(1));
        
        // Assemble and execute the algorithm
        List<ISequenceLayoutProcessor> algorithm = assembleLayoutProcessors(context);
        float monitorIncrement = 1.0f / algorithm.size();
        
        for (ISequenceLayoutProcessor processor : algorithm) {
            processor.process(context, progressMonitor.subTask(monitorIncrement));
        }

        // Apply the calculated layout to the KGraph
        applyLayout(context, parentNode, progressMonitor.subTask(1));

        progressMonitor.done();
    }
    
    /**
     * Assembles the list of layout processors that, when run in order, implement the sequence
     * diagram layout algorithm. The list may be different based on the given layout context.
     * 
     * @param context
     *            the layout context that contains all relevant information for the current layout
     *            run.
     * @return list of layout processors.
     */
    private List<ISequenceLayoutProcessor> assembleLayoutProcessors(final LayoutContext context) {
        List<ISequenceLayoutProcessor> processors = Lists.newArrayList();
        
        processors.add(new SpaceAllocator());
        processors.add(new SCycleBreaker());
        processors.add(new MessageLayerer());
        
        // Lifeline sorting provides different options
        switch (context.sortingStrategy) {
        case LAYER_BASED:
            processors.add(new LayerBasedLifelineSorter());
            break;
            
        case SHORT_MESSAGES:
            processors.add(new EqualDistributionLifelineSorter());
            break;
            
        default:
            processors.add(new InteractiveLifelineSorter());
            break;
        }
        
        processors.add(new CoordinateCalculator());
        
        return processors;
    }
    

    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Layout Application

    /**
     * Apply the calculated coordinates to the KGraph.
     * 
     * @param context
     *            the layout context that contains all relevant information for the current layout run.
     * @param parentNode
     *            the parent layout node in the original KGraph.
     * @param progressMonitor
     *            a progress monitor to track algorithm execution.
     */
    private void applyLayout(final LayoutContext context, final KNode parentNode,
            final IKielerProgressMonitor progressMonitor) {
        
        progressMonitor.begin("Applying Layout Results", 1);
        
        // The height of the diagram (the surrounding interaction)
        double diagramHeight = context.sgraph.getSize().y + context.messageSpacing
                + context.lifelineHeader + context.lifelineYPos;
        
        // Set position for lifelines/nodes
        for (SLifeline lifeline : context.lifelineOrder) {
            // Dummy lifelines don't need any layout
            if (lifeline.isDummy()) {
                continue;
            }

            KNode node = (KNode) lifeline.getProperty(InternalProperties.ORIGIN);
            KShapeLayout nodeLayout = node.getData(KShapeLayout.class);

            if (nodeLayout.getProperty(SequenceDiagramProperties.NODE_TYPE)
                    == NodeType.SURROUNDING_INTERACTION) {
                
                // This is the surrounding node
                break;
            }

            // Handle messages of the lifeline and their labels
            List<SequenceExecution> executions = lifeline.getProperty(
                    SequenceDiagramProperties.EXECUTIONS);
            applyMessageCoordinates(context, diagramHeight, lifeline, executions);

            // Apply execution coordinates and adjust positions of messages attached to these
            // executions.
            applyExecutionCoordinates(context, lifeline);

            // Set position and height for the lifeline.
            nodeLayout.setYpos((float) lifeline.getPosition().y);
            nodeLayout.setXpos((float) lifeline.getPosition().x);
            nodeLayout.setHeight((float) lifeline.getSize().y);

            // Place destruction if existing
            KNode destruction = lifeline.getProperty(SequenceDiagramProperties.DESTRUCTION_EVENT);
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
        KShapeLayout parentLayout = parentNode.getData(KShapeLayout.class);
        parentLayout.setWidth((float) context.sgraph.getSize().x);
        parentLayout.setHeight((float) diagramHeight);
        parentLayout.setPos((float) context.borderSpacing, (float) context.borderSpacing);
        
        progressMonitor.done();
    }

    /**
     * Apply the calculated coordinates of the messages that are connected to the given lifeline.
     * 
     * @param context
     *            the layout context that contains all relevant information for the current layout run.
     * @param diagramHeight
     *            the height of the whole diagram
     * @param lifeline
     *            the lifeline whose messages are handled
     * @param executions
     *            the list of executions
     */
    private void applyMessageCoordinates(final LayoutContext context, final double diagramHeight,
            final SLifeline lifeline, final List<SequenceExecution> executions) {
        
        /*
         * TODO Set this to one if Papyrus team fixes its bug. Workaround for Papyrus bug:
         * Y-coordinates are stored in a strange way by Papyrus. When the message starts or ends at
         * a lifeline, y-coordinates must be given relative to the lifeline. However, these relative
         * coordinates must be scaled as if the lifeline was having the height of its surrounding
         * interaction.
         */
        double factor = (diagramHeight + TWENTY) / lifeline.getSize().y;

        // Resize node if there are any create or delete messages involved
        for (SMessage message : lifeline.getIncomingMessages()) {
            if (message.getProperty(SequenceDiagramProperties.MESSAGE_TYPE) == MessageType.CREATE) {
                // Set lifeline's yPos to the yPos of the create-message
                lifeline.getPosition().y = message.getTargetYPos() + context.lifelineHeader / 2;
                
                // Modify height of lifeline in order to compensate yPos changes
                lifeline.getSize().y += context.lifelineYPos - message.getTargetYPos()
                        - context.lifelineHeader / 2;
            } else if (message.getProperty(SequenceDiagramProperties.MESSAGE_TYPE) 
                    == MessageType.DELETE) {
                
                // Modify height of lifeline in order to end at the yPos of the delete-message
                lifeline.getSize().y -= context.sgraph.getSize().y + context.messageSpacing
                        - message.getTargetYPos();
            }
        }

        // The horizontal center of the current lifeline
        double llCenter = lifeline.getPosition().x + lifeline.getSize().x / 2;

        // Handle outgoing messages
        for (SMessage message : lifeline.getOutgoingMessages()) {
            KEdge edge = (KEdge) message.getProperty(InternalProperties.ORIGIN);
            KEdgeLayout edgeLayout = edge.getData(KEdgeLayout.class);
            KPoint sourcePoint = edgeLayout.getSourcePoint();
            sourcePoint.setY((float) (message.getSourceYPos() * factor));
            sourcePoint.setX((float) (lifeline.getPosition().x + lifeline.getSize().x / 2));

            // Set execution coordinates according to connected messages coordinates
            if (executions != null) {
                for (SequenceExecution execution : executions) {
                    if (execution.getMessages().contains(message)) {
                        double sourceYPos = message.getSourceYPos();
                        if (execution.getPosition().y == 0) {
                            execution.getPosition().y = sourceYPos;
                            execution.getSize().y = 0;
                        } else {
                            if (sourceYPos < execution.getPosition().y) {
                                if (message.getSource() != message.getTarget()) {
                                    double diff = execution.getPosition().y - sourceYPos;
                                    execution.getPosition().y = sourceYPos;
                                    if (execution.getSize().y >= 0) {
                                        execution.getSize().y += diff;
                                    }
                                }
                            }
                            if (sourceYPos > execution.getPosition().y + execution.getSize().y) {
                                execution.getSize().y = sourceYPos - execution.getPosition().y;
                            }
                        }
                    }
                }
            }

            // Handle messages that lead to something else than a lifeline
            if (message.getTarget().isDummy()) {
                KPoint targetPoint = edgeLayout.getTargetPoint();
                double reverseFactor = lifeline.getSize().y / (diagramHeight + FOURTY);
                targetPoint.setY((float) (TWENTY + message.getTargetYPos() * reverseFactor));

                // Lost-messages end between its source and the next lifeline
                if (message.getProperty(SequenceDiagramProperties.MESSAGE_TYPE) == MessageType.LOST) {
                    targetPoint.setX((float) (lifeline.getPosition().x + lifeline.getSize().x 
                            + context.lifelineSpacing / 2));
                }
            }

            if (message.getSource() == message.getTarget()) {
                // Specify bendpoints for selfloops
                List<KPoint> bendPoints = edgeLayout.getBendPoints();
                bendPoints.get(0).setX((float) (llCenter + context.messageSpacing / 2));
                bendPoints.get(0).setY(edgeLayout.getSourcePoint().getY());
            }

            // Walk through the labels and adjust their position
            placeLabels(context, lifeline, factor, llCenter, message, edge);
        }

        // Handle incoming messages
        for (SMessage message : lifeline.getIncomingMessages()) {
            KEdge edge = (KEdge) message.getProperty(InternalProperties.ORIGIN);
            KEdgeLayout edgeLayout = edge.getData(KEdgeLayout.class);
            KPoint targetPoint = edgeLayout.getTargetPoint();
            targetPoint.setX((float) (lifeline.getPosition().x + lifeline.getSize().x / 2));
            targetPoint.setY((float) (message.getTargetYPos() * factor));

            if (message.getProperty(SequenceDiagramProperties.MESSAGE_TYPE) == MessageType.CREATE) {
                // Reset x-position of create message because it leads to the header and not the line
                targetPoint.setX((float) lifeline.getPosition().x);
            } else if (message.getProperty(SequenceDiagramProperties.MESSAGE_TYPE) 
                    == MessageType.DELETE) {
                // Reset y-position of delete message to end at the end of the lifeline
                targetPoint.setY((float) ((lifeline.getPosition().y + lifeline.getSize().y 
                        - context.lifelineHeader) * factor));
            }

            // Reset execution coordinates if the message is contained in an execution
            if (executions != null) {
                for (SequenceExecution execution : executions) {
                    if (execution.getMessages().contains(message)) {
                        double targetYPos = message.getTargetYPos();
                        if (execution.getPosition().y == 0) {
                            execution.getPosition().y = targetYPos;
                            execution.getSize().y = 0;
                        } else {
                            if (targetYPos < execution.getPosition().y) {
                                double diff = execution.getPosition().y - targetYPos;
                                execution.getPosition().y = targetYPos;
                                if (execution.getSize().y >= 0) {
                                    execution.getSize().y += diff;
                                }
                            }
                            if (targetYPos > execution.getPosition().y + execution.getSize().y) {
                                execution.getSize().y = targetYPos - execution.getPosition().y;
                            }
                        }
                    }
                }
            }

            // Handle messages that come from something else than a lifeline
            if (message.getSource().isDummy()) {
                KPoint sourcePoint = edgeLayout.getSourcePoint();
                double reverseFactor = lifeline.getSize().y / (diagramHeight + FOURTY);
                sourcePoint.setY((float) (TWENTY + message.getSourceYPos() * reverseFactor));

                // Found-messages start between its source and the previous lifeline
                if (message.getProperty(SequenceDiagramProperties.MESSAGE_TYPE) == MessageType.FOUND) {
                    sourcePoint.setX((float) (lifeline.getPosition().x - context.lifelineSpacing / 2));
                }
            }

            if (message.getSource() == message.getTarget()) {
                // Specify bendpoints for selfloops
                List<KPoint> bendPoints = edgeLayout.getBendPoints();
                bendPoints.get(1).setX((float) (llCenter + context.messageSpacing / 2));
                bendPoints.get(1).setY(edgeLayout.getTargetPoint().getY());
            }
        }
    }

    /**
     * Place the label(s) of the given message.
     * 
     * @param context
     *            the layout context that contains all relevant information for the current layout run.
     * @param lifeline
     *            the current lifeline
     * @param factor
     *            the edge factor
     * @param llCenter
     *            the horizontal center of the current lifeline
     * @param message
     *            the current message
     * @param edge
     *            the edge representation of the message
     */
    private void placeLabels(final LayoutContext context, final SLifeline lifeline,
            final double factor, final double llCenter, final SMessage message, final KEdge edge) {
        
        for (KLabel label : edge.getLabels()) {
            KShapeLayout labelLayout = label.getData(KShapeLayout.class);

            // The index of the current lifeline in the ordered list of lifelines
            int lifelineIndex = context.lifelineOrder.indexOf(lifeline);

            if (message.getTarget().getHorizontalSlot() > lifeline.getHorizontalSlot()) {
                // Message leads rightwards
                switch (context.labelAlignment) {
                case SOURCE_CENTER:
                    // If the lifeline is the last lifeline (lost message), fall through to SOURCE
                    // placement to avoid ArrayIndexOutOfBoundsException
                    if (lifelineIndex + 1 < context.lifelineOrder.size()) {
                        // Place labels centered between the source lifeline and its neighbored
                        // lifeline
                        SLifeline nextLL = context.lifelineOrder.get(lifelineIndex + 1);
                        double center = (llCenter + nextLL.getPosition().x + nextLL.getSize().x / 2) / 2;
                        labelLayout.setXpos((float) (center - labelLayout.getWidth() / 2));
                        break;
                    }
                case SOURCE:
                    // Place labels near the source lifeline
                    labelLayout.setXpos((float) llCenter + LABELSPACING);
                    break;
                case CENTER:
                    // Place labels in the center of the message
                    double targetCenter = message.getTarget().getPosition().x
                            + message.getTarget().getSize().x / 2;
                    labelLayout.setXpos((float) ((llCenter + targetCenter) / 2 - labelLayout
                            .getWidth() / 2));
                }
                // Create messages should not overlap the target's header
                if (message.getProperty(SequenceDiagramProperties.MESSAGE_TYPE) == MessageType.CREATE) {
                    labelLayout.setXpos((float) (llCenter + LABELSPACING));
                }
                labelLayout.setYpos((float) ((message.getSourceYPos() - labelLayout.getHeight() - 2)
                        * factor));
            } else if (message.getTarget().getHorizontalSlot() < lifeline.getHorizontalSlot()) {
                // Message leads leftwards
                switch (context.labelAlignment) {
                case SOURCE_CENTER:
                    // If the lifeline is the first lifeline (found message), fall through to SOURCE
                    // placement to avoid ArrayIndexOutOfBoundsException
                    if (lifelineIndex > 0) {
                        // Place labels centered between the source lifeline and its neighbored
                        // lifeline
                        SLifeline lastLL = context.lifelineOrder.get(lifelineIndex - 1);
                        double center = (llCenter + lastLL.getPosition().x + lastLL.getSize().x / 2) / 2;
                        labelLayout.setXpos((float) (center - labelLayout.getWidth() / 2));
                        break;
                    }
                case SOURCE:
                    // Place labels near the source lifeline
                    labelLayout.setXpos((float) (llCenter - labelLayout.getWidth() - LABELSPACING));
                    break;
                case CENTER:
                    // Place labels in the center of the message
                    double targetCenter = message.getTarget().getPosition().x
                            + message.getTarget().getSize().x / 2;
                    labelLayout.setXpos((float) ((llCenter + targetCenter) / 2 - labelLayout
                            .getWidth() / 2));
                }
                labelLayout.setYpos((float) ((message.getSourceYPos() + 2) * factor));
            } else {
                // Message is selfloop
                
                // Place labels right of the selfloop
                KEdgeLayout edgeLayout = edge.getData(KEdgeLayout.class);
                double xPos;
                if (edgeLayout.getBendPoints().size() > 0) {
                    KPoint firstBend = edgeLayout.getBendPoints().get(0);
                    xPos = firstBend.getX();
                } else {
                    xPos = edgeLayout.getSourcePoint().getX();
                }
                labelLayout.setYpos((float) ((message.getSourceYPos() + LABELSPACING) * factor));
                labelLayout.setXpos((float) (xPos + LABELMARGIN / 2));
            }
        }
    }

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
                    
                    execution.getPosition().y += TWENTY;
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
                double effectiveHeight = lifeline.getSize().y - TWENTY;
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
