package com.vikbytes

object TerminalColors {
    // Reset
    const val RESET = "\u001B[0m"

    // Regular Colors
    const val BLACK = "\u001B[30m"
    const val RED = "\u001B[31m"
    const val GREEN = "\u001B[32m"
    const val YELLOW = "\u001B[33m"
    const val BLUE = "\u001B[34m"
    const val PURPLE = "\u001B[35m"
    const val CYAN = "\u001B[36m"
    const val WHITE = "\u001B[37m"

    // Bold
    const val BOLD = "\u001B[1m"

    // Italic
    const val ITALIC = "\u001B[3m"

    // Underline
    const val UNDERLINE = "\u001B[4m"

    // Bold Colors
    const val BOLD_BLACK = "\u001B[1;30m"
    const val BOLD_RED = "\u001B[1;31m"
    const val BOLD_GREEN = "\u001B[1;32m"
    const val BOLD_YELLOW = "\u001B[1;33m"
    const val BOLD_BLUE = "\u001B[1;34m"
    const val BOLD_PURPLE = "\u001B[1;35m"
    const val BOLD_CYAN = "\u001B[1;36m"
    const val BOLD_WHITE = "\u001B[1;37m"

    // Background Colors
    const val BG_BLACK = "\u001B[40m"
    const val BG_RED = "\u001B[41m"
    const val BG_GREEN = "\u001B[42m"
    const val BG_YELLOW = "\u001B[43m"
    const val BG_BLUE = "\u001B[44m"
    const val BG_PURPLE = "\u001B[45m"
    const val BG_CYAN = "\u001B[46m"
    const val BG_WHITE = "\u001B[47m"
}

fun stripAnsiCodes(input: String): String {
    val ansiRegex = "\u001B\\[[;\\d]*[A-Za-z]".toRegex()
    return input.replace(ansiRegex, "")
}
fun String.bold() = "${TerminalColors.BOLD}$this${TerminalColors.RESET}"
fun String.italic() = "${TerminalColors.ITALIC}$this${TerminalColors.RESET}"
fun String.underline() = "${TerminalColors.UNDERLINE}$this${TerminalColors.RESET}"

fun String.black() = "${TerminalColors.BLACK}$this${TerminalColors.RESET}"
fun String.red() = "${TerminalColors.RED}$this${TerminalColors.RESET}"
fun String.green() = "${TerminalColors.GREEN}$this${TerminalColors.RESET}"
fun String.yellow() = "${TerminalColors.YELLOW}$this${TerminalColors.RESET}"
fun String.blue() = "${TerminalColors.BLUE}$this${TerminalColors.RESET}"
fun String.purple() = "${TerminalColors.PURPLE}$this${TerminalColors.RESET}"
fun String.cyan() = "${TerminalColors.CYAN}$this${TerminalColors.RESET}"
fun String.white() = "${TerminalColors.WHITE}$this${TerminalColors.RESET}"

fun String.boldBlack() = "${TerminalColors.BOLD_BLACK}$this${TerminalColors.RESET}"
fun String.boldRed() = "${TerminalColors.BOLD_RED}$this${TerminalColors.RESET}"
fun String.boldGreen() = "${TerminalColors.BOLD_GREEN}$this${TerminalColors.RESET}"
fun String.boldYellow() = "${TerminalColors.BOLD_YELLOW}$this${TerminalColors.RESET}"
fun String.boldBlue() = "${TerminalColors.BOLD_BLUE}$this${TerminalColors.RESET}"
fun String.boldPurple() = "${TerminalColors.BOLD_PURPLE}$this${TerminalColors.RESET}"
fun String.boldCyan() = "${TerminalColors.BOLD_CYAN}$this${TerminalColors.RESET}"
fun String.boldWhite() = "${TerminalColors.BOLD_WHITE}$this${TerminalColors.RESET}"

fun String.boldItalic() = "${TerminalColors.BOLD}${TerminalColors.ITALIC}$this${TerminalColors.RESET}"
