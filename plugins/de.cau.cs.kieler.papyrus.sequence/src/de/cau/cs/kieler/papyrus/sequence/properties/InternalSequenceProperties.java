/*
 * KIELER - Kiel Integrated Environment for Layout Eclipse RichClient
 *
 * http://www.informatik.uni-kiel.de/rtsys/kieler/
 * 
 * Copyright 2016 by
 * + Christian-Albrechts-University of Kiel
 *   + Department of Computer Science
 *     + Real-Time and Embedded Systems Group
 * 
 * This code is provided under the terms of the Eclipse Public License (EPL).
 * See the file epl-v10.html for the license text.
 */
package de.cau.cs.kieler.papyrus.sequence.properties;

import org.eclipse.elk.alg.layered.graph.LNode;
import org.eclipse.elk.graph.ElkEdge;
import org.eclipse.elk.graph.properties.IProperty;
import org.eclipse.elk.graph.properties.Property;

import de.cau.cs.kieler.papyrus.sequence.graph.SLifeline;

/**
 * Internal properties used by the layout algorithm.
 */
public final class InternalSequenceProperties {

    /** The lifeline to which an element of the SGraph belongs. */
    public static final IProperty<SLifeline> BELONGS_TO_LIFELINE = new Property<SLifeline>(
            "de.cau.cs.kieler.papyrus.sequence.belongsToLifeline");

    /** The node in the layered graph that corresponds to a message. */
    public static final IProperty<LNode> LAYERED_NODE = new Property<LNode>(
            "de.cau.cs.kieler.papyrus.sequence.layeredNode");

    /** The KEdge that connects the comment to another element of the diagram. */
    public static final IProperty<ElkEdge> COMMENT_CONNECTION = new Property<ElkEdge>(
            "de.cau.cs.kieler.papyrus.sequence.commentConnection");


    private InternalSequenceProperties() {
        // Hide the constructor
    }
}
