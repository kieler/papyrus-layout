/*
 * KIELER - Kiel Integrated Environment for Layout Eclipse RichClient
 *
 * http://www.informatik.uni-kiel.de/rtsys/kieler/
 * 
 * Copyright 2015 by
 * + Christian-Albrechts-University of Kiel
 *   + Department of Computer Science
 *     + Real-Time and Embedded Systems Group
 * 
 * This code is provided under the terms of the Eclipse Public License (EPL).
 * See the file epl-v10.html for the license text.
 */
package de.cau.cs.kieler.papyrus.sequence;

import java.util.List;

import de.cau.cs.kieler.kiml.klayoutdata.KLayoutData;
import de.cau.cs.kieler.klay.layered.graph.LGraph;
import de.cau.cs.kieler.papyrus.sequence.graph.SGraph;
import de.cau.cs.kieler.papyrus.sequence.graph.SLifeline;
import de.cau.cs.kieler.papyrus.sequence.p4sorting.LifelineSortingStrategy;
import de.cau.cs.kieler.papyrus.sequence.properties.LabelAlignment;
import de.cau.cs.kieler.papyrus.sequence.properties.SequenceDiagramProperties;

/**
 * A simple data holder class used to pass data about the layout process around to the different phases
 * of the layout algorithm.
 * 
 * @author cds
 */
public final class LayoutContext {
    // CHECKSTYLEOFF VisibilityModifier
    
    // Layout Graphs
    /** The {@link SGraph} to be laid out. */
    public SGraph sgraph;
    /** The {@link LGraph} created from the SGraph. */
    public LGraph lgraph;
    /** The order of lifelines as determined later in the algorithm. */
    public List<SLifeline> lifelineOrder;
    
    // Layout Settings
    
    /** The height of the lifeline's header. */
    public int lifelineHeader;
    /** The vertical position of lifelines. */
    public int lifelineYPos;
    /** The height of the header of combined fragments. */
    public int areaHeader;
    /** The offset between two nested areas. */
    public int containmentOffset;
    /** Border spacing. */
    public double borderSpacing;
    /** Horizontal spacing between two neighbored lifelines. */
    public double lifelineSpacing;
    /** Vertical spacing between two neighbored layers of messages. */
    public double messageSpacing;
    
    // Algorithm Config
    
    /** The lifeline sorting strategy. */
    public LifelineSortingStrategy sortingStrategy;
    /** Whether to include areas in the lifeline sorting process. Used by some sorters. */
    public boolean groupAreasWhenSorting;
    /** The label alignment strategy. */
    public LabelAlignment labelAlignment;
    
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
     * @param layoutData
     *            the layout data to initialize the new context object with.
     * @return initialized context object.
     */
    public static LayoutContext fromLayoutData(final KLayoutData layoutData) {
        LayoutContext context = new LayoutContext();
        
        context.borderSpacing = layoutData.getProperty(
                SequenceDiagramProperties.BORDER_SPACING).doubleValue();
        context.lifelineHeader = layoutData.getProperty(SequenceDiagramProperties.LIFELINE_HEADER);
        context.lifelineYPos = layoutData.getProperty(SequenceDiagramProperties.LIFELINE_Y_POS);
        context.areaHeader = layoutData.getProperty(SequenceDiagramProperties.AREA_HEADER);
        context.containmentOffset = layoutData.getProperty(SequenceDiagramProperties.CONTAINMENT_OFFSET);
        context.lifelineSpacing = layoutData.getProperty(
                SequenceDiagramProperties.LIFELINE_SPACING).doubleValue();
        context.messageSpacing = layoutData.getProperty(
                SequenceDiagramProperties.MESSAGE_SPACING).doubleValue();
        context.labelAlignment = layoutData.getProperty(SequenceDiagramProperties.LABEL_ALIGNMENT);
        context.sortingStrategy = layoutData.getProperty(SequenceDiagramProperties.LIFELINE_SORTING);
        context.groupAreasWhenSorting = layoutData.getProperty(SequenceDiagramProperties.GROUP_AREAS);
        
        return context;
    }
}