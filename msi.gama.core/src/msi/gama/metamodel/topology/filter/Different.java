/*
 * GAMA - V1.4 http://gama-platform.googlecode.com
 * 
 * (c) 2007-2011 UMI 209 UMMISCO IRD/UPMC & Partners (see below)
 * 
 * Developers :
 * 
 * - Alexis Drogoul, UMI 209 UMMISCO, IRD/UPMC (Kernel, Metamodel, GAML), 2007-2012
 * - Vo Duc An, UMI 209 UMMISCO, IRD/UPMC (SWT, multi-level architecture), 2008-2012
 * - Patrick Taillandier, UMR 6228 IDEES, CNRS/Univ. Rouen (Batch, GeoTools & JTS), 2009-2012
 * - Beno�t Gaudou, UMR 5505 IRIT, CNRS/Univ. Toulouse 1 (Documentation, Tests), 2010-2012
 * - Phan Huy Cuong, DREAM team, Univ. Can Tho (XText-based GAML), 2012
 * - Pierrick Koch, UMI 209 UMMISCO, IRD/UPMC (XText-based GAML), 2010-2011
 * - Romain Lavaud, UMI 209 UMMISCO, IRD/UPMC (RCP environment), 2010
 * - Francois Sempe, UMI 209 UMMISCO, IRD/UPMC (EMF model, Batch), 2007-2009
 * - Edouard Amouroux, UMI 209 UMMISCO, IRD/UPMC (C++ initial porting), 2007-2008
 * - Chu Thanh Quang, UMI 209 UMMISCO, IRD/UPMC (OpenMap integration), 2007-2008
 */
package msi.gama.metamodel.topology.filter;

import java.util.*;
import msi.gama.metamodel.shape.*;
import msi.gama.runtime.IScope;
import msi.gaml.species.ISpecies;

public class Different implements IAgentFilter {

	private static final Different instance = new Different();

	public static Different with() {
		return instance;
	}

	@Override
	public boolean accept(final IShape source, final IShape a) {
		return a.getGeometry() != source.getGeometry();
	}

	@Override
	public boolean filterSpecies(final ISpecies s) {
		return false;
	}

	@Override
	public boolean accept(final ILocation source, final IShape a) {
		return !a.getLocation().equals(source);
	}

	/**
	 * @see msi.gama.metamodel.topology.filter.IAgentFilter#getShapes()
	 */
	@Override
	public Collection<? extends IShape> getShapes(IScope scope) {
		return Collections.EMPTY_LIST;
	}

	@Override
	public ISpecies speciesFiltered() {
		return null;
	}

}