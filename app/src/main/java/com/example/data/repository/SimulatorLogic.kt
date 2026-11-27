package com.example.data.repository

object SimulatorLogic {
    
    /**
     * Calculates the seniority bonus percentage under the CCNT Telecommunications.
     * - < 3 years: 0%
     * - 3-5 years: 3%
     * - 6-8 years: 6%
     * - 9-11 years: 9%
     * - 12-14 years: 12%
     * - >= 15 years: 15%
     */
    fun calculateBonusPercentage(seniorityYears: Int): Int {
        return when {
            seniorityYears < 3 -> 0
            seniorityYears in 3..5 -> 3
            seniorityYears in 6..8 -> 6
            seniorityYears in 9..11 -> 9
            seniorityYears in 12..14 -> 12
            else -> 15
        }
    }

    /**
     * Calculates the monthly seniority bonus in Euros.
     */
    fun calculateMonthlyBonus(baseSalary: Double, bonusPercentage: Int): Double {
        return (baseSalary * bonusPercentage) / 100.0
    }

    /**
     * Calculates the additional paid leave days earned by seniority.
     * - < 5 years: 0 days
     * - 5-9 years: 1 day
     * - 10-14 years: 2 days
     * - >= 15 years: 3 days
     */
    fun calculateExtraLeaveDays(seniorityYears: Int): Int {
        return when {
            seniorityYears < 5 -> 0
            seniorityYears in 5..9 -> 1
            seniorityYears in 10..14 -> 2
            else -> 3
        }
    }
}
