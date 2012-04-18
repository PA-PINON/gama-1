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
package msi.gaml.factories;

import static msi.gama.common.interfaces.IKeyword.*;
import java.lang.reflect.Constructor;
import java.util.*;
import msi.gama.common.interfaces.*;
import msi.gama.precompiler.GamlAnnotations.base;
import msi.gama.precompiler.GamlAnnotations.combination;
import msi.gama.precompiler.GamlAnnotations.facet;
import msi.gama.precompiler.GamlAnnotations.facets;
import msi.gama.precompiler.GamlAnnotations.handles;
import msi.gama.precompiler.GamlAnnotations.inside;
import msi.gama.precompiler.GamlAnnotations.no_scope;
import msi.gama.precompiler.GamlAnnotations.remote_context;
import msi.gama.precompiler.GamlAnnotations.symbol;
import msi.gama.precompiler.GamlAnnotations.uses;
import msi.gama.precompiler.GamlAnnotations.with_args;
import msi.gama.precompiler.GamlAnnotations.with_sequence;
import msi.gama.runtime.GAMA;
import msi.gama.runtime.exceptions.GamaRuntimeException;
import msi.gama.util.GamaList;
import msi.gaml.commands.*;
import msi.gaml.compilation.*;
import msi.gaml.descriptions.*;
import msi.gaml.descriptions.SymbolMetaDescription.FacetMetaDescription;
import msi.gaml.expressions.*;
import msi.gaml.types.*;

/**
 * Written by Alexis Drogoul Modified on 11 mai 2010
 * 
 * @todo Description
 * 
 */
@handles({ ISymbolKind.ENVIRONMENT, ISymbolKind.ABSTRACT_SECTION })
public class SymbolFactory implements ISymbolFactory {

	public static List<String> valueFacets = Arrays.asList(VALUE, INIT, FUNCTION, UPDATE, MIN, MAX);

	protected Map<String, SymbolMetaDescription> registeredSymbols = new HashMap();

	protected final Set<ISymbolFactory> availableFactories = new HashSet();
	protected final Map<String, ISymbolFactory> registeredFactories = new HashMap();

	public SymbolFactory() {
		registerAnnotatedFactories();
		registerAnnotatedSymbols();
	}

	@Override
	public IExpressionFactory getExpressionFactory() {
		return GAMA.getExpressionFactory();
	}

