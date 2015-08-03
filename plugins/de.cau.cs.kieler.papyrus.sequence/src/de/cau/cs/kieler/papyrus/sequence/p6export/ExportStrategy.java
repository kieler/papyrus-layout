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
package de.cau.cs.kieler.papyrus.sequence.p6export;

/**
 * The available coordinate systems. The selected system influences how coordinates are computed.
 * 
 * @author cds
 * @kieler.design proposed grh
 * @kieler.rating proposed yellow grh
 */
public enum ExportStrategy {

    /**
     * All coordinates are to be interpreted in the KGraph coordinate system. With this strategy, the
     * computed layout can be correctly drawn by KLighD.
     */
    KGRAPH,
    
    /**
     * Coordinates are computed such that the Papyrus sequence diagram layouter can make sense of them.
     */
    PAPYRUS;
    

    /**
     * Returns the enumeration value related to the given ordinal.
     * 
     * @param i
     *            ordinal value
     * @return the related enumeration value
     */
    public static ExportStrategy valueOf(final int i) {
        return values()[i];
    }
    
}
