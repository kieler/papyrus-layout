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
package de.cau.cs.kieler.papyrus.sequence.properties;

/**
 * The type of sequence execution.
 * 
 * @author dja
 */
public enum SequenceExecutionType {
    /** It's an execution. */
    EXECUTION,
    /** It's a duration.*/
    DURATION,
    /** It's a time constraint. */
    TIME_CONSTRAINT;
}
