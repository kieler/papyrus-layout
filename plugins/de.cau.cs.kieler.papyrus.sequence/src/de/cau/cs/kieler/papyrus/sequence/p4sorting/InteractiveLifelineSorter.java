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
package de.cau.cs.kieler.papyrus.sequence.p4sorting;

import java.util.List;

import org.eclipse.elk.core.util.IElkProgressMonitor;

import de.cau.cs.kieler.papyrus.sequence.ISequenceLayoutProcessor;
import de.cau.cs.kieler.papyrus.sequence.LayoutContext;
import de.cau.cs.kieler.papyrus.sequence.graph.SLifeline;

/**
 * Lifeline sorting algorithm that respects the given order of the lifelines. The lifelines are
 * numbered as they are ordered before.
 * 
 * @author grh
 * @kieler.design proposed grh
 * @kieler.rating proposed yellow grh
 */
public final class InteractiveLifelineSorter implements ISequenceLayoutProcessor {

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(final LayoutContext context, final IElkProgressMonitor progressMonitor) {
        progressMonitor.begin("Interactive lifeline sorting", 1);
        
        // Sort the lifelines by their x coordinates
        List<SLifeline> lifelines = context.sgraph.getLifelines();
        java.util.Collections.sort(lifelines);
        
        // Apply lifeline slots
        for (int i = 0; i < lifelines.size(); i++) {
            lifelines.get(i).setHorizontalSlot(i);
        }

        // Return the list of lifelines in the calculated order
        context.lifelineOrder = lifelines;
        
        progressMonitor.done();
    }

}
