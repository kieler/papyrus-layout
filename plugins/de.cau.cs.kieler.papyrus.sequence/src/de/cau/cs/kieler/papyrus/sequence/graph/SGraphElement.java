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
package de.cau.cs.kieler.papyrus.sequence.graph;

import org.eclipse.elk.graph.properties.MapPropertyHolder;

/**
 * Superclass for all {@link SGraph} elements. The extension of the {@link MapPropertyHolder}
 * enables the elements to hold properties.
 * 
 * @author grh
 * @kieler.design 2012-11-20 cds, msp
 * @kieler.rating yellow 2012-12-11 cds, ima
 */
public class SGraphElement extends MapPropertyHolder {
    private static final long serialVersionUID = -7980530866752118344L;
}
