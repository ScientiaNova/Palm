package com.scientianova.palm.parser

const val oldExample = """
    if state.char?.isSymbolPart() == true {
        val op = handleSymbol(state)
        if op.value.isPostfixOp(afterOp) {
            list.map(op.area) { expr ->
                PostfixOpExpr(op, expr).at(expr.area.first..op.area.last).succTo(afterOp)
            }.flatMap { newList, next ->
                return handleInlinedBinOps(next, start, excludeCurly, newList)
            }
        } else { 
            handleSubexpr(afterOp.actual, false).flatMap { sub, next ->
              return handleInlinedBinOps(next, start, excludeCurly, list.appendSymbol(op, sub))
            } 
        }
    } else {
        val actual = state.actual
        when actual.char {
            null -> finishBinOps(start, list, state)
            identStartChars -> {
                val infix = handleIdent(actual)
                when infix.value {
                    keywords -> finishBinOps(start, list, state)
                    "is" -> handleType(afterInfix.actual, false).flatMap { type, next ->
                        return handleInlinedBinOps(next, start, excludeCurly, list.appendIs(type))
                    }
                    "as" -> {
                        val handling = when afterInfix.char {
                            '!' -> AsHandling.Unsafe.to(afterInfix.nextActual)
                            '?' -> AsHandling.Nullable.to(afterInfix.nextActual)
                            _ -> AsHandling.Safe.to(afterInfix.actual)
                        }
                        handleType(typeStart, false).flatMap { type, next ->
                            return handleInlinedBinOps(next, start, excludeCurly, list.appendAs(type, handling))
                        }
                    }
                    _ -> handleSubexpr(afterInfix.actual, false).flatMap { part, next ->
                        return handleInlinedBinOps(next, start, excludeCurly, list.appendIdent(infix, part))
                    }
                }
            }
            '`' -> handleBacktickedIdent(actual.next).flatMap { infix, afterInfix ->
                handleSubexpr(afterInfix.nextActual, false).flatMap { part, next ->
                    return handleInlinedBinOps(next, start, excludeCurly, list.appendIdent(infix, part))
                }
            }
            symbolChars -> {
                val op = handleSymbol(state)
                val symbol = op.value
                if !(symbol.endsWith('.') && symbol.length <= 2) || afterOp.char?.isWhitespace() == false {
                    invalidPrefixOperatorError.errAt(afterOp.pos)
                } else {
                    when symbol {
                        "->" -> finishBinOps(start, list, state)
                        _ -> handleSubexpr(afterOp.actual, false).flatMap { part, next ->
                            return handleInlinedBinOps(next.actual, start, excludeCurly, list.appendSymbol(op, part))
                        }
                    }
                }
            }
            '(' -> list.map(state.pos) { expr -> handleCall(state, expr, excludeCurly) }.flatMap { newList, next ->
                return handleInlinedBinOps(next, start, excludeCurly, newList)
            }
            '{' -> if excludeCurly {
                finishBinOps(start, list, state)
            } else { 
                list.map(state.pos) { expr ->
                    handleLambda(state.nextActual, state.pos).map { lambda ->
                        CallExpr(expr, CallParams(listOf(lambda))).at(expr.area.first..lambda.area.first)
                    }
                }
            }.flatMap { nextList, next -> return handleInlinedBinOps(next, start, excludeCurly, nextList) }
            _ -> finishBinOps(start, list, state)
        }
    }
"""

