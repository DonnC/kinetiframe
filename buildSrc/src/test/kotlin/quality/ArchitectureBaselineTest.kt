package quality

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.Properties

class ArchitectureBaselineTest {
    @Test fun `old CRLF findings translate to fewer parameters without widening allowances`() {
        val properties = Properties().apply {
            setProperty("arity|app/File.kt|.legacy(,Int,,String,,Boolean,,Float,,Long)", "10")
        }
        val allowances = ArchitectureBaseline.allowances(properties)
        assertEquals(5, allowances["arity|app/File.kt|.legacy(Int,String,Boolean,Float,Long)"])
        assertNull(allowances["arity|designsystem/File.kt|.legacy(Int,String,Boolean,Float,Long)"])
        assertNull(allowances["arity|app/File.kt|.legacy(Int,String,Boolean,Float,Long,Double)"])
    }

    @Test fun `normal findings and inferred lambda parameter types retain their limits`() {
        val properties = Properties().apply {
            setProperty("lines|app/File.kt", "600")
            setProperty("arity|app/File.kt|.lambda(,,,,)", "5")
        }
        val allowances = ArchitectureBaseline.allowances(properties)
        assertEquals(600, allowances["lines|app/File.kt"])
        assertEquals(5, allowances["arity|app/File.kt|.lambda(,,,,)"])
        assertNull(allowances["arity|app/File.kt|.lambda(,,)"])
        assertNull(allowances["arity|app/File.kt|.lambda(,)"])
    }

    @Test fun `a carriage return between typed parameters does not require a leading phantom`() {
        val properties = Properties().apply {
            setProperty("arity|app/File.kt|.legacy(Int,String,,Boolean,Float,Long)", "6")
        }
        assertEquals(
            5,
            ArchitectureBaseline.allowances(properties)["arity|app/File.kt|.legacy(Int,String,Boolean,Float,Long)"],
        )
    }
}
