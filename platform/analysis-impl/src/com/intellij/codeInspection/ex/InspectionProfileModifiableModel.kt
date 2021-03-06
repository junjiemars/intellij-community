/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.codeInspection.ex

import com.intellij.codeInspection.ModifiableModel
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.WriteExternalException
import com.intellij.profile.codeInspection.ProjectInspectionProfileManager
import com.intellij.util.Consumer

open class InspectionProfileModifiableModel(val source: InspectionProfileImpl) : InspectionProfileImpl(source.name, source.myToolSupplier, source.profileManager, source.myBaseProfile, null), ModifiableModel {
  private var modified = false

  init {
    myUninitializedSettings.putAll(source.myUninitializedSettings)
    isProjectLevel = source.isProjectLevel
    myLockedProfile = source.myLockedProfile
    copyFrom(source)
  }

  fun isChanged() = modified || source.myLockedProfile != myLockedProfile

  fun setModified(value: Boolean) {
    modified = value
  }

  override fun copyToolsConfigurations(project: Project?) {
    copyToolsConfigurations(source, project)
  }

  override fun createTools(project: Project?) = source.getDefaultStates(project).map { it.tool }

  private fun copyToolsConfigurations(profile: InspectionProfileImpl, project: Project?) {
    try {
      for (toolList in profile.myTools.values) {
        val tools = myTools[toolList.shortName]!!
        val defaultState = toolList.defaultState
        tools.setDefaultState(copyToolSettings(defaultState.tool), defaultState.isEnabled, defaultState.level)
        tools.removeAllScopes()
        val nonDefaultToolStates = toolList.nonDefaultTools
        if (nonDefaultToolStates != null) {
          for (state in nonDefaultToolStates) {
            val toolWrapper = copyToolSettings(state.tool)
            val scope = state.getScope(project)
            if (scope == null) {
              tools.addTool(state.scopeName, toolWrapper, state.isEnabled, state.level)
            }
            else {
              tools.addTool(scope, toolWrapper, state.isEnabled, state.level)
            }
          }
        }
        tools.isEnabled = toolList.isEnabled
      }
    }
    catch (e: WriteExternalException) {
      LOG.error(e)
    }
    catch (e: InvalidDataException) {
      LOG.error(e)
    }
  }

  fun isProperSetting(toolId: String): Boolean {
    if (myBaseProfile != null) {
      val tools = myBaseProfile.getToolsOrNull(toolId, null)
      val currentTools = myTools.get(toolId)
      return tools != currentTools
    }
    return false
  }

  fun resetToBase(project: Project?) {
    initInspectionTools(project)

    copyToolsConfigurations(myBaseProfile, project)
    myChangedToolNames = null
  }

  //invoke when isChanged() == true
  fun commit() {
    source.commit(this)
    modified = false
  }

  fun resetToEmpty(project: Project) {
    initInspectionTools(project)
    for (toolWrapper in getInspectionTools(null)) {
      setToolEnabled(toolWrapper.shortName, false, project, fireEvents = false)
    }
  }

  private fun InspectionProfileImpl.commit(model: InspectionProfileImpl) {
    name = model.name
    description = model.description
    isProjectLevel = model.isProjectLevel
    myLockedProfile = model.myLockedProfile
    myChangedToolNames = model.myChangedToolNames
    myTools = model.myTools
    profileManager = model.profileManager
  }

  override fun toString() = "$name (copy)"
}

fun modifyAndCommitProjectProfile(project: Project, action: Consumer<ModifiableModel>) {
  ProjectInspectionProfileManager.getInstance(project).currentProfile.edit { action.consume(this) }
}

inline fun InspectionProfileImpl.edit(task: InspectionProfileModifiableModel.() -> Unit) {
  val model = InspectionProfileModifiableModel(this)
  model.task()
  model.commit()
  profileManager.fireProfileChanged(this)
}
