/*
 * KIELER - Kiel Integrated Environment for Layout Eclipse RichClient
 *
 * http://www.informatik.uni-kiel.de/rtsys/kieler/
 * 
 * Copyright 2015, 2016 by
 * + Christian-Albrechts-University of Kiel
 *   + Department of Computer Science
 *     + Real-Time and Embedded Systems Group
 * 
 * This code is provided under the terms of the Eclipse Public License (EPL).
 * See the file epl-v10.html for the license text.
 */
package de.cau.cs.kieler.papyrus.sequence;

import java.util.List;

import org.eclipse.elk.alg.layered.graph.LGraph;
import org.eclipse.elk.graph.ElkNode;

import de.cau.cs.kieler.papyrus.sequence.graph.SGraph;
import de.cau.cs.kieler.papyrus.sequence.graph.SLifeline;
import de.cau.cs.kieler.papyrus.sequence.p4sorting.LifelineSortingStrategy;
import de.cau.cs.kieler.papyrus.sequence.properties.CoordinateSystem;
import de.cau.cs.kieler.papyrus.sequence.properties.LabelAlignment;
import de.cau.cs.kieler.papyrus.sequence.properties.SequenceDiagramOptions;

/**
 * A simple data holder class used to pass data about the layout process around to the different phases
 * of the layout algorithm.
 * 
 * @author cds
 */
public final class LayoutContext {
    // CHECKSTYLEOFF VisibilityModifier
    
    // Layout Graphs
    
    /** The original ElkGraph the layout algorithm was called with. */
    public ElkNode elkgraph;
    /** The {@link SGraph} to be laid out. */
    public SGraph sgraph;
    /** The {@link LGraph} created from the SGraph. */
    public LGraph lgraph;
    /** The order of lifelines as determined later in the algorithm. */
    public List<SLifeline> lifelineOrder;
    
    
    // Layout Settings
    
    /** Border spacing. */
    public double borderSpacing;
    /** Vertical spacing between two neighbored layers of messages. */
    public double messageSpacing;
    /** Horizontal spacing between two neighbored lifelines. */
    public double lifelineSpacing;
    /** The vertical position of lifelines. */
    public double lifelineYPos;
    /** The height of the lifeline's header. */
    public double lifelineHeader;
    /** The height of the header of combined fragments. */
    public double areaHeader;
    /** The width of timing observations. */
    public double timeObservationWidth;
    /** The offset between two nested areas. */
    public double containmentOffset;
    /** The label alignment strategy. */
    public LabelAlignment labelAlignment;
    /** The lifeline sorting strategy. */
    public LifelineSortingStrategy sortingStrategy;
    /** Whether to include areas in the lifeline sorting process. Used by some sorters. */
    public boolean groupAreasWhenSorting;
    /** The coordinate system to use. */
    public CoordinateSystem coordinateSystem;
    
    // CHECKSTYLEON VisibilityModifier
    
    
    /**
     * Use {@link #fromLayoutData(KLayoutData)} to obtain a new instance.
     */
    private LayoutContext() {
        
    }
    
    /**
     * Creates a new instance initialized based on the given layout data. Should be called with the
     * layout data of the sequence diagram graph.
     * 
     * @param parentNode
     *            parent node of the graph that is to be laid out.
     * @return initialized context object.
     */
    public static LayoutContext fromLayoutData(final ElkNode parentNode) {
        LayoutContext context = new LayoutContext();
        
        context.elkgraph = parentNode;

        context.borderSpacing =
                parentNode.getProperty(SequenceDiagramOptions.SPACING_COMPONENT_COMPONENT);
        context.messageSpacing = parentNode.getProperty(SequenceDiagramOptions.MESSAGE_SPACING);
        context.lifelineSpacing = parentNode.getProperty(SequenceDiagramOptions.LIFELINE_SPACING);
        context.lifelineYPos = parentNode.getProperty(SequenceDiagramOptions.LIFELINE_Y_POS);
        context.lifelineHeader = parentNode.getProperty(
                SequenceDiagramOptions.LIFELINE_HEADER_HEIGHT);
        context.areaHeader = parentNode.getProperty(SequenceDiagramOptions.AREA_HEADER_HEIGHT);
        context.timeObservationWidth = parentNode.getProperty(
                SequenceDiagramOptions.TIME_OBSERVATION_WIDTH);
        context.containmentOffset = parentNode.getProperty(
                SequenceDiagramOptions.CONTAINMENT_OFFSET);
        context.labelAlignment = parentNode.getProperty(SequenceDiagramOptions.LABEL_ALIGNMENT);
        context.sortingStrategy = parentNode.getProperty(
                SequenceDiagramOptions.LIFELINE_SORTING_STRATEGY);
        context.groupAreasWhenSorting = parentNode.getProperty(SequenceDiagramOptions.GROUP_AREAS);
        context.coordinateSystem = parentNode.getProperty(SequenceDiagramOptions.COORDINATE_SYSTEM);
        
        return context;
    }
}