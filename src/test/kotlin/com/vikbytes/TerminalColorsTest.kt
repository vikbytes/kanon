package com.vikbytes

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TerminalColorsTest {

    @Test
    fun `test stripAnsiCodes removes all color codes`() {
        val coloredText = "This is ${TerminalColors.RED}red${TerminalColors.RESET} and ${TerminalColors.BLUE}blue${TerminalColors.RESET} text"
        val plainText = stripAnsiCodes(coloredText)
        
        assertEquals("This is red and blue text", plainText)
        assertNotEquals(coloredText, plainText)
    }
    
    @Test
    fun `test stripAnsiCodes with no color codes`() {
        val plainText = "This is plain text with no color codes"
        val result = stripAnsiCodes(plainText)
        
        assertEquals(plainText, result)
    }
    
    @Test
    fun `test String extension functions add correct color codes`() {
        val text = "Test"
        
        // Test regular colors
        assertEquals("${TerminalColors.RED}Test${TerminalColors.RESET}", text.red())
        assertEquals("${TerminalColors.GREEN}Test${TerminalColors.RESET}", text.green())
        assertEquals("${TerminalColors.BLUE}Test${TerminalColors.RESET}", text.blue())
        assertEquals("${TerminalColors.YELLOW}Test${TerminalColors.RESET}", text.yellow())
        assertEquals("${TerminalColors.CYAN}Test${TerminalColors.RESET}", text.cyan())
        assertEquals("${TerminalColors.PURPLE}Test${TerminalColors.RESET}", text.purple())
        assertEquals("${TerminalColors.WHITE}Test${TerminalColors.RESET}", text.white())
        assertEquals("${TerminalColors.BLACK}Test${TerminalColors.RESET}", text.black())
        
        // Test bold colors
        assertEquals("${TerminalColors.BOLD_RED}Test${TerminalColors.RESET}", text.boldRed())
        assertEquals("${TerminalColors.BOLD_GREEN}Test${TerminalColors.RESET}", text.boldGreen())
        assertEquals("${TerminalColors.BOLD_BLUE}Test${TerminalColors.RESET}", text.boldBlue())
        assertEquals("${TerminalColors.BOLD_YELLOW}Test${TerminalColors.RESET}", text.boldYellow())
        assertEquals("${TerminalColors.BOLD_CYAN}Test${TerminalColors.RESET}", text.boldCyan())
        assertEquals("${TerminalColors.BOLD_PURPLE}Test${TerminalColors.RESET}", text.boldPurple())
        assertEquals("${TerminalColors.BOLD_WHITE}Test${TerminalColors.RESET}", text.boldWhite())
        assertEquals("${TerminalColors.BOLD_BLACK}Test${TerminalColors.RESET}", text.boldBlack())
        
        // Test formatting
        assertEquals("${TerminalColors.BOLD}Test${TerminalColors.RESET}", text.bold())
        assertEquals("${TerminalColors.ITALIC}Test${TerminalColors.RESET}", text.italic())
        assertEquals("${TerminalColors.UNDERLINE}Test${TerminalColors.RESET}", text.underline())
        assertEquals("${TerminalColors.BOLD}${TerminalColors.ITALIC}Test${TerminalColors.RESET}", text.boldItalic())
    }
    
    @Test
    fun `test stripAnsiCodes with multiple formatting`() {
        val complexText = "This is ${TerminalColors.BOLD}${TerminalColors.RED}bold red${TerminalColors.RESET} and " +
                "${TerminalColors.ITALIC}${TerminalColors.BLUE}italic blue${TerminalColors.RESET} text"
        val plainText = stripAnsiCodes(complexText)
        
        assertEquals("This is bold red and italic blue text", plainText)
    }
    
    @Test
    fun `test stripAnsiCodes with extension functions`() {
        val text = "This is ".bold() + "colored".red() + " text".green()
        val plainText = stripAnsiCodes(text)
        
        assertEquals("This is colored text", plainText)
    }
    
    @Test
    fun `test chained extension functions`() {
        val text = "Test"
        val boldRedText = text.bold().red()
        val redBoldText = text.red().bold()
        
        // Both should contain both BOLD and RED codes, but in different order
        assertTrue(boldRedText.contains(TerminalColors.BOLD))
        assertTrue(boldRedText.contains(TerminalColors.RED))
        assertTrue(redBoldText.contains(TerminalColors.BOLD))
        assertTrue(redBoldText.contains(TerminalColors.RED))
        
        // Both should strip to the same plain text
        assertEquals("Test", stripAnsiCodes(boldRedText))
        assertEquals("Test", stripAnsiCodes(redBoldText))
    }
}