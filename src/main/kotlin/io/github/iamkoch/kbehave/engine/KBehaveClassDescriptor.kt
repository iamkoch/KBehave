package io.github.iamkoch.kbehave.engine

import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.ClassSource

/**
 * TestDescriptor for a KBehave test class.
 *
 * Represents a class containing @Scenario methods.
 */
internal class KBehaveClassDescriptor(
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
