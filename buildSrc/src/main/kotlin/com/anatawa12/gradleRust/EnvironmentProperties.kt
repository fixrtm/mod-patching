package com.anatawa12.gradleRust

interface EnvironmentProperties {
    /**
     * merged environment variables. null for unset super properties
     */
    val allEnvironment: Map<String, Any>

    /**
     * environment variables. null for unset super properties
     */
    val environment: Map<String, Any?>

    /**
     * Adds some environment variables
     */
    fun environment(environmentVariables: Map<String, *>)

    /**
     * Adds an environment variable to the environment for this process.
     *
     * @param name The name of the variable.
     * @param value The value for the variable. null for unset super properties
     * @return this
     */
    fun environment(name: String, value: Any?)

    /**
     * extend props from parent.
     */
    fun extendsFrom(parent: EnvironmentProperties)
}

class EnvironmentPropertiesContainer() : EnvironmentProperties {
    private val parents = mutableListOf<EnvironmentProperties>()

    @Suppress("UNCHECKED_CAST")
    override val allEnvironment: Map<String, Any>
        get() {
            val result = hashMapOf<String, Any?>()
            for (parent in parents) result.putAll(parent.allEnvironment)
            result.putAll(environment)
            return result.filterValues { it != null } as Map<String, Any>
        }
    override val environment = mutableMapOf<String, Any?>()

    override fun environment(environmentVariables: Map<String, *>) {
        environment.putAll(environmentVariables)
    }

    override fun environment(name: String, value: Any?) {
        environment[name] = value
    }

    override fun extendsFrom(parent: EnvironmentProperties) {
        parents += parent
    }
}
