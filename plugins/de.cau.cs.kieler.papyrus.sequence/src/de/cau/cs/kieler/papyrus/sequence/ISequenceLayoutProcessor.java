/*
 * KIELER - Kiel Integrated Environment for Layout Eclipse RichClient
 *
 * http://www.informatik.uni-kiel.de/rtsys/kieler/
 * 
 * Copyright 2010 by
 * + Kiel University
 *   + Department of Computer Science
 *     + Real-Time and Embedded Systems Group
 * 
 * This code is provided under the terms of the Eclipse Public License (EPL).
 * See the file epl-v10.html for the license text.
 */
package de.cau.cs.kieler.papyrus.sequence;

import de.cau.cs.kieler.core.alg.IKielerProgressMonitor;

/**
 * A layout processor processes a {@link LayoutContext}, performing layout related tasks on it. This
 * basically models one step in the list of steps required to layout a sequence diagram.
 *
 * @see SequenceDiagramLayoutProvider
 * @author cds
 */
public interface ISequenceLayoutProcessor {
    
    /**
     * Performs the processor's work on the given graph.
     * 
     * @param context
     *            the layout context that contains all relevant information for the current layout run.
     * @param progressMonitor
     *            a progress monitor to track algorithm execution.
     */
    void process(LayoutContext context, IKielerProgressMonitor progressMonitor);
    
}
