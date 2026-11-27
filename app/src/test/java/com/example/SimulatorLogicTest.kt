package com.example

import com.example.data.repository.SimulatorLogic
import org.junit.Assert.assertEquals
import org.junit.Test

class SimulatorLogicTest {

    @Test
    fun testSeniorityBonusPercentage() {
        // Less than 3 years
        assertEquals(0, SimulatorLogic.calculateBonusPercentage(0))
        assertEquals(0, SimulatorLogic.calculateBonusPercentage(1))
        assertEquals(0, SimulatorLogic.calculateBonusPercentage(2))

        // 3 to 5 years: 3%
        assertEquals(3, SimulatorLogic.calculateBonusPercentage(3))
        assertEquals(3, SimulatorLogic.calculateBonusPercentage(4))
        assertEquals(3, SimulatorLogic.calculateBonusPercentage(5))

        // 6 to 8 years: 6%
        assertEquals(6, SimulatorLogic.calculateBonusPercentage(6))
        assertEquals(6, SimulatorLogic.calculateBonusPercentage(7))
        assertEquals(6, SimulatorLogic.calculateBonusPercentage(8))

        // 9 to 11 years: 9%
        assertEquals(9, SimulatorLogic.calculateBonusPercentage(9))
        assertEquals(9, SimulatorLogic.calculateBonusPercentage(10))
        assertEquals(9, SimulatorLogic.calculateBonusPercentage(11))

        // 12 to 14 years: 12%
        assertEquals(12, SimulatorLogic.calculateBonusPercentage(12))
        assertEquals(12, SimulatorLogic.calculateBonusPercentage(13))
        assertEquals(12, SimulatorLogic.calculateBonusPercentage(14))

        // 15 years and more: 15%
        assertEquals(15, SimulatorLogic.calculateBonusPercentage(15))
        assertEquals(15, SimulatorLogic.calculateBonusPercentage(20))
        assertEquals(15, SimulatorLogic.calculateBonusPercentage(25))
    }

    @Test
    fun testMonthlyBonusAmount() {
        // Base salary = 2000€, 3% bonus = 60€
        assertEquals(60.0, SimulatorLogic.calculateMonthlyBonus(2000.0, 3), 0.001)

        // Base salary = 2500€, 12% bonus = 300€
        assertEquals(300.0, SimulatorLogic.calculateMonthlyBonus(2500.0, 12), 0.001)

        // Base salary = 3200€, 0% bonus = 0€
        assertEquals(0.0, SimulatorLogic.calculateMonthlyBonus(3200.0, 0), 0.001)
    }

    @Test
    fun testExtraLeaveDays() {
        // Less than 5 years: 0 days
        assertEquals(0, SimulatorLogic.calculateExtraLeaveDays(0))
        assertEquals(0, SimulatorLogic.calculateExtraLeaveDays(4))

        // 5 to 9 years: +1 day
        assertEquals(1, SimulatorLogic.calculateExtraLeaveDays(5))
        assertEquals(1, SimulatorLogic.calculateExtraLeaveDays(9))

        // 10 to 14 years: +2 days
        assertEquals(2, SimulatorLogic.calculateExtraLeaveDays(10))
        assertEquals(2, SimulatorLogic.calculateExtraLeaveDays(14))

        // 15 years and more: +3 days
        assertEquals(3, SimulatorLogic.calculateExtraLeaveDays(15))
        assertEquals(3, SimulatorLogic.calculateExtraLeaveDays(25))
    }
}
