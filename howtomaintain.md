How To Maintain
===

Background Knowledge
---
### Soot
A framework for analyzing and transforming Java and Android Applications

#### Reference
1. [GitHub](https://github.com/Sable/soot)
2. [Tutorials](https://github.com/Sable/soot/wiki/Tutorials)
3. [Soot Options](https://ssebuild.cased.de/nightly/soot/doc/soot_options.htm)
4. [Javadoc](https://ssebuild.cased.de/nightly/soot/javadoc/)
5. [Nightly Build](http://ssebuild.cased.de/nightly/soot/)

### Heros
Heros is a generic implementation of an IFDS/IDE Solver that can be plugged into existing, Java-based program analysis frameworks. A reference connector exists for the Soot framework.

#### Reference
1. [Precise interprocedural dataflow analysis with applications to constant propagation](http://link.springer.com/chapter/10.1007%2F3-540-59293-8_226)
2. [Inter-procedural data-flow analysis with IFDS/IDE and Soot](http://dl.acm.org/citation.cfm?id=2259052)
3. [GitHub](https://github.com/Sable/heros)

Dependency
---
libericc is depends on Soot and Heros. It use a manipulated version of Soot nightly build jar for the purpose of some bugs. Details: [Soot](https://github.com/pass518224/soot/commits/develop), [Heros](https://github.com/pass518224/heros/commits/develop)

Architecture
---
### libericc.Main
#### Main()
1. Parse command line and setting
2. Add SceneTransformer due to argument
	1. Add SceneTransformer *libericc.LibericcPreEvaluateTransformer* in pack *wjtp* for insturment before evaluation
	2. Add SceneTransformer *libericc.LibericcAnalysisTransformer* in pack *wjtp* for analysis
3. call *PackManager.runPacks()* to start soot

### libericc.Config
1. Global configuration of Libericc

### libericc.LibericcPreEvaluateTransformer
1. Instrument the apk to log the method invocation and branch decision
2. Insert call stmt in methods and branches to call *libericc.PreEvaluateLogger() and log to adblog

### libericc.LibericcPreEvaluateLogger
1. Inserted in apk to log method invocations and branch decisions
2. Output in adblog with prefix "libericc"

### libericc.LibericcAnalysisTransformer
#### internalTransform()
1. Start at wjtp
1. Create dummy main based on ICC log
2. Turn on *spark* and run *cg* pack manually
3. Use *libericc.IDEAnalyzer.analyze()* to start analysis
5. Use *libericc.decider.Decider* to determine whether the branches execute
6. Use *libericc.instrumenter.AnalysisInstrumenter* to instrument the app. It takes a *Decider* and a *libericc.pruner.BranchPruner* as argument
4. Use *libericc.evaluation.Evaluator* to evaluate percision if needed

### libericc.DummyMainCreator
#### createDummyMain()
1. Create class *DummyMain* with a static int field *iccNo* and a *main* method
2. For each ICC log
	1. Assign iccNo to ICC id
	2. Create proper method call for a lifecycle ICC
3. Set DummyMain as the main method of Soot
4. Details
	1. A component is recongnized by a binder in Android. The binder is a 16-bytes long hex in ICC log. A component created with name and binder. After that, system communicate it with only binder. The mapping of component and binder needs to be log in component creation.

### libericc.IDEAnalyzer
#### analyze()
1. Create a *libericc.ide.ConstantPropagationProblem* instance
2. Solve ConstantPropagationProblem with heros.solver.IDESolver

### libericc.decider.AnalysisDecider
1. Determine the runtime result of branches and switches based on the result of Analyzer

### libericc.PredictResult
1. Representation of the predict result of analysis
	1. True: The true branch is taken
	2. False: The false branch is taken
	3. Both: Both branch are taken
	4. Ambiguous: Not able to decide the branch result(ex. the condition variable is unknown in IDE analysis)
	5. Unknown: Not in the analysis scope (ex. the branch is not in call graph, the condition variable is not in IDE analysis)

### libericc.pruner.BranchPruner
1. Prune branches and switches based on given result
1. Available types
	1. ConservativeBranchPruner: Prune the unreached branch of *True* and *False*. Reserve both branch of *Both*. Reserve both branch of *Ambiguous* and *Unknown*. Default.
	2. AggressiveBranchPruner: Prune the unreached branch of *True* and *False*. Reserve both branch of *Both*. Prune both branch of *Ambiguous* and *Unknown*.
	3. DoNothingBranchPruner: Reserve all.

### libericc.instrumenter.AnalysisInstrumenter
1. Instrument the apk
2. Traverse app methods and branches. Pass the result of *Decider* to *BranchPruner*.

### libericc.evaluation.Evaluator
1. Evaluate the precision of *Decider* to the runtime log.
2. Available types
	1. ConservativeEvaluator
	2. AggressiveEvaluator
3. *Evaluator* is independent to *Instrumenter*. Run it before *Instrumenter*. Instrument changes the id of branches. *Evaluator* connot identify branches if it run after *Instrumenter*.



### soot.Value
A interface in soot, represents data or expression. Local, FieldRef, Constant, Expr all implement Value. Used as data-flow fact in *ConstantPropagationProblem.* 

### libericc.value.GeneralValue
A abstract class, represents the value of data-flow fact(soot.Value). IntentValue, PrimitiveDataValue, BottomValue extends it.

### libericc.value.DataValue
The normal implementation of GeneralValue

### libericc.value.PrimitiveDataValue
Special implementation for DataValue. Implement ComparableGeneralValue.

### libericc.value.StringValue
Special implementation for DataValue. Implement EqualableGeneralValue.

### libericc.value.interface.ComparableGeneralValue, libericc.value.interface.EqualableGeneralValue
Interface for GeneralValue. Means the value is able to compare/equal to another value.

### libericc.ide.ConstantPropagationProblem
extends *soot.jimple.toolkits.ide.DefaultJimpleIDETabulationProblem*
#### getNormalFlow(), getCallFlow(), getReturnFlow(), getCallToReturnFlow()
return a *heros.FlowFunction* at a stmt, call to a method, return from a method, direct call-to-return
#### getNormalEdge(), getCallEdge(), getReturnEdge(), getCallToReturnEdge()
return a *heros.EdgeFunction* for each flow at a stmt, call to a method, return from a method, direct call-to-return

### heros.FlowFunction<Value>
#### Set<Value> computeTargets(Value source)
return a set of Value which are reachable from source

### heros.EdgeFunction<GeneralValue>
#### GeneralValue computeTarget(GeneralValue source)
return a GeneralValue after edge calculation from source
#### EdgeFunction<GeneralValue> composeWith(EdgeFunction<GeneralValue> secondFunction)
return a EdgeFunction compose with this and secondFuncion
#### EdgeFunction<GeneralValue> joinWith(EdgeFunction<GeneralValue> otherFunction)
return a EdgeFunction join with this and otherFuncion

### libericc.edgefunction.EdgeFunctionTemplate
A abstract class implements heros.EdgeFunction<GeneralValue>. All Edgefunction in libericc are extends from it.
Use linked list to implement *composeWith()* method. subclass can implement composeWithNext() to optimize.
To simplify *joinWith()*, only join two edge when both edge has same next edge. Otherwise, return AllBottom.

### libericc.callhandler.CallHandler
A abstract class which perform a special handler toward a specific method.
#### Set<MethodSig> getTargets()
Return a set of method signatures handled by the handler
#### getCallFlowFunction(), getReturnFlowFunction(), getCallToReturnFlowFunction()
Return the FlowFunction at the method call
#### getCallEdgeFunction(), getReturnEdgeFunction(), getCallToReturnEdgeFunction()
Return the EdgeFunction at the method call

### libericc.callhandler.Handlers
A collction of CallHandler. IDEProblem will query it.
