package com.scientianova.palm.parser

const val oldExample = """
    if (state.char?.isSymbolPart() == true) {
        val op = handleSymbol(state)
        if (op.value.isPostfixOp(afterOp)) {
            list.map(op.area) { (expr) ->
                PostfixOpExpr(op, expr).at(expr.area.first..op.area.last).succTo(afterOp)
            }.flatMap { (newList, next) ->
                return handleInlinedBinOps(next, start, excludeCurly, newList)
            }
        } else { 
            handleSubexpr(afterOp.actual, false).flatMap { (sub, next) ->
              return handleInlinedBinOps(next, start, excludeCurly, list.appendSymbol(op, sub))
            } 
        }
    } else {
        val actual = state.actual
        when (actual.char) {
            null -> finishBinOps(start, list, state)
            in identStartChars -> {
                val infix = handleIdent(actual)
                when (infix.value) {
                    in keywords -> finishBinOps(start, list, state)
                    "is" -> handleType(afterInfix.actual, false).flatMap { (type, next) ->
                        return handleInlinedBinOps(next, start, excludeCurly, list.appendIs(type))
                    }
                    "as" -> {
                        val handling = when (afterInfix.char) {
                            '!' -> AsHandling.Unsafe.to(afterInfix.nextActual)
                            '?' -> AsHandling.Nullable.to(afterInfix.nextActual)
                            _ -> AsHandling.Safe.to(afterInfix.actual)
                        }
                        handleType(typeStart, false).flatMap { (type, next) ->
                            return handleInlinedBinOps(next, start, excludeCurly, list.appendAs(type, handling))
                        }
                    }
                    _ -> handleSubexpr(afterInfix.actual, false).flatMap { (part, next) ->
                        return handleInlinedBinOps(next, start, excludeCurly, list.appendIdent(infix, part))
                    }
                }
            }
            '`' -> handleBacktickedIdent(actual.next).flatMap { (infix, afterInfix) ->
                handleSubexpr(afterInfix.nextActual, false).flatMap { (part, next) ->
                    return handleInlinedBinOps(next, start, excludeCurly, list.appendIdent(infix, part))
                }
            }
            in symbolChars -> {
                val op = handleSymbol(state)
                val symbol = op.value
                if (!(symbol.endsWith('.') && symbol.length <= 2) || afterOp.char?.isWhitespace() == false) {
                    invalidPrefixOperatorError.errAt(afterOp.pos)
                } else {
                    when (symbol) {
                        "->" -> finishBinOps(start, list, state)
                        _ -> handleSubexpr(afterOp.actual, false).flatMap { (part, next) ->
                            return handleInlinedBinOps(next.actual, start, excludeCurly, list.appendSymbol(op, part))
                        }
                    }
                }
            }
            '(' -> list.map(state.pos) { (expr) -> handleCall(state, expr, excludeCurly) }.flatMap { (newList, next) ->
                return handleInlinedBinOps(next, start, excludeCurly, newList)
            }
            '{' -> if (excludeCurly) {
                finishBinOps(start, list, state)
            } else { 
                list.map(state.pos) { (expr) ->
                    handleLambda(state.nextActual, state.pos).map { (lambda) ->
                        CallExpr(expr, CallParams(listOf(lambda))).at(expr.area.first..lambda.area.first)
                    }
                }
            }.flatMap { (nextList, next) -> return handleInlinedBinOps(next, start, excludeCurly, nextList) }
            _ -> finishBinOps(start, list, state)
        }
    }
"""

const val aocExample = """ 
private val mapping: [Char: Direction] = [
	"U": Direction.Up,
	"R": Direction.Right,
	"D": Direction.Down,
	"L": Direction.Left
]
	
fun directionFromChar(raw: Char) = mapping[raw]!

data class Instruction(
	val direction: Direction,
	val length: Int,
	val vector: Vector2,
)

data class Segment(
	val start: Vector2,
	val end: Vector2,
	val startStepCount: Int,
	val minX: Int = min(start.x, end.x),
	val maxX: Int = max(start.x, end.x),
	val xRange: ClosedRange[Int] = minX..maxX,
	val minY: Int = min(start.y, end.y),
	val maxY: Int = max(start.y, end.y),
	val yRange: ClosedRange[Int] = minY..maxY,
)

impl Segment {
	fun intersections(other: Segment): [(Vector2, Int)] {
		if (!intersects(other)) return []
		
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

val paths: [[Segment]] = instructions.map { path ->
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
	.min()!

val bestIntersection = intersections
	.map { it.stepCount }
	.min()!

fun main() { 
  println("closest intersection: ${'$'}closestIntersection")
  println("best intersection: ${'$'}bestIntersection")
}
"""

