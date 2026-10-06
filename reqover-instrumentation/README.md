**English** | [한국어](README.ko.md) | [Project](../README.md)

# Bytecode Instrumentation

Maven Central library: `io.github.reqover-labs:reqover-instrumentation:0.4.2`.
Most applications use the packaged [agent](../reqover-agent/README.md), not this
module directly. Java packages stay `io.reqover.instrumentation`.

The ASM transformer inserts method-entry probe calls and registers class/method
metadata. It excludes synthetic methods and skips trivial accessors by default.
An optional mode retains accessors; another adds interface-call/static-field
reference probes at included call sites. A reference records use of the target,
not execution of a method body inside an interface or enum.

This is custom method-entry instrumentation, not JaCoCo integration. Probe sets
do not contain invocation order, repeated-call counts, method-exit spans, CPU
time, or line/branch percentages. First resolvable source lines are metadata,
not evidence that every line ran.

The agent applies include/exclude and class-loading safety policy and relocates
its packaged ASM to avoid colliding with an application's ASM. Source files are
not rewritten. Transform failures leave that class uninstrumented.

[Transformer source](src/main/java/io/reqover/instrumentation/ReqoverClassInstrumenter.java) |
[Agent options](../reqover-agent/README.md) |
[JaCoCo interoperability decision](../docs/14_jacoco_interop_decision.md)
