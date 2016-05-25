How To Maintain
===

Background Knowledge
---
### Soot
A framework for analyzing and transforming Java and Android Applications
Ref:
1. [GitHub](https://github.com/Sable/soot)
2. [Tutorials](https://github.com/Sable/soot/wiki/Tutorials)
3. [Soot Options](https://ssebuild.cased.de/nightly/soot/doc/soot_options.htm)
4. [Javadoc](https://ssebuild.cased.de/nightly/soot/javadoc/)
5. [Nightly Build](http://ssebuild.cased.de/nightly/soot/)

### Heros
Heros is a generic implementation of an IFDS/IDE Solver that can be plugged into existing, Java-based program analysis frameworks. A reference connector exists for the Soot framework.
Ref:
1. [Precise interprocedural dataflow analysis with applications to constant propagation](http://link.springer.com/chapter/10.1007%2F3-540-59293-8_226)
2. [Inter-procedural data-flow analysis with IFDS/IDE and Soot](http://dl.acm.org/citation.cfm?id=2259052)
3. [GitHub](https://github.com/Sable/heros)

Dependency
---
HisDroid is depends on Soot and Heros. It use a manipulated version of Soot nightly build jar for the purpose of some bugs. Details: [Soot](https://github.com/pass518224/soot/commits/develop), [Heros](https://github.com/pass518224/heros/commits/develop)

Architecture
---
### hisdroid.Main
#### Main()
1. Parse command line and setting
2. Add SceneTransformer *hisdroid.HisdroidAnalysisTransformer* in pack *wjtp*
3. call *PackManager.runPacks()* to start soot

### hisdroid.HisdroidAnalysisTransformer
#### internalTransform()
1. Start at wjtp
1. Create dummy main based on ICC log
2. Turn on *spark* and run *cg* pack manually
3. Run *hisdroid.IDEAnalyzer.analyze()* to start analysis
4. Instrument code with analysis result

### hisdroid.DummyMainCreator
#### createDummyMain()
1. Create class *DummyMain* with a static int field *iccNo* and a *main* method
2. For each ICC log
	1. Assign iccNo to ICC id
	2. Create proper method call
3. Set DummyMain as the main method of Soot

### hisdroid.IDEAnalyzer
#### analyze()
1. create a *hisdroid.ide.IDEProblem* instance
2. solve IDEProblem with heros.solver.IDESolver

### soot.Value
A interface in soot, represents data or expression. Local, FieldRef, Constant, Expr all implement Value. Used as data-flow fact in *IDEProblem.* 

### hisdroid.value.GeneralValue
A abstract class, represents the value of data-flow fact(soot.Value). IntentValue, PrimitiveDataValue, BottomValue extends it.

### hisdroid.ide.IDEProblem
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

### hisdroid.edgefunction.EdgeFunctionTemplate
A abstract class implements heros.EdgeFunction<GeneralValue>. All Edgefunction in hisdroid are extends from it.
Use linked list to implement *composeWith()* method. subclass can implement composeWithNext() to optimize.
To simplify *joinWith()*, only join two edge when both edge has same next edge. Otherwise, return AllBottom.

### hisdroid.callhandler.CallHandler
A abstract class which perform a special handler toward a specific method.
#### Set<MethodSig> getTargets()
Return a set of method signatures handled by the handler
#### getCallFlowFunction(), getReturnFlowFunction(), getCallToReturnFlowFunction()
Return the FlowFunction at the method call
#### getCallEdgeFunction(), getReturnEdgeFunction(), getCallToReturnEdgeFunction()
Return the EdgeFunction at the method call

### hisdroid.callhandler.Handlers
A collction of CallHandler. IDEProblem will query it.

### hisdroid.instrumenter.Instrumenter
A abstract class.
Implements the feature of determine whether branch takes based on the result of Analyzer.
#### abstract void instrumentBranch(), abstract public List<Unit> instrumentSwitch()
Subclass should implements these two function to instrument code

### hisdroid.instrumenter.Pruner
Prune the branches which won't be taken.

### hisdroid.instrumenter.StatsInstrumenter
Insert code to print some statistics message in branches.
