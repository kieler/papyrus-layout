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
package de.cau.cs.kieler.papyrus.sequence.p3layering;

import org.eclipse.elk.alg.layered.p2layers.NetworkSimplexLayerer;
import org.eclipse.elk.core.util.IElkProgressMonitor;

import de.cau.cs.kieler.papyrus.sequence.ISequenceLayoutProcessor;
import de.cau.cs.kieler.papyrus.sequence.LayoutContext;

/**
 * Uses KLay Layered's {@link NetworkSimplexLayerer} to compute a layering for the messages in the
 * LGraph representation of a sequence diagram. This simply delegates to the network simplex layerer,
 * but needs to be in its own class because the network simplex layerer doesn't implement out layout
 * processor interface.
 * 
 * @author cds
 */
public final class MessageLayerer implements ISequenceLayoutProcessor {

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(final LayoutContext context, final IElkProgressMonitor progressMonitor) {
        NetworkSimplexLayerer layerer = new NetworkSimplexLayerer();
        layerer.process(context.lgraph, progressMonitor);
    }

}
