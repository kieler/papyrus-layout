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
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import de.cau.cs.kieler.core.kgraph.KNode;
import de.cau.cs.kieler.core.math.KVector;

/**
 * Data structure for area-like elements in sequence diagrams. This class is not part of the core data
 * structure of this layout algorithm since it actually has to be attached to the graph in the
 * {@link SequenceDiagramProperties#AREAS} property. Thus, while also being used by the algorithm, this
 * class has to be visible to the outside.
 * 
 * This data structure is used differently depending on whether the algorithm operates in KGraph or in
 * Papyrus mode.
 * 
 * <h3>Papyrus Mode</h3>
 * <p>
 * The algorithm expects the origin, size, and position of sequence areas to be set.
 * </p>
 * 
 * <h3>KGraph Mode</h3>
 * <p>
 * The algorithm expects the layout node ID to be set. Further, it expects the list of messages and
 * lifelines to be filled with the element IDs of the edges and nodes that represent the messages
 * contained in this sequence area and the affected lifelines. If the list of messages is empty, the
 * next message is expected to be the element ID of the message this area should be placed above.
 * </p>
 * 
 * @author grh
 * @kieler.design proposed grh
 * @kieler.rating proposed yellow grh
 */
public final class SequenceArea {
    /** The layout graph node that represents this sequence area. */
    private KNode layoutNode;
    /** The element ID of the node in the layout graph that represents this sequence area. */
    private Integer layoutNodeID;
    /** The list of messages contained in the area. */
    private Set<Object> messages = Sets.newLinkedHashSet();
    /** The list of affected lifelines. */
    private Set<Object> lifelines = Sets.newLinkedHashSet();
    /** The list of subareas (in case of a combined fragment). */
    private List<SequenceArea> subAreas = Lists.newArrayList();
    /** The list of areas that are contained in this area. */
    private List<SequenceArea> containedAreas = Lists.newArrayList();
    /** The message, that is nearest to the area if the area is empty. */
    private Object nextMessage;
    /** The size of the area. */
    private KVector size = new KVector();
    /** The position of the area. */
    private KVector position = new KVector();
    
    
    /**
     * Creates a SequenceArea for the given node in the layout graph. This creation method should be
     * used in Papyrus mode.
     * 
     * @param node the node in the layout graph that represents the sequence area.
     * @return the created sequence area.
     */
    public static SequenceArea forLayoutNode(final KNode node) {
        SequenceArea area = new SequenceArea();
        area.layoutNode = node;
        return area;
    }

    /**
     * Creates a SequenceArea represented by the node with the given element ID in the layout graph.
     * This creation method should be used in KGraph mode.
     * 
     * @param id element id of the node in the layout graph that represents the sequence area.
     * @return the created sequence area.
     */
    public static SequenceArea forElementId(final int id) {
        SequenceArea area = new SequenceArea();
        area.layoutNodeID = id;
        return area;
    }
    

    /**
     * Get the size of the area.
     * 
     * @return the KVector with the size
     */
    public KVector getSize() {
        return size;
    }

    /**
     * Get the position of the area.
     * 
     * @return the KVector with the position
     */
    public KVector getPosition() {
        return position;
    }

    /**
     * Returns the node in the layout graph that represents this area.
     * 
     * @return the layout node.
     */
    public KNode getLayoutNode() {
        return layoutNode;
    }

    /**
     * Sets the node in the layout graph that represents this area.
     * 
     * @param node the layout node.
     */
    public void setLayoutNode(final KNode node) {
        layoutNode = node;
    }
    
    /**
     * Returns the element ID of the node in the layout graph that represents this area.
     * 
     * @return the element ID.
     */
    public int getLayoutNodeElementId() {
        return layoutNodeID;
    }

    /**
     * Get the list of messages that are covered by the area.
     * 
     * @return the list of messages
     */
    public Set<Object> getMessages() {
        return messages;
    }

    /**
     * Get the list of lifelines that are (partly) covered by the area.
     * 
     * @return the list of lifelines
     */
    public Set<Object> getLifelines() {
        return lifelines;
    }

    /**
     * Get the list of sub-areas (for combined fragments) of the area.
     * 
     * @return the list of sub-areas
     */
    public List<SequenceArea> getSubAreas() {
        return subAreas;
    }

    /**
     * Get the list of areas, that are completely covered by this area.
     * 
     * @return the list of contained areas
     */
    public List<SequenceArea> getContainedAreas() {
        return containedAreas;
    }

    /**
     * Get the message that is next to the area if the area does not contain any messages.
     * 
     * @return the message next to the area
     */
    public Object getNextMessage() {
        return nextMessage;
    }

    /**
     * Set the message that is next to the area.
     * 
     * @param nextMessage
     *            the new message that is next to the area
     */
    public void setNextMessage(final Object nextMessage) {
        this.nextMessage = nextMessage;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        String subareas = "\nwith " + subAreas.size() + " Subareas: " + subAreas;
        if (subAreas.size() == 0) {
            subareas = "";
        }
        return "Area " + layoutNode + "\nwith " + messages.size() + " Messages: " + messages + "\nand "
                + lifelines.size() + " Lifelines: " + lifelines + "\nat (" + getPosition().x + "/"
                + getPosition().y + ") + (" + getSize().x + "/" + getSize().y + ")" + subareas;
    }
}
