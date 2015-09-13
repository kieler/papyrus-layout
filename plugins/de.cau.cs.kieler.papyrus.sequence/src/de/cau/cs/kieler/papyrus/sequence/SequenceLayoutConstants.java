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
package de.cau.cs.kieler.papyrus.sequence;

/**
 * Keeps a bunch of constants used throughout the algorithm. Most of this should probably be phased out
 * and replaced by proper layout options.
 * 
 * @author cds
 */
public final class SequenceLayoutConstants {
    
    // TODO All these constants should really disappear or at least be more sensibly named
    
    /** Constant that is needed to calculate some offsets. */
    public static final int TEN = 10;
    /** Constant that is needed to calculate some offsets. */
    public static final int TWENTY = 20;
    /** Constant that is needed to calculate some offsets. */
    public static final int FOURTY = 40;
    /** The vertical spacing between message and label. */
    public static final int LABELSPACING = 5;
    /** The horizontal margin for message labels. */
    public static final int LABELMARGIN = 10;
    /** The minimum height of an execution. */
    public static final int MIN_EXECUTION_HEIGHT = 20;
    /** The width of executions. This could well be turned into a layout option at some point. */
    public static final int EXECUCTION_WIDTH = 16;
    

    /**
     * No instantiation.
     */
    private SequenceLayoutConstants() {
    }

}