	private void registerAnnotatedFactories() {
		uses annot = getClass().getAnnotation(uses.class);
		if ( annot == null ) { return; }
		for ( int kind : annot.value() ) {
			Class c = GamlCompiler.getFactoriesByKind(kind);
			Constructor cc = null;
			try {
				cc = c.getConstructor();
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
			if ( cc == null ) { return; }
			SymbolFactory fact;
			try {
				fact = (SymbolFactory) cc.newInstance();
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
			registerFactory(fact);
		}
	}

	protected void registerAnnotatedSymbols() {
		handles annot = getClass().getAnnotation(handles.class);
		if ( annot == null ) { return; }
		for ( int c : annot.value() ) {
			List<Class> classes = GamlCompiler.getClassesByKind().get(c);
			for ( Class clazz : classes ) {
				register(clazz);
			}
		}
	}

	@Override
	public Set<String> getKeywords() {
		return new HashSet(registeredSymbols.keySet());
		// Necessary to copy, since this list can be modified dynamically (esp. by SpeciesFactory)
	}

	public void registerFactory(final ISymbolFactory f) {
		availableFactories.add(f);
		// Does a pre-registration of the keywords
		for ( String s : f.getKeywords() ) {
			registeredFactories.put(s, f);
		}

		// OutputManager.debug(this.getClass().getSimpleName() + " registers factory " +
		// f.getClass().getSimpleName() + " for keywords " + f.getKeywords());

	}

	public void register(final Class c) {

		Class baseClass = null;
		String omissible = null;
		symbol sym = (symbol) c.getAnnotation(symbol.class);
		int sKind = sym.kind();
		// boolean isVariable = sym.kind() == ISymbolKind.VARIABLE;
		boolean canHaveArgs = c.getAnnotation(with_args.class) != null;
		boolean canHaveSequence = c.getAnnotation(with_sequence.class) != null;
		boolean doesNotHaveScope = c.getAnnotation(no_scope.class) != null;
		boolean isRemoteContext = c.getAnnotation(remote_context.class) != null;
		if ( c.getAnnotation(base.class) != null ) {
			baseClass = ((base) c.getAnnotation(base.class)).value();
		}
		List<String> keywords = Arrays.asList(sym.name());
		List<facet> facets = new ArrayList();
		List<combination> combinations = new ArrayList();
		List<String> contexts = new ArrayList();
		facets ff = (facets) c.getAnnotation(facets.class);
		if ( ff != null ) {
			facets = Arrays.asList(ff.value());
			combinations = Arrays.asList(ff.combinations());
			omissible = ff.omissible();

		}
		if ( c.getAnnotation(inside.class) != null ) {
			inside parents = (inside) c.getAnnotation(inside.class);
			for ( String p : parents.symbols() ) {
				contexts.add(p);
			}
			for ( int kind : parents.kinds() ) {
				List<Class> classes = GamlCompiler.getClassesByKind().get(kind);
				for ( Class clazz : classes ) {
					symbol names = (symbol) clazz.getAnnotation(symbol.class);
					if ( names != null ) {
						for ( String name : names.name() ) {
							contexts.add(name);
						}
					}
				}
			}
		}
		contexts = new ArrayList(new HashSet(contexts));

		for ( String k : keywords ) {
			// try {
			if ( sKind != ISymbolKind.VARIABLE ) {
				SymbolMetaDescription.nonVariableStatements.add(k);
			}
			registeredSymbols.put(k, new SymbolMetaDescription(c, baseClass, k, canHaveSequence,
				canHaveArgs, sKind, doesNotHaveScope, facets, omissible, combinations, contexts,
				isRemoteContext));
			// } catch (GamlException e) {
			// e.addContext("In compiling the meta description of: " + k);
			// e.printStackTrace();
			// }
		}
	}

	@Override
	public SymbolMetaDescription getMetaDescriptionFor(final IDescription context,
		final String keyword) {
		SymbolMetaDescription md = registeredSymbols.get(keyword);
		String upper = context == null ? null : context.getKeyword();
		if ( md == null ) {
			ISymbolFactory f = chooseFactoryFor(keyword, upper);
			if ( f == null ) {
				if ( context != null ) {
					context.flagError("Unknown symbol " + keyword);
				}
				return null;
			}
			return f.getMetaDescriptionFor(context, keyword);
		}
		return md;
	}

	@Override
	public String getOmissibleFacetForSymbol(final String keyword) {
		SymbolMetaDescription md = getMetaDescriptionFor(null, keyword);
		return md == null ? IKeyword.NAME /* by default */: md.getOmissible();

	}

	protected String getKeyword(final ISyntacticElement cur) {
		return cur.getKeyword();
	}

	/**
	 * Creates a semantic description based on a source element, a super-description, and a --
	 * possibly null -- list of children. In this method, the children of the source element are not
	 * considered, so if "children" is null or empty, the description is created without children.
	 * @see msi.gaml.factories.ISymbolFactory#createDescription(msi.gama.common.interfaces.ISyntacticElement,
	 *      msi.gaml.descriptions.IDescription, java.util.List)
	 */
	@Override
	public IDescription createDescription(final ISyntacticElement source,
		final IDescription superDesc, final List<IDescription> children) {
		Facets facets = source.getFacets();
		String keyword = getKeyword(source);
		ISymbolFactory f = chooseFactoryFor(keyword, null);
		if ( f == null ) {
			superDesc.flagError("Impossible to parse keyword " + keyword, source);
			return null;
		}
		if ( f != this ) { return f.createDescription(source, superDesc, children); }
		List<IDescription> commandList = children == null ? new ArrayList() : children;
		SymbolMetaDescription md = getMetaDescriptionFor(superDesc, keyword);
		md.verifyFacets(source, facets, superDesc);
		return buildDescription(source, keyword, commandList, superDesc, md);
	}

	/**
	 * Creates a semantic description based on a source element and a super-description. The
	 * children of the source element are used as a basis for building, recursively, the semantic
	 * tree.
	 * @see msi.gaml.factories.ISymbolFactory#createDescriptionRecursively(msi.gama.common.interfaces.ISyntacticElement,
	 *      msi.gaml.descriptions.IDescription)
	 */
	@Override
	public IDescription createDescriptionRecursively(final ISyntacticElement source,
		final IDescription superDesc) {
		if ( source == null ) { return null; }
		String keyword = getKeyword(source);

		String context = null;
		if ( superDesc != null ) {
			context = superDesc.getKeyword();
		}
		ISymbolFactory f = chooseFactoryFor(keyword, context);
		if ( f == null ) {
			if ( superDesc != null ) {
				superDesc.flagError("Impossible to parse keyword " + keyword, source);
			}
			return null;
		}
		if ( f != this ) { return f.createDescriptionRecursively(source, superDesc); }

		SymbolMetaDescription md = getMetaDescriptionFor(superDesc, keyword);
		Facets facets = source.getFacets();

		if ( md != null ) {
			md.verifyFacets(source, facets, superDesc);
		}
		List<IDescription> children = new ArrayList();

		for ( ISyntacticElement e : source.getChildren() ) {
			// Instead of having to consider this specific case, find a better solution.
			if ( !source.getKeyword().equals(IKeyword.SPECIES) ) {
				children.add(createDescriptionRecursively(e, superDesc));
			} else if ( source.hasParent(IKeyword.DISPLAY) || source.hasParent(IKeyword.SPECIES) ) {
				// "species" declared in "display" or "species" section
				children.add(createDescriptionRecursively(e, superDesc));
			}
		}

		return buildDescription(source, keyword, children, superDesc, md);
	}

	@Override
	public boolean handlesKeyword(final String keyword) {
		return registeredSymbols.containsKey(keyword);
	}

	@Override
	public ISymbolFactory chooseFactoryFor(final String keyword) {
		if ( handlesKeyword(keyword) ) { return this; }
		ISymbolFactory fact = registeredFactories.get(keyword);
		if ( fact != null ) { return fact; }
		for ( ISymbolFactory f : availableFactories ) {
			if ( f.handlesKeyword(keyword) ) {
				registeredFactories.put(keyword, f);
				return f;
			}
		}
		for ( ISymbolFactory f : availableFactories ) {
			ISymbolFactory f2 = f.chooseFactoryFor(keyword);
			if ( f2 != null ) {
				registeredFactories.put(keyword, f2);
				return f2;
			}
		}
		return null;
	}

	protected ISymbolFactory chooseFactoryFor(final String keyword, final String context) {
		ISymbolFactory contextFactory = context != null ? chooseFactoryFor(context) : this;
		if ( contextFactory == null ) {
			contextFactory = this;
		}
		return contextFactory.chooseFactoryFor(keyword);
	}

	protected IDescription buildDescription(final ISyntacticElement source, final String keyword,
		final List<IDescription> children, final IDescription superDesc,
		final SymbolMetaDescription md) {
		return new SymbolDescription(keyword, superDesc, children, source, md);
	}

	@Override
	public final ISymbol compileDescription(final IDescription desc) {
		IDescription superDesc = desc.getSuperDescription();
		ISymbolFactory f =
			chooseFactoryFor(desc.getKeyword(), superDesc == null ? null : superDesc.getKeyword());
		if ( f == null ) {
			desc.flagError("Impossible to compile keyword " + desc.getKeyword());
			return null;
		}
		if ( f != this ) { return f.compileDescription(desc); }
		return privateCompile(desc);
	}

	@Override
	public final void validateDescription(final IDescription desc) {
		IDescription superDesc = desc.getSuperDescription();
		ISymbolFactory f =
			chooseFactoryFor(desc.getKeyword(), superDesc == null ? null : superDesc.getKeyword());
		if ( f == null ) {
			desc.flagError("Impossible to validate keyword " + desc.getKeyword());
			return;
		}
		if ( f != this ) {
			f.validateDescription(desc);
			return;
		}
		privateValidate(desc);
	}

	protected void privateValidate(final IDescription desc) {
		SymbolMetaDescription md = desc.getMeta();
		if ( md == null ) { return; }
		Facets rawFacets = desc.getFacets();
		// Validation of the facets (through their compilation)
		rawFacets.putAsLabel(IKeyword.KEYWORD, desc.getKeyword());
		for ( String s : rawFacets.keySet() ) {
			compileFacet(s, desc);
		}
		verifyFacetsType(desc);
		if ( md.hasSequence() && !desc.getKeyword().equals(IKeyword.PRIMITIVE) ) {
			if ( md.isRemoteContext() ) {
				desc.copyTempsAbove();
			}
			privateValidateChildren(desc);
		}
	}

	protected void privateValidateChildren(final IDescription desc) {
		for ( IDescription sd : desc.getChildren() ) {
			validateDescription(sd);
		}
	}

	protected void compileFacet(final String tag, final IDescription sd) {
		try {
			IExpressionDescription ed = sd.getFacets().get(tag);
			if ( ed == null ) { return; }
			ed.compile(sd, getExpressionFactory());
		} catch (GamaRuntimeException e) {
			e.printStackTrace();
		}
	}

	protected final ISymbol privateCompile(final IDescription desc) {
		SymbolMetaDescription md = desc.getMeta();
		if ( md == null ) { return null; }
		Facets rawFacets = desc.getFacets();
		// Addition of a facet to keep track of the keyword
		rawFacets.putAsLabel(IKeyword.KEYWORD, desc.getKeyword());
		for ( String s : rawFacets.keySet() ) {
			compileFacet(s, desc);
		}
		ISymbol cs = md.getConstructor().create(desc);
		if ( cs == null ) { return null; }
		if ( md.hasArgs() ) {
			((ICommand.WithArgs) cs).setFormalArgs(privateCompileArgs((CommandDescription) desc));
		}
		if ( md.hasSequence() && !desc.getKeyword().equals(IKeyword.PRIMITIVE) ) {
			if ( md.isRemoteContext() ) {
				desc.copyTempsAbove();
			}
			cs.setChildren(privateCompileChildren(desc));
		}
		return cs;

	}

	/**
	 * @param desc
	 * @return
	 */
	protected Arguments privateCompileArgs(final CommandDescription desc) {
		return new Arguments();
	}

	protected List<ISymbol> privateCompileChildren(final IDescription desc) {
		List<ISymbol> lce = new ArrayList();
		for ( IDescription sd : desc.getChildren() ) {
			ISymbol s = compileDescription(sd);
			if ( s != null ) {
				lce.add(s);
			}
		}
		return lce;
	}

	/*
	 * Verification done after the facets have been compiled
	 */

	protected void verifyFacetsType(final IDescription desc) {
		SymbolMetaDescription smd = desc.getMeta();
		ModelDescription md = desc.getModelDescription();
		TypesManager tm = md.getTypesManager();
		for ( Map.Entry<String, IExpressionDescription> entry : desc.getFacets().entrySet() ) {
			String facetName = entry.getKey();
			IExpression expr = entry.getValue().getExpression();
			if ( expr != null ) {
				verifyFacetType(desc, facetName, expr, smd, md, tm);
			}
		}
	}

	protected void verifyFacetType(final IDescription desc, final String facet,
		final IExpression expr, final SymbolMetaDescription smd, final ModelDescription md,
		final TypesManager tm) {
		FacetMetaDescription fmd = smd.getPossibleFacets().get(facet);
		if ( fmd == null ) { return; }

		// We have a multi-valued facet
		if ( fmd.values.length > 0 ) {
			verifyFacetIsInValues(desc, facet, expr, fmd.values);
			return;
		}
		// The facet is supposed to be a type (IType.TYPE_ID)
		List<String> types = new GamaList(fmd.types);
		if ( types.contains(IType.TYPE_ID) ) {
			verifyFacetIsAType(desc, facet, expr, tm);
			return;
		}
		if ( valueFacets.contains(facet) ) {
			String type = desc.getFacets().getLabel(TYPE);
			if ( type != null ) {
				types.add(type);
			}
		}
		if ( !fmd.isLabel ) {
			verifyFacetTypeIsCompatible(desc, facet, expr, types, tm);
		}
	}

	private void verifyFacetTypeIsCompatible(final IDescription desc, final String facet,
		final IExpression expr, final List<String> types, final TypesManager tm) {
		boolean compatible = false;
		IType actualType = expr.type();
		for ( String type : types ) {
			compatible = compatible || tm.get(type).isAssignableFrom(actualType);
			if ( compatible ) {
				break;
			}
		}
		if ( !compatible ) {
			desc.flagWarning("Facet '" + facet + "' is expecting " + types + " instead of " +
				actualType, facet);
		}

	}

	private void verifyFacetIsAType(final IDescription desc, final String facet,
		final IExpression expr, final TypesManager tm) {
		String type = expr.literalValue();
		if ( tm.get(type) == null ) {
			desc.flagError("Facet '" + facet + "' is expecting a type name. " + type +
				" is not a type name");
		}
	}

	private void verifyFacetIsInValues(final IDescription desc, final String facet,
		final IExpression expr, final String[] values) {
		String s = expr.literalValue();
		boolean compatible = false;
		for ( String value : values ) {
			compatible = compatible || value.equals(s);
			if ( compatible ) {
				break;
			}
		}
		if ( !compatible ) {
			desc.flagError(
				"Facet '" + facet + "' is expecting a value among " + Arrays.toString(values) +
					" instead of " + s, facet);
		}
	}

}
