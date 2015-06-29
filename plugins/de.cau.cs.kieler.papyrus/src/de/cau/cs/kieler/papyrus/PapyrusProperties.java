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
package de.cau.cs.kieler.papyrus;

import de.cau.cs.kieler.core.properties.IProperty;
import de.cau.cs.kieler.core.properties.Property;
import de.cau.cs.kieler.papyrus.sequence.properties.SequenceExecution;

/**
 * Properties that are necessary for the sequence diagram layout.
 * 
 * @author grh
 * @kieler.design proposed grh
 * @kieler.rating proposed yellow grh
 */
public final class PapyrusProperties {
    private PapyrusProperties() {
        // Hide constructor
    }

    /** A single execution specification. */
    public static final IProperty<SequenceExecution> EXECUTION = new Property<SequenceExecution>(
            "de.cau.cs.kieler.papyrus.executionSpecification");
}
