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

import com.google.common.collect.Lists;

import de.cau.cs.kieler.core.kgraph.KEdge;
import de.cau.cs.kieler.core.kgraph.KNode;
import de.cau.cs.kieler.core.properties.IProperty;
import de.cau.cs.kieler.core.properties.Property;
import de.cau.cs.kieler.kiml.options.LayoutOptions;
import de.cau.cs.kieler.klay.layered.graph.LNode;
import de.cau.cs.kieler.papyrus.sequence.graph.SLifeline;
import de.cau.cs.kieler.papyrus.sequence.p4sorting.LifelineSortingStrategy;

/**
 * Properties for sequence diagrams. Since the layout algorithm can work in one of two modes (Papyrus
 * and KGraph), some of these properties are mode-specific. If this is the case for a property, the
 * documentation will say so.
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
    
    /**
     * An ID that can be given to diagram elements and referenced by other elements. The ID must be
     * unique in the diagram for it to be of any use and should be {@code >= 0}.
     * 
     * <p>
     * The ID is only used in KGraph mode.
     * </p>
     */
    public static final IProperty<Integer> ELEMENT_ID = new Property<Integer>(
            "de.cau.cs.kieler.papyrus.sequence.elementId", -1);

    /** The type of a node. */
    public static final IProperty<NodeType> NODE_TYPE = new Property<NodeType>(
            "de.cau.cs.kieler.papyrus.sequence.nodeType", NodeType.LIFELINE);

    /** The type of a message. */
    public static final IProperty<MessageType> MESSAGE_TYPE = new Property<MessageType>(
            "de.cau.cs.kieler.papyrus.sequence.messageType", MessageType.ASYNCHRONOUS);

    /** The list of areas (interactions, combined fragments, etc.). */
    public static final IProperty<List<SequenceArea>> AREAS = new Property<List<SequenceArea>>(
            "de.cau.cs.kieler.papyrus.sequence.area");
    
    /**
     * List of element IDs of any areas (such as fragments) a message or lifeline belongs to, if any. On
     * lifelines, this only has to be set for empty areas.
     * 
     * <p>
     * This is only used in KGraph mode.
     * </p>
     */
    public static final IProperty<List<Integer>> AREA_IDS = new Property<>(
            "de.cau.cs.kieler.papyrus.sequence.areaIds", Lists.newArrayList());

    /**
     * Element IDs of an empty area (such as fragments) that should be placed directly above this
     * message, if any.
     * 
     * <p>
     * This is only used in KGraph mode.
     * </p>
     */
    public static final IProperty<Integer> UPPER_EMPTY_AREA_ID = new Property<>(
            "de.cau.cs.kieler.papyrus.sequence.upperEmptyAreaId", -1);
    
    /**
     * Element IDs of an area's parent area, if any.
     * 
     * <p>
     * This is only used in KGraph mode.
     * </p>
     */
    public static final IProperty<Integer> PARENT_AREA_ID = new Property<>(
            "de.cau.cs.kieler.papyrus.sequence.parentAreaId", -1);

    /**
     * The list of execution specifications of a lifeline.
     * 
     * <p>
     * This is only used in Papyrus mode.
     * </p>
     */
    public static final IProperty<List<SequenceExecution>> EXECUTIONS = 
            new Property<List<SequenceExecution>>(
                    "de.cau.cs.kieler.papyrus.sequence.executionSpecifications");
    
    /**
     * The type of execution a node represents.
     * 
     * <p>
     * This is only used in KGraph mode.
     * </p>
     */
    public static final IProperty<SequenceExecutionType> EXECUTION_TYPE = new Property<>(
            "de.cau.cs.kieler.papyrus.sequence.executionType", SequenceExecutionType.EXECUTION);
    
    /**
     * Element IDs of the executions a message starts at, if any. This is a list because a message can
     * be part of several nested executions. The most deeply nested execution will be the one the
     * message will actually be visually attached to.
     * 
     * <p>
     * This is only used in KGraph mode.
     * </p>
     */
    public static final IProperty<List<Integer>> SOURCE_EXECUTION_IDS = new Property<>(
            "de.cau.cs.kieler.papyrus.sequence.executionId.source", Lists.newArrayList());
    
    /**
     * Element IDs of the executions a message ends at, if any.
     * 
     * <p>
     * This is only used in KGraph mode.
     * </p>
     */
    public static final IProperty<List<Integer>> TARGET_EXECUTION_IDS = new Property<>(
            "de.cau.cs.kieler.papyrus.sequence.executionId.target", Lists.newArrayList());
    
    /** Property of a comment that indicates to what kind of element it is attached. */
    public static final IProperty<String> ATTACHED_ELEMENT_TYPE = new Property<String>(
            "de.cau.cs.kieler.papyrus.sequence.attachedElement");

    /**
     * The list of objects, a comment is attached to.
     * 
     * <p>
     * In Papyrus mode, this is a list of concrete diagram elements. In KGraph mode, this is a list of
     * IDs of diagram elements.
     * </p>
     */
    public static final IProperty<List<Object>> ATTACHED_TO = new Property<List<Object>>(
            "de.cau.cs.kieler.papyrus.sequence.attachedTo");
    
    /**
     * Element ID of the object a comment is attached to, if any.
     * 
     * <p>
     * This is only used in KGraph mode.
     * </p>
     */
    public static final IProperty<Integer> ATTACHED_TO_ID = new Property<>(
            "de.cau.cs.kieler.papyrus.sequence.attachedToId", -1);

    /**
     * The destruction event of a lifeline.
     * 
     * <p>
     * This is only used in Papyrus mode. In KGraph mode, the algorithm automatically finds the
     * destruction event node, if there is any.
     * </p> 
     */
    public static final IProperty<KNode> DESTRUCTION = new Property<KNode>(
            "de.cau.cs.kieler.papyrus.sequence.destruction");

    
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Layout Properties

    /** Spacing to the border of the drawing. */
    public static final Property<Float> BORDER_SPACING = new Property<Float>(
            LayoutOptions.BORDER_SPACING, 12.0f, 0.0f);
    
    /** The vertical space between two neighbored messages. This property may be set by the user. */
    public static final IProperty<Float> MESSAGE_SPACING = new Property<Float>(
            "de.cau.cs.kieler.papyrus.sequence.messageSpacing", 50.0f);
    
    /** The horizontal space between two neighbored lifelines. This property may be set by the user. */
    public static final IProperty<Float> LIFELINE_SPACING = new Property<Float>(
            "de.cau.cs.kieler.papyrus.sequence.lifelineSpacing", 50.0f);
    
    /** The vertical position of lifelines. */
    public static final IProperty<Integer> LIFELINE_Y_POS = new Property<Integer>(
            "de.cau.cs.kieler.papyrus.sequence.lifelineYPos", 10);

    /** The height of the lifeline's header. */
    public static final IProperty<Integer> LIFELINE_HEADER = new Property<Integer>(
            "de.cau.cs.kieler.papyrus.sequence.lifelineHeader", 30);

    /** The height of the header of combined fragments. */
    public static final IProperty<Integer> AREA_HEADER = new Property<Integer>(
            "de.cau.cs.kieler.papyrus.sequence.areaHeader", 25);

    /** The width of time observations. */
    public static final IProperty<Integer> TIME_OBSERVATION_WIDTH = new Property<Integer>(
            "de.cau.cs.kieler.papyrus.sequence.timeObservationWidth", 20);

    /** The offset between two nested areas. */
    public static final IProperty<Integer> CONTAINMENT_OFFSET = new Property<Integer>(
            "de.cau.cs.kieler.papyrus.sequence.containmentOffset", 5);

    /** The alignment of message labels. This property may be set by the user. */
    public static final IProperty<LabelAlignment> LABEL_ALIGNMENT = new Property<LabelAlignment>(
            "de.cau.cs.kieler.papyrus.sequence.labelAlignment", LabelAlignment.SOURCE_CENTER);
    
    /** The lifeline sorting strategy that should be used in the algorithm. May be set by the user. */
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

    
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Internal Properties

    /** The lifeline to which an element of the SGraph belongs. */
    public static final IProperty<SLifeline> BELONGS_TO_LIFELINE = new Property<SLifeline>(
            "de.cau.cs.kieler.papyrus.sequence.belongsToLifeline");

    /** The node in the layered graph that corresponds to a message. */
    public static final IProperty<LNode> LAYERED_NODE = new Property<LNode>(
            "de.cau.cs.kieler.papyrus.sequence.layeredNode");

    /** The KEdge that connects the comment to another element of the diagram. */
    public static final IProperty<KEdge> COMMENT_CONNECTION = new Property<KEdge>(
            "de.cau.cs.kieler.papyrus.sequence.commentConnection");
    
}
