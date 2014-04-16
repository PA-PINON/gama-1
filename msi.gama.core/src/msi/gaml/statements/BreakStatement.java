/*********************************************************************************************
 * 
 * 
 * 'BreakStatement.java', in plugin 'msi.gama.core', is part of the source code of the
 * GAMA modeling and simulation platform.
 * (c) 2007-2014 UMI 209 UMMISCO IRD/UPMC & Partners
 * 
 * Visit https://code.google.com/p/gama-platform/ for license information and developers contact.
 * 
 * 
 **********************************************************************************************/
package msi.gaml.statements;

import msi.gama.common.interfaces.IKeyword;
import msi.gama.precompiler.GamlAnnotations.inside;
import msi.gama.precompiler.GamlAnnotations.symbol;
import msi.gama.precompiler.*;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gaml.descriptions.IDescription;

/**
 * The class BreakCommand.
 * 
 * @author drogoul
 * @since 22 avr. 2012
 * 
 */
@symbol(name = IKeyword.BREAK, kind = ISymbolKind.SINGLE_STATEMENT, with_sequence = false)
@inside(kinds = ISymbolKind.SEQUENCE_STATEMENT)
public class BreakStatement extends AbstractStatement {

	/**
	 * @param desc
	 */
	public BreakStatement(final IDescription desc) {
		super(desc);
	}

	/**
	 * @see msi.gaml.commands.AbstractCommand#privateExecuteIn(msi.gama.runtime.IScope)
	 */
	@Override
	protected Object privateExecuteIn(final IScope scope) throws GamaRuntimeException {
		scope.interruptLoop();
		return null; // How to return the last object ??
	}

}
