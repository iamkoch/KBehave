package io.github.iamkoch.kbehave.engine

import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.TestSource
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.ClassSource
import java.util.*

/**
 * TestDescriptor for a KBehave test class.
 *
 * Represents a class containing @Scenario methods.
 */
class KBehaveClassDescriptor(
    uniqueId: UniqueId,
    private val testClass: Class<*>
) : AbstractTestDescriptor(
    uniqueId,
    testClass.simpleName,
    ClassSource.from(testClass)
) {
    override fun getType(): TestDescriptor.Type = TestDescriptor.Type.CONTAINER

    override fun mayRegisterTests(): Boolean = true
}
