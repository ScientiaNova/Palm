package com.scientianova.palm.parser

import com.scientianova.palm.tokenizer.tokenize
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

val code = """
    let x = 15 - 2 * 16
    
    let fac: Int -> Int -> Int
    fac(acc, 0) = acc
    fac(acc, left) = fac(acc * left, left - 1)
    
    let debug = {
      println("Normal")
      println<|"Left pipe"
      println|>"Right pipe"
    }
    
    let iron = stack<|"iron_ingot"
    let redstone = tag("dusts/redstone")

    recipes {
        addShaped(unique, stack("compass"), [empty, iron; iron, redstone, iron; empty, iron])
    }
    
    let maceratorRecipes = recipeType {
      typeName("macerator")
      inputs(1, 4)
      outputs(2, 6)
      fluidInputs(1, 1)
    }
    
    let mareactor1 = regularMachine("macerator_tier_1", maceratorRecipes)
    
    materials forEach { mat ->
      println("${'$'}{mat |> matName}") 
    }
    
    let IntCube = List[List[List[Int]]]
    
    let User record(name: String, working: Bool)

    let Option[a] enum {
      Some(a),
      None
    }
    
    let Enum[a] class where Bounded[a] {
      let fromInt: Int -> a
      let toInt: a -> Int
      let next: a -> a
      let prev: a -> a
      
      next(thing) = fromInt(toInt(thing) + 1)
      prev(thing) = fromInt(toInt(thing) - 1)
    }
    
    let BoundedInt impl Bounded[Int] {
      upperBound = intMax
      lowerBound = intMin
    }
    
    let infix >< 5 = zip
""".trimIndent()

fun tokenizeLargeFile() = benchmark { tokenize(code) }

fun parserLargeFile() = benchmark { parse(code) }