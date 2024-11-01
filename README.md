## EvalConst
Kotlin compiler plugin that allow to evaluate functions in compile time. Plugin manipulates kotlin AST (or IR). This is backend plugin. 

Function names that will be evaluated in compile time should start with configured prefix (default is "eval"). Let us call these functions _"eval" functions_.

If "eval" function cannot be evaluated in compile time, it will be left as is.

"eval" functions should follow the next constraints:
* "eval" function must return a result of a constant type.
* The “eval” function can accept only arguments of a constant type.

The expressions and statements that are allowed inside “eval” functions:
* Operations on primitives. Basically methods declared inside classes Int, Boolean, String etc.
* If/when expressions
* While loops
* Create val/var variables and set values for them
* __Invokations of "eval" functions (direct and indirect recursion is supported)__

Operations that change current continuation are not supported. For example, `break` and `continue` statements 
are not supported, `return` may be only the last statement in the function body.

When "eval" function body contains illegal statement or expression, it will be left as is. This does not apply to rule according `return` statements. 
If `return` statement is not the last statement in "eval" function body the behaviour is undefined.

Plugin has basic handling of too long loops and too deep recursion.
* The number of statements that can be computed in compile time can be rescticted using `EVAL_LIMIT_OPTION` cli option.
* The depth of recursion during computations in compile time can be rescticted using `STACK_LIMIT_OPTION` cli option.

When one of these limits will be exceeded for certain "eval" function, this function will be left as is. 

The main code is in: [evalconst-compiler-plugin/src/main/kotlin/com/github/enotvtapke/evalconst](evalconst-compiler-plugin/src/main/kotlin/com/github/enotvtapke/evalconst).

To check how the plugin works run the tests: [evalconst-compiler-plugin/src/test/kotlin/EvalConstExtensionTest.kt](evalconst-compiler-plugin/src/test/kotlin/EvalConstExtensionTest.kt).
