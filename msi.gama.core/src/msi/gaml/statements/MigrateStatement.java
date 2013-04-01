package msi.gaml.statements;

import java.util.List;
import msi.gama.common.interfaces.IKeyword;
import msi.gama.metamodel.agent.IAgent;
import msi.gama.precompiler.GamlAnnotations.facet;
import msi.gama.precompiler.GamlAnnotations.facets;
import msi.gama.precompiler.GamlAnnotations.inside;
import msi.gama.precompiler.GamlAnnotations.symbol;
import msi.gama.precompiler.*;
import msi.gama.runtime.IScope;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gama.util.*;
import msi.gaml.compilation.ISymbol;
import msi.gaml.descriptions.*;
import msi.gaml.species.ISpecies;
import msi.gaml.types.IType;

/**
 * This command permits agents to migrate from one population/species to another population/species
 * and stay in the same host after the migration.
 * 
 * It has two mandatory parameters:
 * + source: can be an agent, a list of agents, a agent's population to be migrated
 * + target: target species/population that source agent(s) migrate to.
 * 
 * Species of source agents and target species respect the following constraints:
 * + they are "peer" species (sharing the same direct macro-species)
 * + they have sub-species vs. parent-species relationship.
 */
@symbol(name = { IKeyword.MIGRATE }, kind = ISymbolKind.SEQUENCE_STATEMENT, with_sequence = true, remote_context = true)
@inside(kinds = { ISymbolKind.BEHAVIOR, ISymbolKind.SEQUENCE_STATEMENT })
@facets(value = {
	@facet(name = IKeyword.SOURCE, type = IType.SPECIES_STR, optional = false), // workaround
	@facet(name = IKeyword.TARGET, type = IType.SPECIES_STR, optional = false),
	@facet(name = IKeyword.RETURNS, type = IType.NEW_TEMP_ID, optional = true) }, omissible = IKeyword.SOURCE)
public class MigrateStatement extends AbstractStatementSequence {

	// private IExpression source;
	private final String source;
	private final String target;
	private final String returnString;
	private AbstractStatementSequence sequence = null;

	public MigrateStatement(final IDescription desc) {
		super(desc);

		source = getLiteral(IKeyword.SOURCE);

		target = getLiteral(IKeyword.TARGET);
		verifyTarget();

		returnString = getLiteral(IKeyword.RETURNS);
	}

	private void verifyTarget() {
		SpeciesDescription species = this.getDescription().getSpeciesContext();
		TypeDescription targetSpeciesDesc = species.getMicroSpecies(target);
		if ( targetSpeciesDesc == null ) { throw new GamaRuntimeException(target +
			" is not a micro-species of " + species.getName()); }
	}

	@Override
	public void setChildren(final List<? extends ISymbol> com) {
		sequence = new AbstractStatementSequence(description);
		sequence.setName("commands of " + getName());
		sequence.setChildren(com);
	}

	@Override
	public Object privateExecuteIn(final IScope stack) throws GamaRuntimeException {
		IAgent executor = stack.getAgentScope();
		IList<IAgent> immigrants = new GamaList<IAgent>();

		ISpecies targetMicroSpecies = executor.getSpecies().getMicroSpecies(target);
		ISpecies sourceMicroSpecies = executor.getSpecies().getMicroSpecies(source);

		immigrants.addAll(executor
			.migrateMicroAgents(stack, sourceMicroSpecies, targetMicroSpecies));

		/*
		 * Object immigrantCandidates = source.value(stack);
		 * 
		 * if (immigrantCandidates instanceof ISpecies) {
		 * immigrants.addAll(executor.migrateMicroAgent((ISpecies) immigrantCandidates,
		 * targetMicroSpecies));
		 * } else if (immigrantCandidates instanceof IList) {
		 * immigrants.addAll(executor.migrateMicroAgents((IList) immigrantCandidates,
		 * targetMicroSpecies));
		 * } else if (immigrantCandidates instanceof IAgent) {
		 * IList<IAgent> m = new GamaList<IAgent>();
		 * m.add((IAgent) immigrantCandidates);
		 * immigrants.addAll(executor.migrateMicroAgents(m, targetMicroSpecies));
		 * }
		 */

		if ( returnString != null ) {
			stack.setVarValue(returnString, immigrants);
		}

		return null;
	}
}
