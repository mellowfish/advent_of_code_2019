fun allPossiblePasswords(from: Int, to: Int): List<Int> {
    if (to <= from) { throw IllegalArgumentException("`from` must be less than `to`") }

    var possiblePasswords = mutableListOf<Int>()
    var password = from
    while (password <= to) {
        if (validPassword(password)) {
            possiblePasswords.add(password)
        }
        password += 1
    }
    return possiblePasswords
}

fun validPassword(password: Int): Boolean {
    val digits = digits(password)
    return nondecreasing(digits) && containsNeighboringDuplicate(digits)
}

fun digits(number: Int): List<Int> {
    var remainder = number
    var list = mutableListOf<Int>()
    var digit: Int
    while (remainder > 0) {
        digit = remainder % 10
        remainder = (remainder - digit) / 10
        list.add(digit)
    }
    return list.reversed()
}

fun nondecreasing(digits: List<Int>): Boolean {
    return digits.sorted().equals(digits)
}

fun containsNeighboringDuplicate(digits: List<Int>): Boolean {
    var lastLastDigit = -1
    var lastDigit = -1
    var duplicate = -1
    digits.forEach {
        if (it.equals(lastDigit)) {
            if (lastDigit.equals(lastLastDigit)) {
                duplicate = -1
            }
            else {
                duplicate = it
            }
        }
        else if (duplicate > -1) {
            return true
        }
        lastLastDigit = lastDigit
        lastDigit = it
    }
    return duplicate > -1
}

println(allPossiblePasswords(137683, 596253).size)