const val implTest = """
data class Builder(
    internal val kind: Kind,
    internal val enableIO: Bool,
    internal val enableTime: Bool,
    internal val workerThreads: Option<Int>,
    internal val maxThreads: Int,
    internal val threadName: ThreadNameFn,
    internal val threadStackSize: Option<Int>,
    internal val afterStart: Option<Callback>,
    internal val beforeStop: Option<Callback>,
    internal val keepAlive: Option<Duration>,
)

type ThreadNameFn = () -> String

enum class Kind {
    object CurrentThread : Kind()
    object MultiThread : Kind()
}

fun createCurrentThread(): Builder =
    Builder(Kind.CurrentThread)
    
fun createMultiThread(): Builder =
    Builder(Kind.MultiThread)
    
fun create(kind: Kind): Builder = Builder(
    kind, 
    enableIO: false,
    enableTime: false,
    workerThreads: None,
    maxThreads: 512,
    threadName: { "runtime-worker" },
    threadStackSize: None,
    afterStart: None,
    beforeStop: None,
    keepAlive: None,
)

fun enableAll[Builder]() = this.enableIO().enableTime()

fun workerThreads[Builder](value: Int): T {
    assertTrue(value > 0, "Worker threads cannot be set to 0")
    this.copyWith(workerThreads: Some(value))
}

fun maxThreads[Builder](value: Int): T = this.copyWith(maxThreads: value)

fun threadName[Builder](value: Into<String>): T {
    val value = value.into()
    this.copyWith(threadName: { value })
}

fun threadNameFn[Builder](f: () -> String): T = this.copyWith(threadName: f)
    
fun threadStackSize[Builder](value: Int): T = this.copyWith(threadStackSize: Some(value))
    
fun onThreadStart[Builder](f: () -> Send) = this.copyWith(afterStart: Some(f))

fun onThreadStop[Builder](f: () -> Send) = this.copyWith(beforeStop: Some(f))

fun build[Builder](): Result<Runtime> = when (kind) { 
    CurrentThread -> {
        buildBasicRuntime()
    }
    MultiThread -> {
        buildThreadedRuntime()
    }
}

val cfg[Builder]: driver.Cfg get() = driver.Cfg(
    enableIO: enableIO,
    enableTime: enableTime, 
)

fun threadKeepAlive[Builder](duration: Duration) = this.copyWith(keepAlive: Some(duration))

fun buildBasicRuntime[Builder](): io.Result<Runtime> {
    val (driver, resources) = driver.Driver(cfg);
      
    val scheduler = BasicScheduler(driver);
    val spawner = Spawner.Basic(scheduler.spawner());
        
    val blockingPool = blocking.createBlockingPool(this, maxThreads);
    val blockingSpawner = blockingPool.spawner();

    return Result.Ok(Runtime(
        kind: Kind.CurrentThread(scheduler),
        handle: Handle(
            spawner,
            ioHandle: resources.ioHandle,
            timeHandle: resources.timeHandle,
            signalHandle: resources.signalHandle,
            clock: resources.clock,
            blockingSpawner,
        ),
        blockingPool,
    ))
}

fun enableIO[Builder]() = this.copyWith(enableIO: true)

fun enableTime[Builder]() = this.copyWith(enableTime: true)

fun buildThreadedRuntime[Builder](): io.Result<Runtime> {
    val coreThreads = workerThreads.unwrapOrElse { min(maxThreads, numCpus()) }
    assertTrue(core_threads <= self.max_threads, "Core threads number cannot be above max limit");

    val (driver, resources) = driver.Driver(crg);

    val (scheduler, launch) = ThreadPool(core_threads, Parker(driver))
    val spawner = Spawner.ThreadPool(scheduler.spawner())

    val blockingPool = blocking.createBlockingPool(this, maxThreads)
    val blockingSpawner = blockingPool.spawner()

    val handle = Handle(
        spawner,
        ioHandle: resources.ioHandle,
        timeHandle: resources.timeHandle,
        signalHandle: resources.signalHandle,
        clock: resources.clock,
        blockingSpawner,
    )

    val _enter = enter(handle)
    launch.launch()

    Result.Ok(Runtime(
        kind: Kind.ThreadPool(scheduler),
        handle,
        blockingPool,
    ))
}

impl Debug<Builder> {
    fun fmt[Builder](fmt: fmt.Formatter<*>): fmt.Result =
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

val branchesExample = """
fun parseOp[Parser](): PBinOp? = when (current) {
    Token.Plus if currentInfix() -> Plus.end()
    Token.Minus if currentInfix() -> Minus.end()
    Token.Times -> Times.end()
    Token.Div -> Div.end()
    Token.Rem -> Rem.end()
    Token.RangeTo -> RangeTo.end()
    Token.Eq -> Eq.end()
    Token.RefEq -> RefEq.end()
    Token.NotEq -> Eq.end().let { Not(it).at(it.start, it.next) }
    Token.NotRefEq -> RefEq.end().let { Not(it).at(it.start, it.next) }
    Token.As when (rawLookup(1)) {
        Token.QuestionMark -> {
            val asStart = pos
            advance()
            NullableAs.end(asStart)
        }
        Token.ExclamationMark -> {
            val asStart = pos
            advance()
            UnsafeAs.end(asStart)
        }
        _ -> As.end()
    }
    Token.Is if !lastNewline -> Is.end()
    Token.In if !lastNewline -> In.end()
    Token.QuestionMark if rawLookup(1) == Token.Colon -> {
        advance()
        Elvis.end(pos - 1)
    }
    Token.Greater -> Greater.end()
    Token.Less -> Less.end()
    Token.GreaterOrEq -> GreaterOrEq.end()
    Token.LessOrEq -> LessOrEq.end()
    Token.And -> And.end()
    Token.Or -> Or.end()
    Token.Assign -> Assign.end()
    Token.PlusAssign -> PlusAssign.end()
    Token.MinusAssign -> MinusAssign.end()
    Token.TimesAssign -> TimesAssign.end()
    Token.DivAssign -> DivAssign.end()
    Token.RemAssign -> RemAssign.end()
    Token.ExclamationMark if !lastNewline when (rawLookup(1)) {
        is Token.Is -> {
            val start = pos
            advance()
            Is.end().let { Not(it).at(start, it.next) }
        }
        is Token.In -> {
            val start = pos
            advance()
            In.end().let { Not(it).at(start, it.next) }
        }
        is Token.Ident(val name) -> {
            val start = pos
            advance()
            Infix(name).end().let { Not(it).at(start, it.next) }
        }
    }
    is Token.Ident(val name) if !lastNewline -> Infix(token.name).end()
    _ -> null
}
""".trimIndent()