const val enumTest = """
    package test
    
    enum Expr {
        Ident(String),
        Call(PExpr, CallArgs),
        Lambda(PString?, LambdaParams, ExprScope),
        If(List[Condition], ExprScope, ExprScope?),
        When(PExpr?, List[WhenBranch]),
        For(
            PString?,
            PDecPattern,
            PExpr,
            ExprScope,
            ExprScope?
       ),
        While(
            PString?,
            List[Condition],
            ExprScope,
            ExprScope?
       ),
        Loop(PString?, ExprScope),
        Continue(PString?),
        Break(PString?, PExpr?),
        Return(PString?, PExpr?),
        Throw(PExpr),
        Do(ExprScope, List[Catch]),
        Scope(ExprScope),
        Byte(palm.Byte),
        Short(palm.Short),
        Int(palm.Int),
        Long(palm.Long),
        Float(palm.Float),
        Double(palm.Double),
        Char(palm.Char),
        Str(List[StringPart]),
        Bool(Boolean),
        Null,
        This,
        Super,
        Tuple(List[PExpr]),
        Lis(List[PExpr]),
        Map(List[Pair[PExpr, PExpr]]),
        Get(PExpr, List[PExpr]),
        TypeCheck(PExpr, PType),
        SafeCast(PExpr, PType),
        NullableCast(PExpr, PType),
        UnsafeCast(PExpr, PType),
        MemberAccess(PExpr, PString),
        SafeMemberAccess(PExpr, PString),
        Turbofish(PExpr, List[PTypeArg]),
        FunRef(PExpr?, PString),
        Spread(PExpr),
        Unary(UnaryOp, PExpr),
        Binary(PExpr, BinaryOp, PExpr),
        And(PExpr, PExpr),
        Or(PExpr, PExpr),
        Elvis(PExpr, PExpr),
        Eq(PExpr, PExpr),
        NotEq(PExpr, PExpr),
        RefEq(PExpr, PExpr),
        NotRefEq(PExpr, PExpr),
        Annotated(Annotation, PExpr),
        Object(List[SuperType], List[ObjectStatement])
    }
"""

const val aocExample = """ 
package test 

private val mapping: Map[Char, Direction] = [
	"U": Direction.Up,
	"R": Direction.Right,
	"D": Direction.Down,
	"L": Direction.Left
]
	
fun directionFromChar(raw: Char) = mapping[raw]!!

record Instruction {
	direction: Direction,
	length: Int,
	vector: Vector2,
}

record Segment {
	start: Vector2,
	end: Vector2,
	startStepCount: Int,
	minX: Int = min(start.x, end.x),
	maxX: Int = max(start.x, end.x),
	xRange: ClosedRange[Int] = minX..maxX,
	minY: Int = min(start.y, end.y),
	maxY: Int = max(start.y, end.y),
	yRange: ClosedRange[Int] = minY..maxY,
}

impl Segment {
	fun intersections(other: Segment): Map[Pair[Vector2, Int]] {
		guard intersects(other) else { return [] }
		
		val xs = max(minX, other.minX)..min(maxX, other.maxX)
		val ys = max(minY, other.minY)..min(maxY, other.maxY)
		return xs.flatMap { x ->
			ys.map { y -> Vector2(x, y) }
		}.map { (it, steps(to = it) + other.steps(to = it)) }
	}
	
	fun intersects(other: Segment): Bool =
      xRange.overlaps(other.xRange) && yRange.overlaps(other.yRange)
	
	fun steps(position: Vector2): Int =
		startStepCount + abs(position.x - start.x) + abs(position.y - start.y)
}

val instructions = input().lines().map {
	it.components(separatedBy = ",").map(::Instruction)
}

val paths: List[List[Segment]] = instructions.map { path ->
	var position = Vector2.Zero
	var stepCount = 0
	return path.map { segment ->
		val endPosition = position + segment.vector
		defer {
			position = endPosition
			stepCount += segment.length
		}
		Instruction(start = position, end = endPosition, startStepCount = stepCount)
	}
}
 
val path1 = paths[0]
val path2 = paths[1]

val intersections = path1
	.flatMap { path2.flatMap(it.intersections()) }
	.filter { it.position != Vector2.Zero }

val closestIntersection = intersections
	.map { it.position.absolute }
	.min()!!

val bestIntersection = intersections
	.map { it.stepCount }
	.min()!!

fun main() { 
  println("closest intersection: ${'$'}closestIntersection")
  println("best intersection: ${'$'}bestIntersection")
}
"""

