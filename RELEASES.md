# 0.1.5

- Fix bug with `isDefinition` on global variables not accounting for 'extern'
- Fix bug with wrong builtin type of `free` Stdlib function
- Fix type dependency analysis; now differentiates between depending on declaration vs. definition.
- Add TypeContext to represent type requirements.
- Add TypeEnv.resolve(TypeContext): TypeResolutions to resolve type requirements to suitable definitions.
- Add TypeResolutions.toPreamble to compute source-level definitions to reproduce a type resolution in another unit.

# 0.1.4

- Add AST and pretty-printer for (most of) expressions and syntax
- Add TypeEnv to abstract type environments computed from (collections of) C units.
- Add model of normalized C types and functions to normalize types based on a type env.
- Add various utility functions on normalized types (e.g. isReturnable).
- Polishing the API of UnitState
- Extract exact include path from preprocessor output
- Add index model and analysis
- Add closure analysis to compute unambigous linking based on an index

# 0.1.3

- Now modeling (and reporting) presumed locations on Diagnostics

# 0.1.2

- AST for enumerators now contains symbol referencing enclosing enum.
- ClangDependencyAnalysis now correctly handles enumerator references.
- Renamed Declaration > UnitDeclaration, making space for GlobalDeclaration super class encompassing also enumerators.

# 0.1.1

# 0.1.0

Initial release
