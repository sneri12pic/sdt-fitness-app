package com.stepandemianenko.sdtfitness.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedExerciseCatalogTest {

    @Test
    fun exerciseCatalog_hasExpectedMvpSizeAndUniqueIds() {
        val exercises = SeedExerciseCatalog.exercises

        assertEquals(120, exercises.size)
        assertEquals(120, exercises.map { it.id }.toSet().size)
        assertTrue(exercises.all { it.title.isNotBlank() })
        assertTrue(exercises.all { it.muscleGroup.isNotBlank() })
    }
}
