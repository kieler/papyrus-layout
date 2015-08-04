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
package de.cau.cs.kieler.papyrus.sequence.properties;

import java.util.List;

import de.cau.cs.kieler.core.kgraph.KEdge;
import de.cau.cs.kieler.core.kgraph.KNode;
import de.cau.cs.kieler.core.properties.IProperty;
import de.cau.cs.kieler.core.properties.Property;
import de.cau.cs.kieler.kiml.options.LayoutOptions;
import de.cau.cs.kieler.klay.layered.graph.LNode;
import de.cau.cs.kieler.papyrus.sequence.graph.SLifeline;
import de.cau.cs.kieler.papyrus.sequence.p4sorting.LifelineSortingStrategy;

/**
 * Properties for sequence diagrams.
 * 
 * @author grh
 * @kieler.design 2012-11-20 cds, msp
 * @kieler.rating yellow 2012-12-11 cds, ima
 */
public final class SequenceDiagramProperties {

    private SequenceDiagramProperties() {
        // Hide the constructor
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Properties Describing the Graph Structure

    /** The type of a node. */
    public static final IProperty<NodeType> NODE_TYPE = new Property<NodeType>(
            "de.cau.cs.kieler.papyrus.sequence.nodeType", NodeType.LIFELINE);

    /** The type of a message. */
    public static final IProperty<MessageType> MESSAGE_TYPE = new Property<MessageType>(
            "de.cau.cs.kieler.papyrus.sequence.messageType", MessageType.ASYNCHRONOUS);

    /** The list of areas (interactions, combined fragments, etc.). */
    public static final IProperty<List<SequenceArea>> AREAS = new Property<List<SequenceArea>>(
            "de.cau.cs.kieler.papyrus.sequence.area");

    /** The list of execution specifications of a lifeline. */
    public static final IProperty<List<SequenceExecution>> EXECUTIONS = 
            new Property<List<SequenceExecution>>(
                    "de.cau.cs.kieler.papyrus.sequence.executionSpecifications");

    /** Property of a comment that indicates to what kind of element it is attached. */
    public static final IProperty<String> ATTACHED_ELEMENT_TYPE = new Property<String>(
            "de.cau.cs.kieler.papyrus.sequence.attachedElement");

    /** The list of objects, a comment is attached to. */
    public static final IProperty<List<Object>> ATTACHED_TO = new Property<List<Object>>(
            "de.cau.cs.kieler.papyrus.sequence.attachedTo");

    /** The destruction event of a lifeline. */
    public static final IProperty<KNode> DESTRUCTION = new Property<KNode>(
            "de.cau.cs.kieler.papyrus.sequence.destruction");
    
    

    
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Layout Properties

    /** Spacing to the border of the drawing. */
    public static final Property<Float> BORDER_SPACING = new Property<Float>(
            LayoutOptions.BORDER_SPACING, 12.0f, 0.0f);

    
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Other Properties

    /** The lifeline to which an element of the SGraph belongs. */
    public static final IProperty<SLifeline> BELONGS_TO_LIFELINE = new Property<SLifeline>(
            "de.cau.cs.kieler.papyrus.sequence.belongsToLifeline");

    /** The node in the layered graph that corresponds to a message. */
    public static final IProperty<LNode> LAYERED_NODE = new Property<LNode>(
            "de.cau.cs.kieler.papyrus.sequence.layeredNode");

    /** The node in the KGraph that corresponds to the destruction event of a lifeline. */
    public static final IProperty<KNode> DESTRUCTION_EVENT = new Property<KNode>(
            "de.cau.cs.kieler.papyrus.sequence.destructionEvent");

    /** The KEdge that connects the comment to another element of the diagram. */
    public static final IProperty<KEdge> COMMENT_CONNECTION = new Property<KEdge>(
            "de.cau.cs.kieler.papyrus.sequence.commentConnection");

    /** The height of the lifeline's header. */
    public static final IProperty<Integer> LIFELINE_HEADER = new Property<Integer>(
            "de.cau.cs.kieler.papyrus.sequence.lifelineHeader", 30);

    /** The vertical position of lifelines. */
    public static final IProperty<Integer> LIFELINE_Y_POS = new Property<Integer>(
            "de.cau.cs.kieler.papyrus.sequence.lifelineYPos", 10);

    /** The height of the header of combined fragments. */
    public static final IProperty<Integer> AREA_HEADER = new Property<Integer>(
            "de.cau.cs.kieler.papyrus.sequence.areaHeader", 25);

    /** The width of time observations. */
    public static final IProperty<Integer> TIME_OBSERVATION_WIDTH = new Property<Integer>(
            "de.cau.cs.kieler.papyrus.sequence.timeObservationWidth", 20);

    /** The offset between two nested areas. */
    public static final IProperty<Integer> CONTAINMENT_OFFSET = new Property<Integer>(
            "de.cau.cs.kieler.papyrus.sequence.containmentOffset", 5);

    /** The horizontal space between two neighbored lifelines. This property may be set by the user. */
    public static final IProperty<Float> LIFELINE_SPACING = new Property<Float>(
            "de.cau.cs.kieler.papyrus.sequence.lifelineSpacing", 50.0f);

    /** The vertical space between two neighbored messages. This property may be set by the user. */
    public static final IProperty<Float> MESSAGE_SPACING = new Property<Float>(
            "de.cau.cs.kieler.papyrus.sequence.messageSpacing", 50.0f);

    /** The alignment of message labels. This property may be set by the user. */
    public static final IProperty<LabelAlignment> LABEL_ALIGNMENT = new Property<LabelAlignment>(
            "de.cau.cs.kieler.papyrus.sequence.labelAlignment", LabelAlignment.SOURCE_CENTER);

    /**
     * The lifeline sorting strategy that should be used in the algorithm. This property may be set
     * by the user.
     */
    public static final Property<LifelineSortingStrategy> LIFELINE_SORTING =
            new Property<LifelineSortingStrategy>("de.cau.cs.kieler.papyrus.sequence.lifelineSorting",
                    LifelineSortingStrategy.INTERACTIVE);

    /**
     * If messages in areas should be grouped together. This property may be set by the user if the
     * SHORT_MESSAGES lifeline sorter is chosen.
     */
    public static final IProperty<Boolean> GROUP_AREAS = new Property<Boolean>(
            "de.cau.cs.kieler.papyrus.sequence.groupAreas", false);
    
    /**
     * The coordinate system the layout results are computed for.
     */
    public static final IProperty<CoordinateSystem> COORDINATE_SYSTEM = new Property<CoordinateSystem>(
            "de.cau.cs.kieler.papyrus.sequence.coordinateSystem", CoordinateSystem.KGRAPH);
    
}
