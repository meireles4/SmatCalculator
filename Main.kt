package calculator

import java.math.BigInteger
import java.util.Stack
import kotlin.system.exitProcess

var mapVar = mutableMapOf<String, String>()
var regexOnlyDigits = Regex("-?[0-9]+")
var regexOnlyLetters = Regex("[a-zA-Z]+")

fun main() {

    while (true) {

        val input = readln()

        if(input.startsWith("/")){
            processCommand(input)
        }
        else if (input == "") {
            continue
        }
        else {
            processExpression(input)
        }

    }

}

fun processExpression(input: String) {

    val regexSignalNumber = Regex("(-*|\\+*)[0-9]+")
    val regexLongExpression = Regex("[)(0-9a-zA-Z ]+( *(-+|\\++|\\*|/)* *[)(0-9a-zA-Z ]+)+")
    val regexVarAssign = Regex(".+=.+")
    val regexInvalidDivAndMul = Regex(".*(\\*\\*+|//+).*")

    if ( (!regexLongExpression.matches(input) and
         !regexSignalNumber.matches(input) and
         !regexVarAssign.matches(input) and
         !regexOnlyLetters.matches(input)) or
         regexInvalidDivAndMul.matches(input)

        ) {
        println("Invalid expression")
        return
    }
    else if (regexVarAssign.matches(input)) {
        processAssignment(input)
    }
    else if (regexOnlyDigits.matches(input)){
        println(input)
        return
    }
    else {
        if (!wellUsedParenthesis(input)){
            println("Invalid expression")
            return
        }

        var test = cleanPlusAndMinus(input)
        test = arrangeSpaces(test)
        var newExpression = test.split(" ")

        //var newExpression = getListWithParenthesisSeparated(expression)
        //println("newExpression: $newExpression")

        newExpression = cleanInput(newExpression)

        if (unknownVariables(newExpression)){
            println("Unknown variable")
            return
        }

        val result = calculate(newExpression)

        println(result)
    }
}

fun cleanPlusAndMinus(input: String): String {
    var s = input.replace(Regex("\\+\\++"), "+")
    s = s.replace("--", "+")
    s = s.replace("+-", "-")
    s = s.replace("++", "+")

    return s
}

fun arrangeSpaces(inputRaw: String): String {
    var newString = ""

    for (c in inputRaw) {
        if (c == '('){
            newString += "( "
        }
        else if (c == ')'){
            newString += " )"
        }
        else if (c.toString().matches(Regex("(\\+|-|\\*|/)"))){
            newString += " $c "
        }
        else if (c.toString() != " ") {
            newString += c
        }
    }

    return newString.replace("  ", " ")

}

fun wellUsedParenthesis(value: String): Boolean {
    val parenthesis = setOf('(', ')')
    val filteredValue = value.filter { parenthesis.contains(it) }
    //println("filteredValue: $filteredValue")

    val stack: Stack<Char> = Stack()
    for (letter in filteredValue){
        when (letter) {
            ')' -> {
                if ( stack.empty() || stack.peek() != '(') {
                    return false
                }
                stack.pop()
            }
            else -> stack.add(letter)
        }
    }

    if (!stack.empty()){
        return false
    }

    return true
}

fun unknownVariables(expression: List<String>): Boolean {
    for (e in expression){
        if (e.matches(regexOnlyLetters) and !mapVar.containsKey(e)) {
            return true
        }
    }

    return false
}

fun processAssignment(input: String) {
    val cleanInput = input.replace(" ", "").split("=")

    if (!validateMembers(cleanInput)){
        return
    }

    val member1 = cleanInput[0]
    val member2 = cleanInput[1]

    if (member2.matches(regexOnlyDigits)){
        mapVar[member1] = member2
        //println("mapVar: $member1 = $member2")
    }
    else if (member2.matches(regexOnlyLetters)) {
        mapVar[member1] = mapVar[member2].toString()
        //println("mapVar: $member1 = ${mapVar[member2]}")
    }


}

fun validateMembers(cleanInput: List<String>): Boolean {

    if (cleanInput.size > 2){
        println("Invalid assignment")
        return false
    }

    val member1 = cleanInput[0]
    val member2 = cleanInput[1]

    if (!member1.matches(regexOnlyLetters)){
        println("Invalid identifier")
        return false
    }
    else if (!member2.matches(regexOnlyLetters) and !member2.matches(regexOnlyDigits)) {
        println("Invalid assignment")
        return false
    }
    else if (member2.matches(regexOnlyLetters) and !mapVar.containsKey(member2)){
        println("Unknown variable")
        return false
    }

    return true

}

