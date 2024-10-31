package com.github.enotvtapke

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.*

class EvalconstSubplugin : KotlinCompilerPluginSupportPlugin {
    override fun apply(target: Project) {
        target.extensions.create("evalconst", EvalconstExtension::class.java)
        super.apply(target)
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val extension = project.extensions.getByType(EvalconstExtension::class.java)
        return project.provider {
            listOf(
                SubpluginOption(key = "const-fun-prefix", value = extension.constFunctionPrefix),
                SubpluginOption(key = "step-Limit", value = extension.stepNumberLimit.toString()),
                SubpluginOption(key = "stack-Limit", value = extension.stackSizeLimit.toString()),
            )
        }
    }

    override fun getCompilerPluginId() = "com.github.enotvtapke.evalconst"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = "com.github.enotvtapke",
        artifactId = "evalconst",
    )
}
