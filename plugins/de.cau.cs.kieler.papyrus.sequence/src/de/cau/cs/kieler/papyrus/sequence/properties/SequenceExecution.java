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

import org.eclipse.elk.core.math.KVector;
import org.eclipse.elk.graph.ElkNode;

import com.google.common.collect.Lists;

/**
 * Data structure for execution specification elements in sequence diagrams. This class is not part
 * of the core data structure of this layout algorithm since it actually has to be attached to the
 * graph in the {@link InternalSequenceProperties#EXECUTIONS} property. Thus, while also being used
 * by the algorithm, this class has to be visible to the outside.
 * 
 * TODO: Add a generic type to replace Object in the code?
 * 
 * @author grh
 * @kieler.design proposed grh
 * @kieler.rating proposed yellow grh
 */
public final class SequenceExecution {
    
    /** The originating ElkNode of the execution. */
    private ElkNode origin;
    /** The type of the execution. */
    private SequenceExecutionType type = null;
    /** The list of connected messages. */
    private List<Object> messages = Lists.newArrayList();
    /** The size of the execution. */
    private KVector size = new KVector(0, -1);
    /** The position of the execution. */
    private KVector position = new KVector(0, 0);
    
    
    /**
     * Constructor that initializes the execution. The origin can be used to map the sequence execution
     * to an original ElkNode that the size and position will eventually be transferred to.
     * 
     * @param origin
     *            the origin this object is created for
     */
    public SequenceExecution(final ElkNode origin) {
        this.origin = origin;
    }

    
    /**
     * Get the size of the execution.
     * 
     * @return the KVector with the size
     */
    public KVector getSize() {
        return size;
    }

    /**
     * Get the position of the execution.
     * 
     * @return the KVector with the position
     */
    public KVector getPosition() {
        return position;
    }

    /**
     * Get the type of the execution.
     * 
     * @return the type
     */
    public SequenceExecutionType getType() {
        return type;
    }

    /**
     * Set the type of the execution.
     * 
     * @param type
     *            the new type
     */
    public void setType(final SequenceExecutionType type) {
        this.type = type;
    }

    /**
     * Get the list of messages that are connected to the execution.
     * 
     * @return the list of messages
     */
    public List<Object> getMessages() {
        return messages;
    }

    /**
     * Add a message to the list of connected messages.
     * 
     * @param message
     *            the new message
     */
    public void addMessage(final Object message) {
        this.messages.add(message);
    }

    /**
     * Get the origin ElkNode of the execution.
     * 
     * @return the origin
     */
    public ElkNode getOrigin() {
        return origin;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        return "Origin: " + origin.getClass().getSimpleName() + ", Messages: " + messages
                + ", Pos: (" + getPosition().x + "/" + getPosition().y + "), Size: (" + getSize().x + "/"
                + getSize().y + ")";
    }
}