fun processCommand(input: String) {

    when (input) {
        "/exit" -> { println("Bye!")
            exitProcess(0) }
        "/help" -> println("The program calculates the sum ans subtraction of numbers")
        else -> println("Unknown command")
    }
}

fun calculate(input: List<String>): BigInteger {

    //println("input list: $input")
    val postfix = infixToPostfix(input)
    val postfixWithoutVars = substituteVarsWithValues(postfix)

    return postfixToAnswer(postfixWithoutVars)

}

fun postfixToAnswer(postfixWithoutVars: MutableList<String>): BigInteger {

    val stack = Stack<String>()

    for (value in postfixWithoutVars) {
        if (value.matches(regexOnlyDigits)){
            stack.push(value)
        }
        else if (value.matches(Regex("(\\+|-|\\*|/)"))) {
            val number2 = stack.pop().toBigInteger()
            val number1 = stack.pop().toBigInteger()

            var calc = 0.toBigInteger()

            when (value) {
                "+" -> calc = number1 + number2
                "-" -> calc = number1 - number2
                "/" -> calc = number1 / number2
                "*" -> calc = number1 * number2
            }

            stack.push(calc.toString())
        }
    }

    return stack.pop().toBigInteger()
}

fun substituteVarsWithValues(postfix: MutableList<String>): MutableList<String> {
    val list = mutableListOf<String>()
    for (value in postfix){
        if (mapVar.containsKey(value)){
            list.add(mapVar[value].toString())
        }
        else{
            list.add(value)
        }
    }

    return list
}



fun infixToPostfix(infix: List<String>): MutableList<String> {
    val postfix = mutableListOf<String>()
    val stack = Stack<String>()

    for (value in infix){
        // When getting a operands
        if (value.matches(regexOnlyDigits) || value.matches(regexOnlyLetters)){
            postfix.add(value)
        }
        else {
            if (stack.isEmpty() || value == "("){
                // Base case When getting an open parenthesis Or stack is empty
                stack.push(value)
            }
            else if (value == ")") {
                // Need to remove stack element until the close bracket
                while (!stack.isEmpty() && stack.peek() != "(")
                {
                    // Get top element
                    postfix.add(stack.peek())
                    // Remove stack element
                    stack.pop()
                }
                if (stack.peek() == "(")
                {
                    // Remove stack element
                    stack.pop()
                }
            }
            else
            {
                // Remove stack element until precedence of top is greater than current infix operator
                while (!stack.isEmpty() && precedence(value) <= precedence(stack.peek()))
                {
                    // Get top element
                    postfix.add(stack.peek())
                    // Remove stack element
                    stack.pop()
                }
                // Add new operator
                stack.push(value)
            }
        }
    }

    // Add remaining elements
    while (!stack.isEmpty())
    {
        postfix.add(stack.peek())
        stack.pop()
    }

    return postfix
}

fun precedence(text: String): Int
{
    return when (text) {
        "+", "-" -> {
            1
        }
        "*", "/" -> {
            2
        }
        "^" -> {
            3
        }
        else -> -1
    }
}

fun cleanInput(list: List<String>): MutableList<String> {

    val newList: MutableList<String> = ArrayList()

    for (i in list.indices) {

        if (list[i].matches(Regex("-+"))){
            if (list[i].length % 2 == 0)
                newList.add("+")
            else
                newList.add("-")
        }

        else if (list[i].matches(Regex("\\++"))){
            newList.add("+")
        }

        else if (list[i].matches(Regex("(\\++)[0-9]+"))){
            newList.add(list[i].replace("+", ""))
        }

        else if (list[i].matches(Regex("(-+)[0-9]+"))){
            //Count how many '-' exist
            val lastMinusIndex = list[i].lastIndexOf("-")
            val countMinusSymbol = list[i].substring(0, lastMinusIndex+1).length

            //if number of '-' is even, replace with nothing
            if (countMinusSymbol % 2 == 0) {
                newList.add(list[i].replace(Regex("-+"), ""))
            }
            //if number of '-' is odd, replace with only one '-'
            else {
                newList.add(list[i].replace(Regex("-+"), "-"))
            }
        }

        else if (list[i] != "") {
            newList.add(list[i])
        }
    }

    //println("cleanInput: $newList")

    return newList
}