const val implTest = """
package test

record Builder {
    internal kind: Kind,
    internal enableIO: bool,
    internal enableTime: bool,
    internal workerThreads: Option[Int],
    internal maxThreads: Int,
    internal threadName: ThreadNameFn,
    internal threadStackSize: Option[Int],
    internal afterStart: Option[Callback],
    internal beforeStop: Option[Callback],
    internal keepAlive: Option[Duration],
}

type ThreadNameFn = () -> String + Send + Sync

enum Kind {
    CurrentThread,
    MultiThread,
}

impl Builder {
    static fun createCurrentThread(): Builder =
        Builder(Kind.CurrentThread)
    
    static fun createMultiThread(): Builder =
        Builder(Kind.MultiThread)
    
    static fun create(kind: Kind): Builder = Builder(
        kind, 
        enableIO = false,
        enableTime = false,
        workerThreads = None,
        maxThreads = 512,
        threadName = { "runtime-worker".into() },
        threadStack_Size = None,
        afterStart = None,
        beforeStop = None,
        keepAlive = None,
    )


    fun enableAll() = this.enableIO().enableTime()

    fun workerThreads(value: Int): This {
        assertTrue(value > 0, "Worker threads cannot be set to 0")
        return this.copyWith(workerThreads = Some(value))
    }

    fun maxThreads(value: Int): This = this.copyWith(maxThreads = value)

    fun threadName(value: Into[String]): This {
        val value = value.into()
        return this.copyWith(threadName = { value })
    }

    fun threadNameFn(f: () -> String + Send + Sync): This = this.copyWith(threadName = f)
    
    fun threadStackSize(value: Int): This = this.copyWith(threadStackSize = Some(value))
    
    fun onThreadStart(f: () -> Send + Sync) = this.copyWith(afterStart = Some(f))

    fun onThreadStop(f: () -> Send + Sync) = this.copyWith(beforeStop = Some(f))

    fun build(): Result[Runtime] = when kind { 
        CurrentThread -> {
            buildBasicRuntime()
        }
        MultiThread -> {
            buildThreadedRuntime()
        }
    }

    val cfg: driver.Cfg get() = driver.Cfg(
        enableIO = enableIO,
        enableTime = enableTime, 
    )

    fun threadKeepAlive(duration: Duration) = this.copyWith(keepAlive = Some(duration))

    fun buildBasicRuntime(): io.Result[Runtime] {
        val (driver, resources) = driver.Driver(cfg);
        
        val scheduler = BasicScheduler(driver);
        val spawner = Spawner.Basic(scheduler.spawner());
        
        val blockingPool = blocking.createBlockingPool(this, maxThreads);
        val blockingSpawner = blockingPool.spawner();

        return Result.Ok(Runtime(
            kind = Kind.CurrentThread(scheduler),
            handle = Handle(
                spawner,
                ioHandle = resources.ioHandle,
                timeHandle = resources.timeHandle,
                signalHandle = resources.signalHandle,
                clock = resources.clock,
                blockingSpawner,
            ),
            blockingPool,
        ))
    }

    fun enableIO() = this.copyWith(enableIO = true)

    fun enableTime() = this.copyWith(enableTime = true)

    fun buildThreadedRuntime(): io.Result[Runtime] {
        val coreThreads = workerThreads.unwrapOrElse { min(maxThreads, numCpus()) }
        assertTrue(core_threads <= self.max_threads, "Core threads number cannot be above max limit");

        val (driver, resources) = driver.Driver(crg);

        val (scheduler, launch) = ThreadPool(core_threads, Parker(driver))
        val spawner = Spawner.ThreadPool(scheduler.spawner())

        val blockingPool = blocking.createBlockingPool(this, maxThreads)
        val blockingSpawner = blockingPool.spawner()

        val handle = Handle(
            spawner,
            ioHandle = resources.ioHandle,
            timeHandle = resources.timeHandle,
            signalHandle = resources.signalHandle,
            clock = resources.clock,
            blockingSpawner,
        )

        val _enter = enter(handle)
        launch.launch()

        return Result.Ok(Runtime(
            kind = Kind.ThreadPool(scheduler),
            handle,
            blockingPool,
        ))
    }
}

impl Debug for Builder {
    fun fmt(fmt: fmt.Formatter[_]): fmt.Result =
        fmt.debug_struct("Builder")
            .field("worker_threads", workerThreads)
            .field("max_threads", maxThreads)
            .field( "thread_name",threadName)
            .field("thread_stack_size", thread_StackSize)
            .field("after_start", afterStart.map { "..." })
            .field("before_stop", beforeStop.map { "..." })
            .finish()
}
"""