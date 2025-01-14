package com.vanniktech.maven.publish.sonatype

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.options.Option

internal abstract class CloseAndReleaseSonatypeRepositoryTask : DefaultTask() {

  @get:Internal
  abstract val buildService: Property<SonatypeRepositoryBuildService>

  @Option(option = "repository", description = "Specify which staging repository to close and release.")
  @Input
  @Optional
  var manualStagingRepositoryId: String? = null

  @TaskAction
  fun closeAndReleaseRepository() {
    val service = this.buildService.get()

    // if repository was already closed in this build this is a no-op
    if (service.repositoryClosed) {
      return
    }

    val manualStagingRepositoryId = this.manualStagingRepositoryId
    if (manualStagingRepositoryId != null) {
      service.nexus.closeStagingRepository(manualStagingRepositoryId)
      service.nexus.releaseStagingRepository(manualStagingRepositoryId)
    } else {
      val id = service.nexus.closeCurrentStagingRepository()
      service.nexus.releaseStagingRepository(id)
    }

    service.repositoryClosed = true
  }

  companion object {
    private const val NAME = "closeAndReleaseRepository"

    fun TaskContainer.registerCloseAndReleaseRepository(
      buildService: Provider<SonatypeRepositoryBuildService>,
    ): TaskProvider<CloseAndReleaseSonatypeRepositoryTask> {
      return register(NAME, CloseAndReleaseSonatypeRepositoryTask::class.java) {
        it.description = "Closes and releases a staging repository on Sonatype OSS"
        it.group = "release"
        it.buildService.set(buildService)
        it.usesService(buildService)
      }
    }
  }
}
